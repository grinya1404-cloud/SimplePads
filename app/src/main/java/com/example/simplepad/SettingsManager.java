package com.example.simplepad;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.WindowManager;
import android.widget.Toast;

public class SettingsManager {

    private final Activity activity;
    private final PadStorage storage;
    private final GridLayoutUpdater gridUpdater;
    private final Runnable onScreenModeChanged;
    private final Runnable onGridChanged;

    public interface GridLayoutUpdater {
        void updateGridSize(int newGridIndex);
    }

    public SettingsManager(Activity activity, PadStorage storage, 
                           GridLayoutUpdater gridUpdater,
                           Runnable onScreenModeChanged, 
                           Runnable onGridChanged) {
        this.activity = activity;
        this.storage = storage;
        this.gridUpdater = gridUpdater;
        this.onScreenModeChanged = onScreenModeChanged;
        this.onGridChanged = onGridChanged;
    }

    public void showMainMenu(String currentGridName, boolean isAutoRecordEnabled) {
        String[] screenModes = {"Системно", "При записи", "Всегда"};
        int currentMode = storage.getKeepScreenMode();

        final String[] items = {
                "Размер сетки: " + currentGridName,
                "Автозапись при тапе: " + (isAutoRecordEnabled ? "ВКЛ" : "ВЫКЛ"),
                "Не гасить экран: " + screenModes[currentMode]
        };

        new AlertDialog.Builder(activity)
                .setTitle("Настройки")
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        showGridSizeDialog();
                    } else if (which == 1) {
                        boolean newState = !storage.isAutoRecordEnabled();
                        storage.setAutoRecordEnabled(newState);
                        Toast.makeText(activity, "Автозапись: " + (newState ? "ВКЛ" : "ВЫКЛ"), Toast.LENGTH_SHORT).show();
                    } else if (which == 2) {
                        showKeepScreenDialog();
                    }
                })
                .show();
    }

    private void showGridSizeDialog() {
        String[] gridNames = {"3×3", "4×4", "4×5", "5×5"};
        int currentIndex = storage.getGridIndex();

        new AlertDialog.Builder(activity)
                .setTitle("Размер сетки")
                .setSingleChoiceItems(gridNames, currentIndex, (dialog, which) -> {
                    if (which != currentIndex) {
                        gridUpdater.updateGridSize(which);
                        onGridChanged.run();
                        Toast.makeText(activity, "Сетка: " + gridNames[which], Toast.LENGTH_SHORT).show();
                    }
                    dialog.dismiss();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showKeepScreenDialog() {
        final String[] modes = {"Системно (по умолчанию)", "Только при записи", "Всегда, пока открыто"};
        new AlertDialog.Builder(activity)
                .setTitle("Не гасить экран")
                .setSingleChoiceItems(modes, storage.getKeepScreenMode(), (dialog, which) -> {
                    storage.setKeepScreenMode(which);
                    onScreenModeChanged.run(); // Применяем сразу
                    dialog.dismiss();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }
}