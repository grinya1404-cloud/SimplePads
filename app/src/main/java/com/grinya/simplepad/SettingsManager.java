package com.grinya.simplepad;

import android.app.Activity;
import android.app.AlertDialog;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Toast;

public class SettingsManager {

  private final Activity activity;
  private final PadStorage storage;
  private final GridLayoutUpdater gridUpdater;
  private final SettingsChangeListener settingsChangeListener;

  public interface GridLayoutUpdater {
    void updateGridSize(int newGridIndex);
  }

  public interface SettingsChangeListener {
    void onSettingsChanged();
  }

  public SettingsManager(Activity activity, PadStorage storage, GridLayoutUpdater gridUpdater,
      SettingsChangeListener settingsChangeListener) {
    this.activity = activity;
    this.storage = storage;
    this.gridUpdater = gridUpdater;
    this.settingsChangeListener = settingsChangeListener;
  }

  public void showMainMenu(String currentGridName, boolean isAutoRecordEnabled) {
    String[] screenModes = { "Системно", "При записи", "Всегда" };
    int currentMode = storage.getKeepScreenMode();

    // Статус для активации экрана
    int wakeFrames = storage.getWakeScreenFrames();
    boolean wakeEnabled = storage.isWakeScreenOnVox() && wakeFrames > 0;
    String wakeStatus = wakeEnabled ? (wakeFrames + " кадров") : "Выкл";

    // Статус стартовой громкости
    int startVolInt = storage.getStartVolume();
    String startVol = (startVolInt == -1) ? "Не задана" : (startVolInt + "%");

    final String[] items = { "Размер сетки: " + currentGridName,
        "Автозапись при тапе: " + (isAutoRecordEnabled ? "ВКЛ" : "ВЫКЛ"),
        "Не гасить экран: " + screenModes[currentMode], "Стартовая громкость: " + startVol,
        "Активировать экран по Vox: " + wakeStatus, "Vox-настройки" };

    new AlertDialog.Builder(activity).setTitle("Настройки").setItems(items, (dialog, which) -> {
      switch (which) {
        case 0:
          showGridSizeDialog();
          break;
        case 1:
          boolean newAuto = !storage.isAutoRecordEnabled();
          storage.setAutoRecordEnabled(newAuto);
          Toast.makeText(activity, "Автозапись: " + (newAuto ? "ВКЛ" : "ВЫКЛ"), Toast.LENGTH_SHORT)
              .show();
          if (settingsChangeListener != null)
            settingsChangeListener.onSettingsChanged();
          break;
        case 2:
          showKeepScreenDialog();
          break;
        case 3:
          showStartVolumeDialog(); // <--- Новый диалог
          break;
        case 4:
          showWakeScreenDialog();
          break;
        case 5:
          showVoxSubmenu();
          break;
      }
    }).show();
  }

  private void showStartVolumeDialog() {
    LinearLayout layout = new LinearLayout(activity);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setPadding(40, 40, 40, 40);

    final SeekBar seekBar = new SeekBar(activity);
    seekBar.setMax(100);
    int currentVol = storage.getStartVolume();
    if (currentVol == -1)
      currentVol = 100; // По умолчанию 100%, если не задана
    seekBar.setProgress(currentVol);

    final EditText editText = new EditText(activity);
    editText.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
    editText.setText(String.valueOf(currentVol));
    editText.setGravity(android.view.Gravity.CENTER);

    layout.addView(seekBar);
    layout.addView(editText);

    seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
        if (fromUser)
          editText.setText(String.valueOf(progress));
      }

      @Override
      public void onStartTrackingTouch(SeekBar sb) {
      }

