package com.grinya.simplepad;

// a7
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.io.File;
import android.media.AudioManager;
import android.database.ContentObserver;
import android.os.Handler;

public class MainActivity extends Activity {

  private static final String[] GRID_NAMES = { "3×3", "4×4", "4×5", "5×5" };
  private static final int[][] GRID_SIZES = { { 3, 3 }, { 4, 4 }, { 4, 5 }, { 5, 5 } };
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

  private Handler voxHandler = new Handler(Looper.getMainLooper());
  private Runnable voxRunnable;
  private long voxSilenceStart = 0;
  private boolean voxIsRecording = false;

  // Счётчик подряд идущих измерений выше порога для подтверждения начала сигнала
  private int voxTriggerFrames = 0;

  private HeadsetMonitor headsetMonitor;
  private PowerManager.WakeLock wakeLock;
  private int voxWakeFrames = 0;

  private AudioManager audioManager;
  private ContentObserver volumeObserver;
  private Handler volumeHandler = new Handler(Looper.getMainLooper());

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    audioPlayer = new AudioPlayer();
    audioRecorder = new AudioRecorder(this);
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

    LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0,
        LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
    bp.setMargins(8, 0, 8, 0);
    btnPause.setLayoutParams(bp);
    btnStop.setLayoutParams(bp);

    LinearLayout.LayoutParams bpSmall = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    bpSmall.setMargins(8, 0, 8, 0);
    btnSettings.setLayoutParams(bpSmall);

