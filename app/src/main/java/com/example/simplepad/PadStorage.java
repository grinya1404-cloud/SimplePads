package com.example.simplepad;

import android.content.Context;
import android.content.SharedPreferences;

public class PadStorage {
  private static final int MAX_PADS = 25;
  private SharedPreferences prefs;

  public PadStorage(Context context) {
    prefs = context.getSharedPreferences("simplepad", Context.MODE_PRIVATE);
  }

  public int getKeepScreenMode() {
    return prefs.getInt("keep_screen_mode", 0); // 0 по умолчанию
  }

  public void setKeepScreenMode(int mode) {
    prefs.edit().putInt("keep_screen_mode", mode).apply();
  }

  public void savePads(Pad[] pads) {
    SharedPreferences.Editor ed = prefs.edit();
    for (int i = 0; i < MAX_PADS; i++) {
      ed.putString("pad_" + i + "_label", pads[i].label);
      ed.putString("pad_" + i + "_path", pads[i].filePath);
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

  public int getGridIndex() {
    return prefs.getInt("gridIndex", 1);
  }

  public void saveGridIndex(int index) {
    prefs.edit().putInt("gridIndex", index).apply();
  }

  // --- Новые методы для автозаписи ---
  public boolean isAutoRecordEnabled() {
    return prefs.getBoolean("auto_record", false); // По умолчанию выключено
  }

  public void setAutoRecordEnabled(boolean enabled) {
    prefs.edit().putBoolean("auto_record", enabled).apply();
  }

  // --- Настройка Vox-индикатора ---
  public boolean isVoxIndicatorEnabled() {
    return prefs.getBoolean("vox_indicator", false);
  }

  public void setVoxIndicatorEnabled(boolean enabled) {
    prefs.edit().putBoolean("vox_indicator", enabled).apply();
  }

  // --- Vox автостарт/автостоп ---
  public boolean isVoxAutoEnabled() {
    return prefs.getBoolean("vox_auto", false);
  }

  public void setVoxAutoEnabled(boolean enabled) {
    prefs.edit().putBoolean("vox_auto", enabled).apply();
  }

  public int getVoxThreshold() {
    return prefs.getInt("vox_threshold", 3000);
  }

  public void setVoxThreshold(int threshold) {
    prefs.edit().putInt("vox_threshold", threshold).apply();
  }

  public int getVoxSilenceTimeout() {
    return prefs.getInt("vox_timeout", 3); // Секунды
  }

  public void setVoxSilenceTimeout(int seconds) {
    prefs.edit().putInt("vox_timeout", seconds).apply();
  }
}