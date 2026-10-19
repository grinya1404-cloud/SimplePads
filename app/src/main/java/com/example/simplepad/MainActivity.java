package com.example.simplepad;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends Activity {

    private static final String[] GRID_NAMES = {"3×3", "4×4", "4×5", "5×5"};
    private static final int[][] GRID_SIZES = {{3, 3}, {4, 4}, {4, 5}, {5, 5}};
    private static final int MAX_PADS = 25;
    private static final int REQ_PICK_AUDIO = 101;

    private Pad[] pads = new Pad[MAX_PADS];
    private PadGridManager.PadView[] padViews = new PadGridManager.PadView[MAX_PADS];

    private int gridIndex = 1;
    private GridLayout grid;

    private AudioPlayer audioPlayer;
    private AudioRecorder audioRecorder;
    private PadStorage storage;
    private TopBarView topBar;
    private RecordManager recordManager;
    private SettingsManager settingsManager;
    private VoxManager voxManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        audioPlayer = new AudioPlayer();
        audioRecorder = new AudioRecorder();
        storage = new PadStorage(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#1E1E1E"));

        // Панель диктофона
        topBar = new TopBarView(this);
        
        // Сетка пэдов
        grid = new GridLayout(this);
        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        grid.setLayoutParams(gridParams);
        grid.setPadding(8, 8, 8, 8);

        // Нижняя панель
        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setGravity(Gravity.CENTER);
        bottomBar.setPadding(8, 8, 8, 16);

        Button btnPause = new Button(this);
        btnPause.setText("⏸  Pause");
        Button btnStop = new Button(this);
        btnStop.setText("⏹  Stop");
        Button btnSettings = new Button(this);
        btnSettings.setText("⚙");

        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        bp.setMargins(8, 0, 8, 0);
        btnPause.setLayoutParams(bp);
        btnStop.setLayoutParams(bp);

        LinearLayout.LayoutParams bpSmall = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        bpSmall.setMargins(8, 0, 8, 0);
        btnSettings.setLayoutParams(bpSmall);

        btnPause.setOnClickListener(v -> {
            audioPlayer.togglePause();
            refreshPadColors();
        });
        btnStop.setOnClickListener(v -> {
            audioPlayer.stopAll();
            refreshPadColors();
        });

        // Настройки
        settingsManager = new SettingsManager(
            this, storage,
            newIndex -> {
                gridIndex = newIndex;
                applyGrid();
            },
            () -> updateVoxState() // Слушатель изменений
        );
        
        btnSettings.setOnClickListener(v -> {
            settingsManager.showMainMenu(GRID_NAMES[gridIndex], storage.isAutoRecordEnabled());
        });

        bottomBar.addView(btnPause);
        bottomBar.addView(btnStop);
        bottomBar.addView(btnSettings);

        root.addView(topBar);
        root.addView(grid);
        root.addView(bottomBar);
        setContentView(root);

        pads = storage.loadPads();
        gridIndex = storage.getGridIndex();
        applyGrid();

        // ================= ИНИЦИАЛИЗАЦИЯ VOX (ДО RecordManager!) =================
        voxManager = new VoxManager(audioRecorder, storage, new VoxManager.VoxListener() {
            @Override
            public void onVoxLevel(int level) {
                if (topBar.voxLevelBar.getVisibility() == View.VISIBLE) {
                    topBar.voxLevelBar.setProgress(level);
                }
            }

            @Override
            public void onVoxStart() {
                String path = MediaFileManager.createRecordFilePath();
                if (path != null) {
                    audioRecorder.start(MainActivity.this, path, topBar.tvRecordTimer);
                    updateButtonsState(true);
                }
            }

            @Override
            public void onVoxStop() {
                audioRecorder.stop();
                updateButtonsState(false);
                
                if (storage.isVoxEnabled()) {
                    voxManager.restart();
                }
            }
        });

        // ================= ИНИЦИАЛИЗАЦИЯ RECORD MANAGER =================
        recordManager = new RecordManager(this, audioRecorder, topBar, 
            isRecording -> {
                updateKeepScreenFlag();
                updateButtonsState(isRecording);
                
                if (voxManager != null) {
                    if (isRecording) {
                        voxManager.forceStart(); // Сообщаем Vox, что мы начали писать вручную
                    } else if (storage.isVoxEnabled()) {
                        voxManager.restart();    // Перезапускаем Vox после остановки
                    }
                }
            });

        updateVoxState(); 
        updateKeepScreenFlag();
        
        // Принудительно синхронизируем кнопки с реальным состоянием
        boolean isRec = recordManager.isRecordingMode() || (voxManager != null && voxManager.isRecordingActive());
        updateButtonsState(isRec);
    }

    private void updateVoxState() {
        boolean isVoxEnabled = storage.isVoxEnabled();
        if (isVoxEnabled) {
            topBar.voxLevelBar.setVisibility(View.VISIBLE);
            voxManager.start();
        } else {
            topBar.voxLevelBar.setVisibility(View.GONE);
            voxManager.stop();
        }
        // Синхронизируем кнопки с текущим состоянием
        boolean isRec = recordManager.isRecordingMode() || (voxManager != null && voxManager.isRecordingActive());
        updateButtonsState(isRec);
    }

    private void updateButtonsState(boolean isRecording) {
        topBar.btnRecStart.setEnabled(!isRecording);
        topBar.btnRecPause.setEnabled(isRecording);
        topBar.btnRecStop.setEnabled(isRecording);
        topBar.tvRecordIndicator.setVisibility(isRecording ? View.VISIBLE : View.GONE);
        if (!isRecording) {
            topBar.btnRecPause.setText("⏸");
            topBar.tvRecordTimer.setText("00:00");
        }
    }

    private void updateKeepScreenFlag() {
        int mode = storage.getKeepScreenMode();
        boolean keepOn = mode == 2 || (mode == 1 && recordManager != null && recordManager.isRecordingMode());
        if (keepOn) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        audioPlayer.stopAll();
        if (voxManager != null) voxManager.stop();
        if (audioRecorder != null) audioRecorder.stop();
    }

    // ================= СЕТКА =================

    private void applyGrid() {
        int cols = GRID_SIZES[gridIndex][0];
        int rows = GRID_SIZES[gridIndex][1];
        int visible = cols * rows;

        if (audioPlayer.getCurrentPadIndex() >= visible) audioPlayer.stopAll();

        grid.removeAllViews();
        grid.setColumnCount(cols);
        grid.setRowCount(rows);

        for (int i = 0; i < visible; i++) {
            final int index = i;
            PadGridManager.PadView pv = PadGridManager.createPadView(this, grid, i, cols, pads[i].label);
            pv.container.setOnClickListener(v -> onPadTapped(index));
            pv.container.setOnLongClickListener(v -> {
                showPadMenu(index);
                return true;
            });
            padViews[i] = pv;
            updateDurationText(i);
        }

        storage.saveGridIndex(gridIndex);
        refreshPadColors();
    }

    private void onPadTapped(int index) {
        boolean hasSound = pads[index].filePath != null;

        if (storage.isAutoRecordEnabled()) {
            if (!hasSound) return;
            if (!recordManager.isRecordingMode()) {
                recordManager.startAutoRecording();
            }
            if (index == audioPlayer.getCurrentPadIndex()) {
                audioPlayer.togglePause();
            } else {
                audioPlayer.playNew(this, index, pads[index].filePath, padViews[index].progress, this::refreshPadColors);
            }
            refreshPadColors();
            return;
        }

        if (!hasSound) {
            showPadMenu(index);
            return;
        }

        if (index == audioPlayer.getCurrentPadIndex()) {
            audioPlayer.togglePause();
        } else {
            audioPlayer.playNew(this, index, pads[index].filePath, padViews[index].progress, this::refreshPadColors);
        }
        refreshPadColors();
    }

    private void refreshPadColors() {
        int cols = GRID_SIZES[gridIndex][0];
        int rows = GRID_SIZES[gridIndex][1];
        int visible = cols * rows;
        int currentPad = audioPlayer.getCurrentPadIndex();
        boolean isPaused = audioPlayer.isPaused();

        for (int i = 0; i < visible; i++) {
            PadGridManager.PadView pv = padViews[i];
            if (pv == null) continue;

            int bg = PadGridManager.COLOR_IDLE;
            if (i == currentPad) {
                bg = isPaused ? PadGridManager.COLOR_PAUSED : PadGridManager.COLOR_ACTIVE;
            }
            pv.container.setBackgroundColor(bg);

            if (i != currentPad) {
                pv.progress.setProgress(0);
            }
        }
    }

    private void updateDurationText(int index) {
        if (padViews[index] == null) return;
        
        if (pads[index].filePath == null) {
            padViews[index].durationText.setText("");
            return;
        }

        int durationMs = MediaFileManager.getAudioDuration(pads[index].filePath);
        if (durationMs > 0) {
            padViews[index].durationText.setText(MediaFileManager.formatDuration(durationMs));
        } else {
            padViews[index].durationText.setText("");
        }
    }

    // ================= МЕНЮ ПЭДА И ИМПОРТ =================

    private void showPadMenu(final int index) {
        final String[] items = {"✏️ Переименовать", "🎤 Записать сэмпл", "📂 Импорт аудио", "🗑 Удалить сэмпл", "❌ Отмена"};
        new AlertDialog.Builder(this)
                .setTitle("Пэд: " + pads[index].label)
                .setItems(items, (dialog, which) -> {
                    switch (which) {
                        case 0: renamePad(index); break;
                        case 1: recordSample(index); break;
                        case 2: importAudio(index); break;
                        case 3: deleteSample(index); break;
                    }
                })
                .show();
    }

    private void importAudio(int padIndex) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "Выберите аудиофайл"), REQ_PICK_AUDIO + padIndex);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Установите файловый менеджер", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode >= REQ_PICK_AUDIO && requestCode <= REQ_PICK_AUDIO + MAX_PADS && resultCode == RESULT_OK) {
            int padIndex = requestCode - REQ_PICK_AUDIO;
            if (data != null && data.getData() != null) {
                String destPath = MediaFileManager.copyAudioToInternalStorage(this, data.getData(), padIndex);
                if (destPath != null) {
                    pads[padIndex].filePath = destPath;
                    storage.savePads(pads);
                    updateDurationText(padIndex);
                    Toast.makeText(this, "Файл импортирован", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Ошибка импорта", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void renamePad(final int index) {
        EditText input = new EditText(this);
        input.setText(pads[index].label);
        input.setSelection(input.getText().length());
        new AlertDialog.Builder(this)
                .setTitle("Новое имя пэда")
                .setView(input)
                .setPositiveButton("OK", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        pads[index].label = name;
                        if (padViews[index] != null) padViews[index].label.setText(name);
                        storage.savePads(pads);
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void deleteSample(int index) {
        if (pads[index].filePath != null) {
            pads[index].filePath = null;
            storage.savePads(pads);
            if (audioPlayer.getCurrentPadIndex() == index) audioPlayer.stopAll();
            updateDurationText(index);
            refreshPadColors();
            Toast.makeText(this, "Сэмпл удалён", Toast.LENGTH_SHORT).show();
        }
    }

    private void recordSample(final int index) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);

        final TextView tvTime = new TextView(this);
        tvTime.setText("00:00");
        tvTime.setTextSize(36);
        tvTime.setGravity(Gravity.CENTER);

        final Button btnRec = new Button(this);
        btnRec.setText("🎤 Записать");
        layout.addView(tvTime);
        layout.addView(btnRec);

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Запись сэмпла на пэд " + pads[index].label)
                .setView(layout)
                .setNegativeButton("Закрыть", null)
                .create();

        final String path = new File(getFilesDir(), "sample_" + index + ".m4a").getAbsolutePath();

        btnRec.setOnClickListener(v -> {
            if (!audioRecorder.isRecording()) {
                if (audioRecorder.start(this, path, tvTime)) btnRec.setText("⏹ Стоп");
            } else {
                audioRecorder.stop();
                btnRec.setText("🎤 Записать");
                tvTime.setText("Готово ✓");
                pads[index].filePath = path;
                storage.savePads(pads);
                updateDurationText(index);
                Toast.makeText(this, "Сэмпл сохранён", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();
    }
}