    btnPause.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        audioPlayer.togglePause();
        refreshPadColors();
      }
    });
    btnStop.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        audioPlayer.stopAll();
        refreshPadColors();
      }
    });

    // Настройки
    settingsManager = new SettingsManager(this, storage, new SettingsManager.GridLayoutUpdater() {
      @Override
      public void updateGridSize(int newGridIndex) {
        gridIndex = newGridIndex;
        applyGrid();
      }
    }, new SettingsManager.SettingsChangeListener() {
      @Override
      public void onSettingsChanged() {
        updateKeepScreenFlag();
        startVoxIndicatorIfEnabled();
        updateVoxLabels();
      }
    });
    btnSettings.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        settingsManager.showMainMenu(GRID_NAMES[gridIndex], storage.isAutoRecordEnabled());
      }
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

    // Менеджер записи
    recordManager = new RecordManager(this, audioRecorder, topBar,
        new RecordManager.OnRecordingStateChanged() {
          @Override
          public void onStateChanged(boolean isRecording) {
            updateKeepScreenFlag();
            startVoxIndicatorIfEnabled(); // <--- ДОБАВИТЬ ЭТУ СТРОКУ
          }
        });

    // Инициализация монитора гарнитуры
    headsetMonitor = new HeadsetMonitor(this, new HeadsetMonitor.OnHeadsetStateChangedListener() {
      @Override
      public void onStateChanged(boolean isHeadsetWithMicConnected) {
        int color = isHeadsetWithMicConnected ? Color.GREEN : Color.GRAY;
        topBar.tvHeadsetIcon.setTextColor(color);
      }
    });
    audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

    // Принудительно ставим стартовую громкость при запуске
    int startVol = storage.getStartVolume();
    if (startVol != -1) {
      int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
      int systemVol = Math.round((startVol / 100f) * maxVol);
      audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, systemVol, 0);
    }
    updateSystemVolumeDisplay(); // Первичное отображение
    setupVolumeObserver(); // Подписка на изменения

    updateKeepScreenFlag();

    // Запускаем Vox-индикатор, если он включён в настройках
    startVoxIndicatorIfEnabled();
    // auto и pad при старте
    updateVoxLabels();

  }

  @Override
  protected void onResume() {
    super.onResume();
    if (headsetMonitor != null) {
      headsetMonitor.start();
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (headsetMonitor != null) {
      headsetMonitor.stop();
    }
  }

  private void updateKeepScreenFlag() {
    int mode = storage.getKeepScreenMode();
    boolean keepOn = mode == 2
        || (mode == 1 && recordManager != null && recordManager.isRecordingMode());
    if (keepOn) {
      getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    } else {
      getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
  }

  // ================= СЕТКА =================

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
      pv.container.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          onPadTapped(index);
        }
      });
      pv.container.setOnLongClickListener(new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
          showPadMenu(index);
          return true;
        }
      });
      padViews[i] = pv;
      updateDurationText(i);
    }

    storage.saveGridIndex(gridIndex);
    refreshPadColors();
  }

  private void onPadTapped(int index) {
    boolean hasSound = pads[index].filePath != null && new File(pads[index].filePath).exists();

    // Если автозапись включена
    if (storage.isAutoRecordEnabled()) {
      // Если пэд ПУСТОЙ — ничего не делаем
      if (!hasSound) {
        return;
      }

      // Если пэд ЗАПОЛНЕН:
      // 1. Если запись еще не идет — запускаем её
      if (!recordManager.isRecordingMode()) {
        recordManager.startAutoRecording();
      }

      // 2. Воспроизводим звук (или ставим на паузу, если это тот же пэд)
      if (index == audioPlayer.getCurrentPadIndex()) {
        audioPlayer.togglePause();
      } else {
        audioPlayer.playNew(this, index, pads[index].filePath, pads[index].volume,
            padViews[index].progress, this::refreshPadColors);
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
      audioPlayer.playNew(this, index, pads[index].filePath, pads[index].volume,
          padViews[index].progress, this::refreshPadColors);
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

  // ================= МЕНЮ ПЭДА И ИМПОРТ =================

  private void showPadMenu(final int index) {
    final String[] items = { "✏️ Переименовать", "🎤 Записать сэмпл", "📂 Импорт аудио",
        "🔊 Громкость", // <--- Новый пункт
        "🗑 Удалить сэмпл", "❌ Отмена" };
    new AlertDialog.Builder(this).setTitle("Пэд: " + pads[index].label)
        .setItems(items, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
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
                showVolumeDialog(index);
                break; // <--- Вызов диалога громкости
              case 4:
                deleteSample(index);
                break;
            }
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

  private void showVolumeDialog(final int index) {
    // Контейнер для ползунка и текстового поля
    LinearLayout layout = new LinearLayout(this);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setPadding(40, 40, 40, 40);

    // Ползунок (0-100)
    final SeekBar seekBar = new SeekBar(this);
    seekBar.setMax(100);
    seekBar.setProgress((int) (pads[index].volume * 100));

    // Текстовое поле для точного ввода
    final EditText editText = new EditText(this);
    editText.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
    editText.setText(String.valueOf((int) (pads[index].volume * 100)));
    editText.setGravity(android.view.Gravity.CENTER);

    layout.addView(seekBar);
    layout.addView(editText);

    // Синхронизация: ползунок -> поле
    seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
        if (fromUser) {
          editText.setText(String.valueOf(progress));
        }
      }

      @Override
      public void onStartTrackingTouch(SeekBar sb) {
      }

      @Override
      public void onStopTrackingTouch(SeekBar sb) {
      }
    });

    // Синхронизация: поле -> ползунок (при вводе)
    editText.addTextChangedListener(new android.text.TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
      }

      @Override
      public void afterTextChanged(android.text.Editable s) {
        String text = s.toString();
        if (!text.isEmpty()) {
          try {
            int val = Integer.parseInt(text);
            if (val > 100)
              val = 100;
            seekBar.setProgress(val);
          } catch (NumberFormatException ignored) {
          }
        }
      }
    });

    new AlertDialog.Builder(this).setTitle("Громкость пэда: " + pads[index].label).setView(layout)
        .setPositiveButton("OK", (d, w) -> {
          String text = editText.getText().toString();
          int volumePercent = 100;
          if (!text.isEmpty()) {
            try {
              volumePercent = Integer.parseInt(text);
              if (volumePercent > 100)
                volumePercent = 100;
              if (volumePercent < 0)
                volumePercent = 0;
            } catch (NumberFormatException ignored) {
            }
          }
          pads[index].volume = volumePercent / 100f;
          storage.savePads(pads);
          Toast.makeText(this, "Громкость: " + volumePercent + "%", Toast.LENGTH_SHORT).show();
        }).setNegativeButton("Отмена", null).show();
  }

  private void startVoxListening() {
    stopVoxListening();
    audioRecorder.startBackgroundListening();
    voxSilenceStart = 0;
    voxTriggerFrames = 0;

    voxRunnable = new Runnable() {
      @Override
      public void run() {
        int amp = audioRecorder.getAmplitude();
        topBar.voxLevelBar.setProgress(amp);

        boolean isVoxAuto = storage.isVoxAutoEnabled();
        int startThreshold = storage.getVoxStartThreshold();
        int stopThreshold = storage.getVoxStopThreshold();
        int timeoutSec = storage.getVoxSilenceTimeout();
        int triggerFrames = storage.getVoxTriggerFrames();

        boolean wakeOnVox = storage.isWakeScreenOnVox();
        int wakeFrames = storage.getWakeScreenFrames();

        boolean isActuallyRecording = recordManager != null && recordManager.isRecordingMode();
        boolean isPaused = audioRecorder.isPaused();

        // --- Логика пробуждения экрана ---
        if (wakeOnVox && wakeFrames > 0 && !isActuallyRecording && amp > startThreshold) {
          voxWakeFrames++; // Считаем кадры для пробуждения
          if (voxWakeFrames >= wakeFrames) {
            wakeUpScreen();
            voxWakeFrames = 0;
          }
        } else {
          voxWakeFrames = 0;
        }

        // --- Логика автостарта/автостопа ---
        if (isVoxAuto && !isPaused) {
          if (!isActuallyRecording) {
            if (amp > startThreshold) {
              voxTriggerFrames++;
              if (voxTriggerFrames >= triggerFrames) {
                voxTriggerFrames = 0;
                topBar.btnRecStart.performClick();
                voxSilenceStart = 0;
              }
            } else {
              voxTriggerFrames = 0;
            }
          } else {
            voxTriggerFrames = 0;
            if (amp < stopThreshold) { // <--- Используем порог СТОПА
              if (voxSilenceStart == 0) {
                voxSilenceStart = System.currentTimeMillis();
              } else if (System.currentTimeMillis() - voxSilenceStart > timeoutSec * 1000L) {
                topBar.btnRecStop.performClick();
                voxSilenceStart = 0;
              }
            } else {
              voxSilenceStart = 0;
            }
          }
        }

        voxHandler.postDelayed(this, 50);
      }
    };
    voxHandler.post(voxRunnable);
  }

  private void stopVoxListening() {
    if (voxRunnable != null) {
      voxHandler.removeCallbacks(voxRunnable);
      voxRunnable = null;
    }
    voxTriggerFrames = 0;
    audioRecorder.stopBackgroundListening();
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

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (volumeObserver != null) {
      getContentResolver().unregisterContentObserver(volumeObserver);
    }
    // ... остальной код ...
    if (wakeLock != null && wakeLock.isHeld())
      wakeLock.release();
    if (headsetMonitor != null)
      headsetMonitor.stop();

    stopVoxListening();
    audioPlayer.stopAll();
    if (audioRecorder != null)
      audioRecorder.stop();

  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions,
      int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (recordManager != null) {
      recordManager.onRequestPermissionResult(requestCode, permissions, grantResults);
    }
  }

  private void updateVoxLabels() {
    // Оптимизация: обновляем цвета только если состояние изменилось
    boolean isVoxAuto = storage.isVoxAutoEnabled();
    boolean isPadAuto = storage.isAutoRecordEnabled();

    topBar.setAutoLabelActive(isVoxAuto);
    topBar.setPadLabelActive(isPadAuto);
  }

  private void startVoxIndicatorIfEnabled() {
    // Активность индикатора зависит от настроек: индикатор, автостарт или идёт запись
    boolean shouldListen = storage.isVoxIndicatorEnabled() || storage.isVoxAutoEnabled()
        || (recordManager != null && recordManager.isRecordingMode());

    if (shouldListen) {
      startVoxListening(); // Запускаем обновление полоски
    } else {
      stopVoxListening(); // Останавливаем обновление
      topBar.voxLevelBar.setProgress(0); // Но не скрываем, а ставим на 0
    }
  }

  private void wakeUpScreen() {
    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
    if (pm == null)
      return;
    if (pm.isInteractive())
      return;

    if (wakeLock == null) {
      wakeLock = pm.newWakeLock(
          PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
          "SimplePad:WakeOnVox");
      wakeLock.setReferenceCounted(false);
    }
    if (!wakeLock.isHeld()) {
      wakeLock.acquire(5000);
    }
  }

  private void updateSystemVolumeDisplay() {
    if (audioManager == null || topBar.tvSystemVolume == null)
      return;

    int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    int percent = (int) ((current / (float) max) * 100);

    // Просто выводим процент без пробелов
    topBar.tvSystemVolume.setText(percent + "%");

    // Красим в зависимости от сравнения со стартовой громкостью
    int startVol = storage.getStartVolume();
    if (startVol != -1) {
      // Погрешность ±2%, чтобы компенсировать округление системных шагов
      int diff = percent - startVol;

      if (Math.abs(diff) <= 2) {
        // Совпадает (в пределах погрешности)
        topBar.tvSystemVolume.setTextColor(Color.GREEN);
      } else if (diff < 0) {
        // Текущая громкость НИЖЕ стартовой
        topBar.tvSystemVolume.setTextColor(0xFFFFC107);
      } else {
        // Текущая громкость ВЫШЕ стартовой
        topBar.tvSystemVolume.setTextColor(Color.RED);
      }
    } else {
      // Стартовая не задана
      topBar.tvSystemVolume.setTextColor(Color.GRAY);
    }
  }

  private void setupVolumeObserver() {
    volumeObserver = new ContentObserver(volumeHandler) {
      @Override
      public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        updateSystemVolumeDisplay(); // Обновляем при изменении громкости
      }
    };
    // Слушаем изменения системных настроек громкости
    getContentResolver().registerContentObserver(android.provider.Settings.System.CONTENT_URI, true,
        volumeObserver);
  }
}