      @Override
      public void onStopTrackingTouch(SeekBar sb) {
      }
    });

    editText.addTextChangedListener(new android.text.TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
      }

      @Override
      public void afterTextChanged(android.text.Editable s) {
        if (!s.toString().isEmpty()) {
          try {
            int val = Integer.parseInt(s.toString());
            seekBar.setProgress(Math.min(val, 100));
          } catch (NumberFormatException ignored) {
          }
        }
      }
    });

    new AlertDialog.Builder(activity).setTitle("Стартовая громкость").setView(layout)
        .setPositiveButton("OK", (d, w) -> {
          String text = editText.getText().toString();
          int vol = 100;
          if (!text.isEmpty()) {
            try {
              vol = Integer.parseInt(text);
              if (vol > 100)
                vol = 100;
              if (vol < 0)
                vol = 0;
            } catch (NumberFormatException ignored) {
            }
          }
          storage.setStartVolume(vol);
          if (settingsChangeListener != null)
            settingsChangeListener.onSettingsChanged();
          Toast.makeText(activity, "Стартовая громкость: " + vol + "%", Toast.LENGTH_SHORT).show();
        }).setNegativeButton("Отмена", null).show();
  }

  private void showGridSizeDialog() {
    String[] gridNames = { "3×3", "4×4", "4×5", "5×5" };
    int currentIndex = storage.getGridIndex();

    new AlertDialog.Builder(activity).setTitle("Размер сетки")
        .setSingleChoiceItems(gridNames, currentIndex, (dialog, which) -> {
          if (which != currentIndex) {
            gridUpdater.updateGridSize(which);
            if (settingsChangeListener != null)
              settingsChangeListener.onSettingsChanged();
            Toast.makeText(activity, "Сетка: " + gridNames[which], Toast.LENGTH_SHORT).show();
          }
          dialog.dismiss();
        }).setNegativeButton("Отмена", null).show();
  }

  private void showKeepScreenDialog() {
    final String[] modes = { "Системно (по умолчанию)", "Только при записи",
        "Всегда, пока открыто" };
    new AlertDialog.Builder(activity).setTitle("Не гасить экран")
        .setSingleChoiceItems(modes, storage.getKeepScreenMode(), (dialog, which) -> {
          storage.setKeepScreenMode(which);
          if (settingsChangeListener != null)
            settingsChangeListener.onSettingsChanged();
          dialog.dismiss();
        }).setNegativeButton("Отмена", null).show();
  }

  private void showWakeScreenDialog() {
    final int[] frames = { 0, 2, 4, 6, 8 };
    final String[] names = { "Выкл", "2 кадра", "4 кадра", "6 кадров", "8 кадров" };

    int current = 0;
    for (int i = 0; i < frames.length; i++) {
      if (frames[i] == storage.getWakeScreenFrames()) {
        current = i;
        break;
      }
    }

    new AlertDialog.Builder(activity).setTitle("Активировать экран по Vox")
        .setSingleChoiceItems(names, current, (dialog, which) -> {
          int selectedFrames = frames[which];
          storage.setWakeScreenFrames(selectedFrames);
          storage.setWakeScreenOnVox(selectedFrames > 0);

          Toast.makeText(activity, selectedFrames > 0 ? "Экран: " + names[which] : "Экран: Выкл",
              Toast.LENGTH_SHORT).show();

          if (settingsChangeListener != null)
            settingsChangeListener.onSettingsChanged();
          dialog.dismiss();
        }).setNegativeButton("Отмена", null).show();
  }

  // ================= ПОДМЕНЮ VOX =================

  private void showVoxSubmenu() {
    String voxIndicatorStatus = storage.isVoxIndicatorEnabled() ? "ВКЛ" : "ВЫКЛ";
    String voxAutoStatus = storage.isVoxAutoEnabled() ? "ВКЛ" : "ВЫКЛ";
    String startThresholdVal = String.valueOf(storage.getVoxStartThreshold());
    String stopThresholdVal = String.valueOf(storage.getVoxStopThreshold());
    String timeoutVal = storage.getVoxSilenceTimeout() + " сек";
    String framesVal = String.valueOf(storage.getVoxTriggerFrames());

    final String[] items = { "Vox-индикатор: " + voxIndicatorStatus,
        "Vox (Автостарт/стоп): " + voxAutoStatus, "Порог СТАРТА: " + startThresholdVal,
        "Порог СТОПА: " + stopThresholdVal, "Таймаут тишины: " + timeoutVal,
        "Кадров для старта записи: " + framesVal };

    new AlertDialog.Builder(activity).setTitle("Vox-настройки").setItems(items, (dialog, which) -> {
      switch (which) {
        case 0:
          boolean newVoxInd = !storage.isVoxIndicatorEnabled();
          storage.setVoxIndicatorEnabled(newVoxInd);
          Toast.makeText(activity, "Vox-индикатор: " + (newVoxInd ? "ВКЛ" : "ВЫКЛ"),
              Toast.LENGTH_SHORT).show();
          if (settingsChangeListener != null)
            settingsChangeListener.onSettingsChanged();
          dialog.dismiss();
          showVoxSubmenu();
          break;
        case 1:
          boolean newVoxAuto = !storage.isVoxAutoEnabled();
          storage.setVoxAutoEnabled(newVoxAuto);
          Toast.makeText(activity, "Vox автостарт: " + (newVoxAuto ? "ВКЛ" : "ВЫКЛ"),
              Toast.LENGTH_SHORT).show();
          if (settingsChangeListener != null)
            settingsChangeListener.onSettingsChanged();
          dialog.dismiss();
          showVoxSubmenu();
          break;
        case 2:
          showVoxStartThresholdDialog();
          break;
        case 3:
          showVoxStopThresholdDialog();
          break;
        case 4:
          showVoxTimeoutDialog();
          break;
        case 5:
          showVoxTriggerFramesDialog();
          break;
      }
    }).show();
  }

  private void showVoxStartThresholdDialog() {
    final int[] thresholds = { 1000, 2000, 3000, 4000, 5000, 6000 };
    final String[] names = { "1000 (Низкий)", "2000", "3000 (Средний)", "4000", "5000",
        "6000 (Высокий)" };
    int current = 2;
    for (int i = 0; i < thresholds.length; i++) {
      if (thresholds[i] == storage.getVoxStartThreshold()) {
        current = i;
        break;
      }
    }
    new AlertDialog.Builder(activity).setTitle("Порог СТАРТА записи")
        .setSingleChoiceItems(names, current, (dialog, which) -> {
          storage.setVoxStartThreshold(thresholds[which]);
          if (settingsChangeListener != null)
            settingsChangeListener.onSettingsChanged();
          dialog.dismiss();
          showVoxSubmenu();
        }).setNegativeButton("Отмена", null).show();
  }

  private void showVoxStopThresholdDialog() {
    final int[] thresholds = { 100, 200, 300, 400, 500, 1000, 1500, 2000, 3000 };
    final String[] names = { "100 (Очень тихо)", "200", "300", "400", "500", "1000", "1500", "2000",
        "3000" };
    int current = 4; // По умолчанию 500
    for (int i = 0; i < thresholds.length; i++) {
      if (thresholds[i] == storage.getVoxStopThreshold()) {
        current = i;
        break;
      }
    }
    new AlertDialog.Builder(activity).setTitle("Порог СТОПА записи")
        .setSingleChoiceItems(names, current, (dialog, which) -> {
          storage.setVoxStopThreshold(thresholds[which]);
          if (settingsChangeListener != null)
            settingsChangeListener.onSettingsChanged();
          dialog.dismiss();
          showVoxSubmenu();
        }).setNegativeButton("Отмена", null).show();
  }

  private void showVoxTimeoutDialog() {
    final int[] timeouts = { 5, 10, 15, 30, 60 };
    final String[] names = { "5 секунд", "10 секунд", "15 секунд", "30 секунд", "1 минута" };
    int current = 2;
    for (int i = 0; i < timeouts.length; i++) {
      if (timeouts[i] == storage.getVoxSilenceTimeout()) {
        current = i;
        break;
      }
    }
    new AlertDialog.Builder(activity).setTitle("Таймаут тишины")
        .setSingleChoiceItems(names, current, (dialog, which) -> {
          storage.setVoxSilenceTimeout(timeouts[which]);
          if (settingsChangeListener != null)
            settingsChangeListener.onSettingsChanged();
          dialog.dismiss();
          showVoxSubmenu();
        }).setNegativeButton("Отмена", null).show();
  }

  private void showVoxTriggerFramesDialog() {
    final int[] frames = { 2, 4, 8, 16, 32 };
    final String[] names = { "2 кадра", "4 кадра", "8 кадров", "16 кадров", "32 кадра" };

    int current = 1; // По умолчанию 4 кадра
    for (int i = 0; i < frames.length; i++) {
      if (frames[i] == storage.getVoxTriggerFrames()) {
        current = i;
        break;
      }
    }

    new AlertDialog.Builder(activity).setTitle("Кадров для старта записи")
        .setMessage(
            "Чем больше кадров, тем дольше сигнал должен держаться выше порога для старта записи.")
        .setSingleChoiceItems(names, current, (dialog, which) -> {
          storage.setVoxTriggerFrames(frames[which]);
          if (settingsChangeListener != null)
            settingsChangeListener.onSettingsChanged();
          dialog.dismiss();
          showVoxSubmenu();
        }).setNegativeButton("Отмена", null).show();
  }
}