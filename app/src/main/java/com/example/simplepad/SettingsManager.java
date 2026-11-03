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

        String[] items = {
                "Размер сетки: " + currentGridName,
                "Автозапись при тапе: " + (isAutoRecordEnabled ? "ВКЛ" : "ВЫКЛ"),
                "Не гасить экран: " + screenModes[currentMode],
                "Vox-настройки"
        };

        new AlertDialog.Builder(activity)
                .setTitle("Настройки")
                .setItems(items, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showGridSizeDialog();
                            break;
                        case 1:
                            toggleAutoRecord();
                            break;
                        case 2:
                            showKeepScreenDialog();
                            break;
                        case 3:
                            showVoxSubmenu();
                            break;
                    }
                })
                .show();
    }

    private void toggleAutoRecord() {
        boolean newAuto = !storage.isAutoRecordEnabled();
        storage.setAutoRecordEnabled(newAuto);
        Toast.makeText(activity, "Автозапись: " + (newAuto ? "ВКЛ" : "ВЫКЛ"), Toast.LENGTH_SHORT)
                .show();
        notifyChanged();
    }

    // ================= ДИАЛОГИ НАСТРОЕК =================

    private void showGridSizeDialog() {
        String[] gridNames = { "3×3", "4×4", "4×5", "5×5" };
        int currentIndex = storage.getGridIndex();

        new AlertDialog.Builder(activity)
                .setTitle("Размер сетки")
                .setSingleChoiceItems(gridNames, currentIndex, (dialog, which) -> {
                    if (which != currentIndex) {
                        gridUpdater.updateGridSize(which);
                        notifyChanged();
                        Toast.makeText(activity, "Сетка: " + gridNames[which], Toast.LENGTH_SHORT)
                                .show();
                    }
                    dialog.dismiss();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showKeepScreenDialog() {
        String[] modes = {
                "Системно (по умолчанию)",
                "Только при записи",
                "Всегда, пока открыто"
        };
        new AlertDialog.Builder(activity)
                .setTitle("Не гасить экран")
                .setSingleChoiceItems(modes, storage.getKeepScreenMode(), (dialog, which) -> {
                    storage.setKeepScreenMode(which);
                    notifyChanged();
                    dialog.dismiss();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    // ================= ПОДМЕНЮ VOX =================

    private void showVoxSubmenu() {
        String[] items = {
                "Vox-индикатор: " + status(storage.isVoxIndicatorEnabled()),
                "Vox (Автостарт/стоп): " + status(storage.isVoxAutoEnabled()),
                "Порог Vox: " + storage.getVoxThreshold(),
                "Таймаут тишины: " + storage.getVoxSilenceTimeout() + " сек",
                "Кадров для срабатывания: " + storage.getVoxTriggerFrames()
        };

        new AlertDialog.Builder(activity)
                .setTitle("Vox-настройки")
                .setItems(items, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            toggleVoxIndicator();
                            break;
                        case 1:
                            toggleVoxAuto();
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
                    dialog.dismiss();
                })
                .show();
    }

    private void toggleVoxIndicator() {
        boolean enabled = !storage.isVoxIndicatorEnabled();
        storage.setVoxIndicatorEnabled(enabled);
        Toast.makeText(activity, "Vox-индикатор: " + status(enabled), Toast.LENGTH_SHORT).show();
        notifyChanged();
        showVoxSubmenu();
    }

    private void toggleVoxAuto() {
        boolean enabled = !storage.isVoxAutoEnabled();
        storage.setVoxAutoEnabled(enabled);
        Toast.makeText(activity, "Vox автостарт: " + status(enabled), Toast.LENGTH_SHORT).show();
        notifyChanged();
        showVoxSubmenu();
    }

    private void showVoxThresholdDialog() {
        int[] thresholds = { 1000, 3000, 6000 };
        String[] names = { "Низкий (1000)", "Средний (3000)", "Высокий (6000)" };
        int current = indexOf(thresholds, storage.getVoxThreshold());

        new AlertDialog.Builder(activity)
                .setTitle("Порог срабатывания")
                .setSingleChoiceItems(names, current, (dialog, which) -> {
                    storage.setVoxThreshold(thresholds[which]);
                    notifyChanged();
                    dialog.dismiss();
                    showVoxSubmenu();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showVoxTimeoutDialog() {
        int[] timeouts = { 5, 10, 15, 30, 60 };
        String[] names = { "5 секунд", "10 секунд", "15 секунд", "30 секунд", "1 минута" };
        int current = indexOf(timeouts, storage.getVoxSilenceTimeout());

        new AlertDialog.Builder(activity)
                .setTitle("Таймаут тишины")
                .setSingleChoiceItems(names, current, (dialog, which) -> {
                    storage.setVoxSilenceTimeout(timeouts[which]);
                    notifyChanged();
                    dialog.dismiss();
                    showVoxSubmenu();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showVoxTriggerFramesDialog() {
        int[] frames = { 2, 3, 4, 5, 6 };
        String[] names = { "2 кадра", "3 кадра", "4 кадра", "5 кадров", "6 кадров" };
        int current = indexOf(frames, storage.getVoxTriggerFrames());

        new AlertDialog.Builder(activity)
                .setTitle("Кадров выше порога")
                .setSingleChoiceItems(names, current, (dialog, which) -> {
                    storage.setVoxTriggerFrames(frames[which]);
                    notifyChanged();
                    dialog.dismiss();
                    showVoxSubmenu();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private static String status(boolean enabled) {
        return enabled ? "ВКЛ" : "ВЫКЛ";
    }

    private static int indexOf(int[] values, int target) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == target) {
                return i;
            }
        }
        return 0;
    }

    private void notifyChanged() {
        if (settingsChangeListener != null) {
            settingsChangeListener.onSettingsChanged();
        }
    }
}
