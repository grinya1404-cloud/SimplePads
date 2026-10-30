package com.example.simplepad;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class RecordManager {

  private final Activity activity;
  private final AudioRecorder recorder;
  private final TopBarView topBar;
  private final OnRecordingStateChanged listener;

  private boolean isRecordingMode = false;

  // Для индикатора
  private Handler indicatorHandler;
  private Runnable indicatorRunnable;

  public interface OnRecordingStateChanged {
    void onStateChanged(boolean isRecording);
  }

  public RecordManager(Activity activity, AudioRecorder recorder, TopBarView topBar,
      OnRecordingStateChanged listener) {
    this.activity = activity;
    this.recorder = recorder;
    this.topBar = topBar;
    this.listener = listener;
    this.indicatorHandler = new Handler(Looper.getMainLooper());
    setupButtons();
  }

  public boolean isRecordingMode() {
    return isRecordingMode;
  }

  private void setupButtons() {
    topBar.btnRecStart.setOnClickListener(v -> {
      if (isRecordingMode)
        return;
      if (!hasPermission()) {
        requestPermission();
        return;
      }
      String path = MediaFileManager.createRecordFilePath();
      if (path == null)
        return;

      if (recorder.start(activity, path, topBar.tvRecordTimer)) {
        setRecordingState(true);
      }
    });

    topBar.btnRecPause.setOnClickListener(v -> {
      if (!isRecordingMode)
        return;
      if (recorder.isPaused()) {
        recorder.resume();
        topBar.btnRecPause.setText("⏸");
        topBar.tvRecordIndicator.setVisibility(View.VISIBLE);
        startIndicator(); // Возобновляем индикатор
      } else {
        recorder.pause();
        topBar.btnRecPause.setText("▶️");
        topBar.tvRecordIndicator.setVisibility(View.INVISIBLE);
        stopIndicator(); // Останавливаем индикатор
      }
    });

    topBar.btnRecStop.setOnClickListener(v -> {
      if (!isRecordingMode)
        return;
      recorder.stop();
      setRecordingState(false);
      Toast.makeText(activity, "Запись сохранена", Toast.LENGTH_SHORT).show();
    });
  }

  private void setRecordingState(boolean isRecording) {
    isRecordingMode = isRecording;
    topBar.tvRecordIndicator.setVisibility(isRecording ? View.VISIBLE : View.INVISIBLE);
    topBar.btnRecStart.setEnabled(!isRecording);
    topBar.btnRecPause.setEnabled(isRecording);
    topBar.btnRecStop.setEnabled(isRecording);
    topBar.btnRecPause.setText("⏸");

    if (isRecording) {
      startIndicator();
    } else {
      stopIndicator();
      // Если Vox выключен, сбрасываем индикатор на 0
      // (но не прячем его, чтобы кнопки не двигались)
      if (!activity.getSharedPreferences("simplepad", android.content.Context.MODE_PRIVATE)
          .getBoolean("vox_indicator", false)) {
        topBar.voxLevelBar.setProgress(0);
      }
    }

    if (listener != null) {
      listener.onStateChanged(isRecording);
    }
  }

  // --- Логика обновления индикатора ---
  private void startIndicator() {
    stopIndicator(); // Убираем старые
    indicatorRunnable = new Runnable() {
      @Override
      public void run() {
        if (!isRecordingMode)
          return;
        int amp = recorder.getAmplitude();
        topBar.voxLevelBar.setProgress(amp);
        indicatorHandler.postDelayed(this, 50); // Обновляем каждые 50 мс
      }
    };
    indicatorHandler.post(indicatorRunnable);
  }

  private void stopIndicator() {
    if (indicatorRunnable != null) {
      indicatorHandler.removeCallbacks(indicatorRunnable);
      indicatorRunnable = null;
    }
  }

  public void startAutoRecording() {
    if (isRecordingMode)
      return;
    if (!hasPermission()) {
      requestPermission();
      return;
    }
    String path = MediaFileManager.createRecordFilePath();
    if (path == null)
      return;

    if (recorder.start(activity, path, topBar.tvRecordTimer)) {
      setRecordingState(true);
    }
  }

  private boolean hasPermission() {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || activity
        .checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
  }

  private void requestPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      if (!activity.shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
        showPermissionSettingsDialog();
      } else {
        activity.requestPermissions(new String[] { Manifest.permission.RECORD_AUDIO }, 100);
      }
    }
  }

  private void showPermissionSettingsDialog() {
    new AlertDialog.Builder(activity).setTitle("Нужно разрешение")
        .setMessage("Для записи нужен доступ к микрофону. Разрешите его в настройках.")
        .setPositiveButton("Настройки", (d, w) -> {
          Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
          intent.setData(Uri.parse("package:" + activity.getPackageName()));
          activity.startActivity(intent);
        }).setNegativeButton("Отмена", null).show();
  }

  public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
    if (requestCode != 100)
      return;
    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      // Разрешение получено —повторяем отложенное действие, если запись всё ещё нужна
      if (isRecordingMode)
        return;
      // Пользователь мог нажимать REC во время запроса — ничего не стартуем автоматически,
      // повторное нажатие кнопки запустит запись уже с разрешением.
    }
  }
}