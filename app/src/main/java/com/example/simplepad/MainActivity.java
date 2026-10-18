package com.example.simplepad;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.WindowManager;

import java.io.File;

public class MainActivity extends Activity {

  private static final String[] GRID_NAMES = { "3×3", "4×4", "4×5", "5×5" };
  private static final int[][] GRID_SIZES = { { 3, 3 }, { 4, 4 }, { 4, 5 }, { 5, 5 } };
  private static final int MAX_PADS = 25;
  private static final int REQ_AUDIO = 100;
  private static final int REQ_PICK_AUDIO = 101;

  private Pad[] pads = new Pad[MAX_PADS];
  private PadGridManager.PadView[] padViews = new PadGridManager.PadView[MAX_PADS];

  private int gridIndex = 1;
  private GridLayout grid;

  private AudioPlayer audioPlayer;
  private AudioRecorder audioRecorder;
  private PadStorage storage;
  private TopBarView topBar;

  private boolean isRecordingMode = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    audioPlayer = new AudioPlayer();
    audioRecorder = new AudioRecorder();
    storage = new PadStorage(this);

    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(Color.parseColor("#1E1E1E"));

    topBar = new TopBarView(this);

    grid = new GridLayout(this);
    LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
    grid.setLayoutParams(gridParams);
    grid.setPadding(8, 8, 8, 8);

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

    LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0,
        LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
    bp.setMargins(8, 0, 8, 0);
    btnPause.setLayoutParams(bp);
    btnStop.setLayoutParams(bp);

