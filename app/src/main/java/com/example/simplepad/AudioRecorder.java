package com.example.simplepad;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

public class AudioRecorder {

    private MediaRecorder recorder;
    private boolean isRecording = false;
    private boolean isPaused = false;
    private Handler timerHandler;
    private Runnable timerRunnable;
    private long startTime = 0;
    private long pausedTime = 0;

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
            isPaused = false;
            startTime = System.currentTimeMillis();
            pausedTime = 0;
            
            if (timerView != null) startTimer(timerView);
            return true;
        } catch (Exception e) {
            Toast.makeText(context, "Ошибка записи: " + e.getMessage(), Toast.LENGTH_LONG).show();
            recorder = null;
            isRecording = false;
            return false;
        }
    }

    public void pause() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isRecording && !isPaused) {
            try {
                recorder.pause();
                isPaused = true;
                pausedTime += System.currentTimeMillis() - startTime;
            } catch (Exception ignored) {}
        }
    }

    public void resume() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isRecording && isPaused) {
            try {
                recorder.resume();
                isPaused = false;
                startTime = System.currentTimeMillis();
            } catch (Exception ignored) {}
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
        isPaused = false;
        stopTimer();
    }

    public boolean isRecording() { return isRecording; }
    public boolean isPaused() { return isPaused; }

    private void startTimer(TextView tv) {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isRecording || isPaused) return;
                long ms = pausedTime + (System.currentTimeMillis() - startTime);
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