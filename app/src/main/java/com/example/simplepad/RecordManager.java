package com.example.simplepad;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

public class RecordManager {

    private final Activity activity;
    private final AudioRecorder recorder;
    private final TopBarView topBar;
    private final OnRecordingStateChanged listener;

    private boolean isRecordingMode = false;

    public interface OnRecordingStateChanged {
        void onStateChanged(boolean isRecording);
    }

    public RecordManager(Activity activity, AudioRecorder recorder, TopBarView topBar,
                         OnRecordingStateChanged listener) {
        this.activity = activity;
        this.recorder = recorder;
        this.topBar = topBar;
        this.listener = listener;
        setupButtons();
    }

    public boolean isRecordingMode() {
        return isRecordingMode;
    }

    private void setupButtons() {
        topBar.getBtnRecStart().setOnClickListener(v -> {
            if (isRecordingMode) {
                return;
            }
            if (!hasPermission()) {
                requestPermission();
                return;
            }
            startRecording();
        });

        topBar.getBtnRecPause().setOnClickListener(v -> {
            if (!isRecordingMode) {
                return;
            }
            if (recorder.isPaused()) {
                recorder.resume();
                topBar.getBtnRecPause().setText("⏸");
                topBar.setRecordingDot(true);
            } else {
                recorder.pause();
                topBar.getBtnRecPause().setText("▶️");
                topBar.setRecordingDot(false);
            }
        });

        topBar.getBtnRecStop().setOnClickListener(v -> {
            if (!isRecordingMode) {
                return;
            }
            recorder.stop();
            setRecordingState(false);
            Toast.makeText(activity, "Запись сохранена", Toast.LENGTH_SHORT).show();
        });
    }

    private void startRecording() {
        String path = MediaFileManager.createRecordFilePath(activity);
        if (path == null) {
            return;
        }
        if (recorder.start(activity, path, topBar.getRecordTimer())) {
            setRecordingState(true);
        }
    }

    public void startAutoRecording() {
        if (isRecordingMode) {
            return;
        }
        if (!hasPermission()) {
            requestPermission();
            return;
        }
        startRecording();
    }

    private void setRecordingState(boolean isRecording) {
        isRecordingMode = isRecording;
        topBar.getBtnRecStart().setEnabled(!isRecording);
        topBar.getBtnRecPause().setEnabled(isRecording);
        topBar.getBtnRecStop().setEnabled(isRecording);
        topBar.getBtnRecPause().setText("⏸");
        topBar.setRecordingDot(isRecording);

        if (!isRecording) {
            topBar.getRecordTimer().setText("--:--");
        }

        if (listener != null) {
            listener.onStateChanged(isRecording);
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
        if (requestCode != 100) {
            return;
        }
    }
}
