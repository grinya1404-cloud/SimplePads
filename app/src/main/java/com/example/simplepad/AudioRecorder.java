package com.example.simplepad;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

public class AudioRecorder {

    private MediaRecorder recorder;
    private boolean isRecording = false;
    private Handler timerHandler;
    private Runnable timerRunnable;
    private long startTime = 0;

    public AudioRecorder() {
        timerHandler = new Handler(Looper.getMainLooper());
    }

    public boolean start(Context context, String path, TextView timerView) {
        try {
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setAudioSamplingRate(44100);
            recorder.setAudioEncodingBitRate(128000);
            recorder.setOutputFile(path);
            recorder.prepare();
            recorder.start();
            isRecording = true;
            startTime = System.currentTimeMillis();
            
            if (timerView != null) startTimer(timerView);
            return true;
        } catch (Exception e) {
            Toast.makeText(context, "Ошибка записи: " + e.getMessage(), Toast.LENGTH_LONG).show();
            recorder = null;
            isRecording = false;
            return false;
        }
    }

    public void stop() {
        if (!isRecording || recorder == null) return;
        try {
            recorder.stop();
        } catch (Exception ignored) {}
        recorder.release();
        recorder = null;
        isRecording = false;
        stopTimer();
    }

    public boolean isRecording() {
        return isRecording;
    }

    private void startTimer(TextView tv) {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isRecording) return;
                long ms = System.currentTimeMillis() - startTime;
                tv.setText(String.format("%02d:%02d", ms / 60000, (ms / 1000) % 60));
                timerHandler.postDelayed(this, 200);
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void stopTimer() {
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
            timerRunnable = null;
        }
    }
}