package com.example.simplepad;

import android.app.Activity;
import android.app.AlertDialog;
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

    final String[] items = { "Размер сетки: " + currentGridName,
        "Автозапись при тапе: " + (isAutoRecordEnabled ? "ВКЛ" : "ВЫКЛ"),
        "Не гасить экран: " + screenModes[currentMode], "Vox-настройки" };

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
          showVoxSubmenu();
          break;
      }
    }).show();
  }

  // ================= ДИАЛОГИ НАСТРОЕК =================

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

  // ================= ПОДМЕНЮ VOX =================

  private void showVoxSubmenu() {
    String voxIndicatorStatus = storage.isVoxIndicatorEnabled() ? "ВКЛ" : "ВЫКЛ";
    String voxAutoStatus = storage.isVoxAutoEnabled() ? "ВКЛ" : "ВЫКЛ";
    String thresholdVal = String.valueOf(storage.getVoxThreshold());
    String timeoutVal = storage.getVoxSilenceTimeout() + " сек";
    String framesVal = String.valueOf(storage.getVoxTriggerFrames());

    final String[] items = { "Vox-индикатор: " + voxIndicatorStatus,
        "Vox (Автостарт/стоп): " + voxAutoStatus, "Порог Vox: " + thresholdVal,
        "Таймаут тишины: " + timeoutVal, "Кадров для срабатывания: " + framesVal };

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
          showVoxSubmenu(); // Возвращаемся в меню Vox
          break;
        case 1:
          boolean newVoxAuto = !storage.isVoxAutoEnabled();
          storage.setVoxAutoEnabled(newVoxAuto);
          // storage.setVoxIndicatorEnabled(newVoxAuto); // <--- УДАЛИТЬ ЭТУ СТРОКУ
          Toast.makeText(activity, "Vox автостарт: " + (newVoxAuto ? "ВКЛ" : "ВЫКЛ"),
              Toast.LENGTH_SHORT).show();
          if (settingsChangeListener != null)
            settingsChangeListener.onSettingsChanged();
          dialog.dismiss();
          showVoxSubmenu();
          break;
        case 2:
          showVoxThresholdDialog();
          break;
        case 3:
          showVoxTimeoutDialog();
          break;
        case 4:
          showVoxTriggerFramesDialog();
          break;
      }
    }).show();
  }

  private void showVoxThresholdDialog() {
    final int[] thresholds = { 1000, 3000, 6000 };
    final String[] names = { "Низкий (1000)", "Средний (3000)", "Высокий (6000)" };
    int current = 1;
    for (int i = 0; i < thresholds.length; i++) {
      if (thresholds[i] == storage.getVoxThreshold()) {
        current = i;
        break;
      }
    }
    new AlertDialog.Builder(activity).setTitle("Порог срабатывания")
        .setSingleChoiceItems(names, current, (dialog, which) -> {
          storage.setVoxThreshold(thresholds[which]);
          if (settingsChangeListener != null)
            settingsChangeListener.onSettingsChanged();
          dialog.dismiss();
          showVoxSubmenu(); // Возврат в меню
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
          showVoxSubmenu(); // Возврат в меню
        }).setNegativeButton("Отмена", null).show();
  }

  private void showVoxTriggerFramesDialog() {
    final int[] frames = { 2, 3, 4, 5, 6 };
    final String[] names = { "2 кадра", "3 кадра", "4 кадра", "5 кадров", "6 кадров" };
    int current = 2;
    for (int i = 0; i < frames.length; i++) {
      if (frames[i] == storage.getVoxTriggerFrames()) {
        current = i;
        break;
      }
    }
    new AlertDialog.Builder(activity).setTitle("Кадров выше порога")
        .setSingleChoiceItems(names, current, (dialog, which) -> {
          storage.setVoxTriggerFrames(frames[which]);
          if (settingsChangeListener != null)
            settingsChangeListener.onSettingsChanged();
          dialog.dismiss();
          showVoxSubmenu(); // Возврат в меню
        }).setNegativeButton("Отмена", null).show();
  }
}