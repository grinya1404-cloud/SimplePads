package com.example.simplepad;

import android.app.Activity;
import android.app.AlertDialog;
import android.widget.Toast;

public class SettingsManager {

  private final Activity activity;
  private final PadStorage storage;
  private final GridLayoutUpdater gridUpdater;
  private final SettingsChangeListener settingsChangeListener; // <-- Объявлено как поле

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

    String voxIndicatorStatus = storage.isVoxIndicatorEnabled() ? "ВКЛ" : "ВЫКЛ";
    String voxAutoStatus = storage.isVoxAutoEnabled() ? "ВКЛ" : "ВЫКЛ";
    String thresholdVal = String.valueOf(storage.getVoxThreshold());
    String timeoutVal = storage.getVoxSilenceTimeout() + " сек";

    final String[] items = { "Размер сетки: " + currentGridName,
        "Автозапись при тапе: " + (isAutoRecordEnabled ? "ВКЛ" : "ВЫКЛ"),
        "Не гасить экран: " + screenModes[currentMode], "Vox-индикатор: " + voxIndicatorStatus,
        "Vox (Автостарт/стоп): " + voxAutoStatus, "Порог Vox: " + thresholdVal,
        "Таймаут тишины: " + timeoutVal };

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
          boolean newVoxInd = !storage.isVoxIndicatorEnabled();
          storage.setVoxIndicatorEnabled(newVoxInd);
          Toast.makeText(activity, "Vox-индикатор: " + (newVoxInd ? "ВКЛ" : "ВЫКЛ"),
              Toast.LENGTH_SHORT).show();
          if (settingsChangeListener != null)
            settingsChangeListener.onSettingsChanged();
          break;
        case 4:
          boolean newVoxAuto = !storage.isVoxAutoEnabled();
          storage.setVoxAutoEnabled(newVoxAuto);
          Toast.makeText(activity, "Vox автостарт: " + (newVoxAuto ? "ВКЛ" : "ВЫКЛ"),
              Toast.LENGTH_SHORT).show();
          if (settingsChangeListener != null)
            settingsChangeListener.onSettingsChanged();
          break;
        case 5:
          showVoxThresholdDialog();
          break;
        case 6:
          showVoxTimeoutDialog();
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
        }).setNegativeButton("Отмена", null).show();
  }

  private void showVoxTimeoutDialog() {
    final int[] timeouts = { 2, 3, 5 };
    final String[] names = { "2 секунды", "3 секунды", "5 секунд" };
    int current = 1;
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
}