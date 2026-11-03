package com.example.simplepad;

import android.content.Context;
import android.content.SharedPreferences;

public class PadStorage {
    private static final String PREFS_NAME = "simplepad";
    private static final int MAX_PADS = 25;

    // Ключи настроек
    private static final String KEY_KEEP_SCREEN = "keep_screen_mode";
    private static final String KEY_GRID_INDEX = "grid_index";
    private static final String KEY_AUTO_RECORD = "auto_record";
    private static final String KEY_VOX_INDICATOR = "vox_indicator";
    private static final String KEY_VOX_AUTO = "vox_auto";
    private static final String KEY_VOX_THRESHOLD = "vox_threshold";
    private static final String KEY_VOX_TIMEOUT = "vox_timeout";
    private static final String KEY_VOX_TRIGGER_FRAMES = "vox_trigger_frames";

    private final SharedPreferences prefs;

    public PadStorage(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // --- Экран ---
    public int getKeepScreenMode() {
        return prefs.getInt(KEY_KEEP_SCREEN, 0);
    }

    public void setKeepScreenMode(int mode) {
        prefs.edit().putInt(KEY_KEEP_SCREEN, mode).apply();
    }

    // --- Сетка ---
    public int getGridIndex() {
        return prefs.getInt(KEY_GRID_INDEX, 1);
    }

    public void setGridIndex(int index) {
        prefs.edit().putInt(KEY_GRID_INDEX, index).apply();
    }

    // --- Пэды ---
    public void savePads(Pad[] pads) {
        SharedPreferences.Editor ed = prefs.edit();
        for (int i = 0; i < MAX_PADS; i++) {
            Pad pad = pads[i];
            if (pad == null)
                continue;
            ed.putString("pad_" + i + "_label", pad.getLabel());
            ed.putString("pad_" + i + "_path", pad.getFilePath());
        }
        ed.apply();
    }

    public Pad[] loadPads() {
        Pad[] pads = new Pad[MAX_PADS];
        for (int i = 0; i < MAX_PADS; i++) {
            String label = prefs.getString("pad_" + i + "_label", String.valueOf(i + 1));
            String path = prefs.getString("pad_" + i + "_path", null);
            pads[i] = new Pad(label, path);
        }
        return pads;
    }

    // --- Автозапись ---
    public boolean isAutoRecordEnabled() {
        return prefs.getBoolean(KEY_AUTO_RECORD, false);
    }

    public void setAutoRecordEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_AUTO_RECORD, enabled).apply();
    }

    // --- Vox-индикатор ---
    public boolean isVoxIndicatorEnabled() {
        return prefs.getBoolean(KEY_VOX_INDICATOR, false);
    }

    public void setVoxIndicatorEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_VOX_INDICATOR, enabled).apply();
    }

    // --- Vox автостарт/стоп ---
    public boolean isVoxAutoEnabled() {
        return prefs.getBoolean(KEY_VOX_AUTO, false);
    }

    public void setVoxAutoEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_VOX_AUTO, enabled).apply();
    }

    public int getVoxThreshold() {
        return prefs.getInt(KEY_VOX_THRESHOLD, 3000);
    }

    public void setVoxThreshold(int threshold) {
        prefs.edit().putInt(KEY_VOX_THRESHOLD, threshold).apply();
    }

    public int getVoxSilenceTimeout() {
        return prefs.getInt(KEY_VOX_TIMEOUT, 15);
    }

    public void setVoxSilenceTimeout(int seconds) {
        prefs.edit().putInt(KEY_VOX_TIMEOUT, seconds).apply();
    }

    public int getVoxTriggerFrames() {
        return prefs.getInt(KEY_VOX_TRIGGER_FRAMES, 4);
    }

    public void setVoxTriggerFrames(int frames) {
        prefs.edit().putInt(KEY_VOX_TRIGGER_FRAMES, frames).apply();
    }
}
