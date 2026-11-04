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
    return prefs.getInt("vox_timeout", 15); // Секунды (по умолчанию 15 для диалогов с паузами)
  }

  public void setVoxSilenceTimeout(int seconds) {
    prefs.edit().putInt("vox_timeout", seconds).apply();
  }

  // --- Количество подряд идущих кадров выше порога для автозапуска Vox ---
  public int getVoxTriggerFrames() {
    return prefs.getInt("vox_trigger_frames", 4);
  }

  public void setVoxTriggerFrames(int frames) {
    prefs.edit().putInt("vox_trigger_frames", frames).apply();
  }

  // --- Vox: раздельные пороги ---
  public int getVoxStartThreshold() {
    return prefs.getInt("vox_start_threshold", 3000); // По умолчанию 3000
  }

  public void setVoxStartThreshold(int threshold) {
    prefs.edit().putInt("vox_start_threshold", threshold).apply();
  }

  public int getVoxStopThreshold() {
    return prefs.getInt("vox_stop_threshold", 1000); // По умолчанию 1000 (ниже старта)
  }

  public void setVoxStopThreshold(int threshold) {
    prefs.edit().putInt("vox_stop_threshold", threshold).apply();
  }

  // --- Настройка активации экрана по Vox ---
  public boolean isWakeScreenOnVox() {
    return prefs.getBoolean("wake_screen_on_vox", false);
  }

  public void setWakeScreenOnVox(boolean enabled) {
    prefs.edit().putBoolean("wake_screen_on_vox", enabled).apply();
  }

  // --- Кадры для активации экрана ---
  public int getWakeScreenFrames() {
    return prefs.getInt("wake_screen_frames", 0); // 0 = выкл
  }

  public void setWakeScreenFrames(int frames) {
    prefs.edit().putInt("wake_screen_frames", frames).apply();
  }

  // --- Стартовая громкость ---
  public int getStartVolume() {
    return prefs.getInt("start_volume", -1); // -1 означает "не задана"
  }

  public void setStartVolume(int volumePercent) {
    prefs.edit().putInt("start_volume", volumePercent).apply();
  }

  public void savePads(Pad[] pads) {
    SharedPreferences.Editor ed = prefs.edit();
    for (int i = 0; i < MAX_PADS; i++) {
      ed.putString("pad_" + i + "_label", pads[i].label);
      ed.putString("pad_" + i + "_path", pads[i].filePath);
      ed.putFloat("pad_" + i + "_volume", pads[i].volume);
      ed.putInt("pad_" + i + "_offset", pads[i].startOffsetMs); // <--- Добавлено
    }
    ed.apply();
  }

  public Pad[] loadPads() {
    Pad[] pads = new Pad[MAX_PADS];
    for (int i = 0; i < MAX_PADS; i++) {
      String label = prefs.getString("pad_" + i + "_label", String.valueOf(i + 1));
      String path = prefs.getString("pad_" + i + "_path", null);
      float volume = prefs.getFloat("pad_" + i + "_volume", 1.0f);
      int offset = prefs.getInt("pad_" + i + "_offset", 0); // <--- Добавлено
      pads[i] = new Pad(label, path);
      pads[i].volume = volume;
      pads[i].startOffsetMs = offset;
    }
    return pads;
  }
}