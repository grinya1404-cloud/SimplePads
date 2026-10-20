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

    public SettingsManager(Activity activity, PadStorage storage, 
                           GridLayoutUpdater gridUpdater, 
                           SettingsChangeListener settingsChangeListener) {
        this.activity = activity;
        this.storage = storage;
        this.gridUpdater = gridUpdater;
        this.settingsChangeListener = settingsChangeListener;
    }

    public void showMainMenu(String currentGridName, boolean isAutoRecordEnabled) {
        String[] screenModes = {"Системно", "При записи", "Всегда"};
        int currentMode = storage.getKeepScreenMode();
        
        String voxIndicatorStatus = storage.isVoxIndicatorEnabled() ? "ВКЛ" : "ВЫКЛ";

        final String[] items = {
                "Размер сетки: " + currentGridName,
                "Автозапись при тапе: " + (isAutoRecordEnabled ? "ВКЛ" : "ВЫКЛ"),
                "Не гасить экран: " + screenModes[currentMode],
                "Vox-индикатор (уровень): " + voxIndicatorStatus // <-- Новый пункт
        };

        new AlertDialog.Builder(activity)
                .setTitle("Настройки")
                .setItems(items, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showGridSizeDialog();
                            break;
                        case 1:
                            boolean newAutoRecord = !storage.isAutoRecordEnabled();
                            storage.setAutoRecordEnabled(newAutoRecord);
                            Toast.makeText(activity, "Автозапись: " + (newAutoRecord ? "ВКЛ" : "ВЫКЛ"), Toast.LENGTH_SHORT).show();
                            if (settingsChangeListener != null) settingsChangeListener.onSettingsChanged();
                            break;
                        case 2:
                            showKeepScreenDialog();
                            break;
                        case 3:
                            boolean newVox = !storage.isVoxIndicatorEnabled();
                            storage.setVoxIndicatorEnabled(newVox);
                            Toast.makeText(activity, "Vox-индикатор: " + (newVox ? "ВКЛ" : "ВЫКЛ"), Toast.LENGTH_SHORT).show();
                            if (settingsChangeListener != null) settingsChangeListener.onSettingsChanged();
                            break;
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
                        if (settingsChangeListener != null) settingsChangeListener.onSettingsChanged();
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
                    if (settingsChangeListener != null) settingsChangeListener.onSettingsChanged();
                    dialog.dismiss();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }
}