    LinearLayout.LayoutParams bpSmall = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
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
    btnSettings.setOnClickListener(v -> showSettingsMenu());

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
    setupRecorderButtons();
    updateKeepScreenFlag();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    audioPlayer.stopAll();
    audioRecorder.stop();
  }

  private void updateKeepScreenFlag() {
    int mode = storage.getKeepScreenMode(); // 0=системно, 1=при записи, 2=всегда
    boolean keepOn = false;

    if (mode == 2) {
      keepOn = true; // Всегда
    } else if (mode == 1 && isRecordingMode) {
      keepOn = true; // Только при записи
    }

    if (keepOn) {
      getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    } else {
      getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
  }

  private void applyGrid() {
    int cols = GRID_SIZES[gridIndex][0];
    int rows = GRID_SIZES[gridIndex][1];
    int visible = cols * rows;

    if (audioPlayer.getCurrentPadIndex() >= visible)
      audioPlayer.stopAll();

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

    // Если автозапись включена
    if (storage.isAutoRecordEnabled()) {
      // Если пэд ПУСТОЙ — ничего не делаем
      if (!hasSound) {
        return;
      }

      // Если пэд ЗАПОЛНЕН:
      // 1. Если запись еще не идет — запускаем её
      if (!isRecordingMode) {
        startAutoRecording();
      }

      // 2. Воспроизводим звук (или ставим на паузу, если это тот же пэд)
      if (index == audioPlayer.getCurrentPadIndex()) {
        audioPlayer.togglePause();
      } else {
        audioPlayer.playNew(this, index, pads[index].filePath, padViews[index].progress,
            this::refreshPadColors);
      }
      refreshPadColors();
      return;
    }

    // --- Логика для обычного режима (автозапись выключена) ---
    if (!hasSound) {
      showPadMenu(index);
      return;
    }

    if (index == audioPlayer.getCurrentPadIndex()) {
      audioPlayer.togglePause();
    } else {
      audioPlayer.playNew(this, index, pads[index].filePath, padViews[index].progress,
          this::refreshPadColors);
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
      if (pv == null)
        continue;

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
    if (padViews[index] == null)
      return;

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

  // ================= ЛОГИКА ДИКТОФОНА =================

  private void setupRecorderButtons() {
    topBar.btnRecStart.setOnClickListener(v -> {
      if (isRecordingMode)
        return;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(
          Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
        requestAudioPermission();
        return;
      }
      String path = MediaFileManager.createRecordFilePath();
      if (path == null)
        return;

      if (audioRecorder.start(MainActivity.this, path, topBar.tvRecordTimer)) {
        isRecordingMode = true;
        updateKeepScreenFlag();
        topBar.tvRecordIndicator.setVisibility(TextView.VISIBLE);
        topBar.btnRecStart.setEnabled(false);
        topBar.btnRecPause.setEnabled(true);
        topBar.btnRecStop.setEnabled(true);
        topBar.btnRecPause.setText("⏸");
      }
    });

    topBar.btnRecPause.setOnClickListener(v -> {
      if (!isRecordingMode)
        return;
      if (audioRecorder.isPaused()) {
        audioRecorder.resume();
        topBar.btnRecPause.setText("⏸");
        topBar.tvRecordIndicator.setVisibility(TextView.VISIBLE);
      } else {
        audioRecorder.pause();
        topBar.btnRecPause.setText("▶️");
        topBar.tvRecordIndicator.setVisibility(TextView.INVISIBLE);
      }
    });

    topBar.btnRecStop.setOnClickListener(v -> {
      if (!isRecordingMode)
        return;
      audioRecorder.stop();
      isRecordingMode = false;
      updateKeepScreenFlag();
      topBar.tvRecordIndicator.setVisibility(TextView.GONE);
      topBar.btnRecStart.setEnabled(true);
      topBar.btnRecPause.setEnabled(false);
      topBar.btnRecStop.setEnabled(false);
      Toast.makeText(this, "Запись сохранена в Music/SimplePad", Toast.LENGTH_SHORT).show();
    });
  }

  private void startAutoRecording() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(
        Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
      requestAudioPermission();
      return;
    }
    String path = MediaFileManager.createRecordFilePath();
    if (path == null)
      return;

    if (audioRecorder.start(MainActivity.this, path, topBar.tvRecordTimer)) {
      isRecordingMode = true;
      updateKeepScreenFlag();
      topBar.tvRecordIndicator.setVisibility(TextView.VISIBLE);
      topBar.btnRecStart.setEnabled(false);
      topBar.btnRecPause.setEnabled(true);
      topBar.btnRecStop.setEnabled(true);
      topBar.btnRecPause.setText("⏸");
    }
  }

  // ================= ДИАЛОГИ И ИМПОРТ =================

  private void showPadMenu(final int index) {
    final String[] items = { "✏️ Переименовать", "🎤 Записать сэмпл", "📂 Импорт аудио",
        "🗑 Удалить сэмпл", "❌ Отмена" };
    new AlertDialog.Builder(this).setTitle("Пэд: " + pads[index].label)
        .setItems(items, (dialog, which) -> {
          switch (which) {
            case 0:
              renamePad(index);
              break;
            case 1:
              recordSample(index);
              break;
            case 2:
              importAudio(index);
              break;
            case 3:
              deleteSample(index);
              break;
          }
        }).show();
  }

  private void importAudio(int padIndex) {
    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
    intent.setType("audio/*");
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    try {
      startActivityForResult(Intent.createChooser(intent, "Выберите аудиофайл"),
          REQ_PICK_AUDIO + padIndex);
    } catch (android.content.ActivityNotFoundException ex) {
      Toast.makeText(this, "Установите файловый менеджер", Toast.LENGTH_SHORT).show();
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode >= REQ_PICK_AUDIO && requestCode <= REQ_PICK_AUDIO + MAX_PADS
        && resultCode == RESULT_OK) {
      int padIndex = requestCode - REQ_PICK_AUDIO;
      if (data != null && data.getData() != null) {
        String destPath = MediaFileManager.copyAudioToInternalStorage(this, data.getData(),
            padIndex);
        if (destPath != null) {
          pads[padIndex].filePath = destPath;
          storage.savePads(pads);
          updateDurationText(padIndex);
          Toast.makeText(this, "Файл импортирован на пэд", Toast.LENGTH_SHORT).show();
        } else {
          Toast.makeText(this, "Ошибка импорта файла", Toast.LENGTH_SHORT).show();
        }
      }
    }
  }

  private void renamePad(final int index) {
    EditText input = new EditText(this);
    input.setText(pads[index].label);
    input.setSelection(input.getText().length());
    new AlertDialog.Builder(this).setTitle("Новое имя пэда").setView(input)
        .setPositiveButton("OK", (d, w) -> {
          String name = input.getText().toString().trim();
          if (!name.isEmpty()) {
            pads[index].label = name;
            if (padViews[index] != null)
              padViews[index].label.setText(name);
            storage.savePads(pads);
          }
        }).setNegativeButton("Отмена", null).show();
  }

  private void deleteSample(int index) {
    if (pads[index].filePath != null) {
      pads[index].filePath = null;
      storage.savePads(pads);
      if (audioPlayer.getCurrentPadIndex() == index)
        audioPlayer.stopAll();
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
        .setTitle("Запись сэмпла на пэд " + pads[index].label).setView(layout)
        .setNegativeButton("Закрыть", null).create();

    final String path = new File(getFilesDir(), "sample_" + index + ".m4a").getAbsolutePath();

    btnRec.setOnClickListener(v -> {
      if (!audioRecorder.isRecording()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(
            Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
          requestAudioPermission();
          return;
        }
        if (audioRecorder.start(this, path, tvTime))
          btnRec.setText("⏹ Стоп");
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

  // ================= НАСТРОЙКИ И РАЗРЕШЕНИЯ =================

  private void showSettingsMenu() {
    String[] screenModes = { "Системно", "При записи", "Всегда" };
    int currentMode = storage.getKeepScreenMode();

    final String[] items = { "Размер сетки: " + GRID_NAMES[gridIndex],
        "Автозапись при тапе: " + (storage.isAutoRecordEnabled() ? "ВКЛ" : "ВЫКЛ"),
        "Не гасить экран: " + screenModes[currentMode] };

    new AlertDialog.Builder(this).setTitle("Настройки").setItems(items, (dialog, which) -> {
      if (which == 0) {
        showGridSizeDialog();
      } else if (which == 1) {
        boolean newState = !storage.isAutoRecordEnabled();
        storage.setAutoRecordEnabled(newState);
        Toast.makeText(this, "Автозапись: " + (newState ? "ВКЛ" : "ВЫКЛ"), Toast.LENGTH_SHORT)
            .show();
      } else if (which == 2) {
        showKeepScreenDialog();
      }
    }).show();
  }

  private void showKeepScreenDialog() {
    final String[] modes = { "Системно (по умолчанию)", "Только при записи",
        "Всегда, пока открыто" };
    new AlertDialog.Builder(this).setTitle("Не гасить экран")
        .setSingleChoiceItems(modes, storage.getKeepScreenMode(), (dialog, which) -> {
          storage.setKeepScreenMode(which);
          updateKeepScreenFlag(); // Применяем сразу
          dialog.dismiss();
        }).setNegativeButton("Отмена", null).show();
  }

  private void showGridSizeDialog() {
    new AlertDialog.Builder(this).setTitle("Размер сетки")
        .setSingleChoiceItems(GRID_NAMES, gridIndex, (dialog, which) -> {
          if (which != gridIndex) {
            gridIndex = which;
            applyGrid();
            Toast.makeText(this, "Сетка: " + GRID_NAMES[which], Toast.LENGTH_SHORT).show();
          }
          dialog.dismiss();
        }).setNegativeButton("Отмена", null).show();
  }

  private void requestAudioPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      if (checkSelfPermission(
          Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
        if (!shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
          showPermissionSettingsDialog();
        } else {
          requestPermissions(new String[] { Manifest.permission.RECORD_AUDIO }, REQ_AUDIO);
        }
      }
    }
  }

  private void showPermissionSettingsDialog() {
    new AlertDialog.Builder(this).setTitle("Нужно разрешение")
        .setMessage("Для записи нужен доступ к микрофону. Пожалуйста, разрешите его в настройках.")
        .setPositiveButton("Настройки", (dialog, which) -> {
          Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
          intent.setData(Uri.parse("package:" + getPackageName()));
          startActivity(intent);
        }).setNegativeButton("Отмена", null).show();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions,
      int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == REQ_AUDIO) {
      if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        Toast.makeText(this, "Микрофон разрешён", Toast.LENGTH_SHORT).show();
      } else {
        Toast.makeText(this, "Без разрешения запись не работает", Toast.LENGTH_LONG).show();
      }
    }
  }
}