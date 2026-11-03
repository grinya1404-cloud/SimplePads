package com.example.simplepad;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.IOException;

public class AudioPlayer {

    private MediaPlayer player;
    private int currentPadIndex = -1;
    private boolean isPaused = false;

    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;
    private long padDuration = 0;
    private long progressBase = 0;
    private long progressStartTime = 0;

    private ProgressBar currentProgressBar;

    public int getCurrentPadIndex() {
        return currentPadIndex;
    }

    public boolean isPaused() {
        return isPaused;
    }

    /**
     * Начинает воспроизведение файла.
     *
     * @param context    контекст для Toast
     * @param index      индекс пэда
     * @param filePath   путь к аудиофайлу
     * @param progressBar индикатор прогресса (может быть null)
     * @param onCompletion callback по окончании (может быть null)
     */
    public void playNew(Context context, int index, String filePath,
                        ProgressBar progressBar, Runnable onCompletion) {
        release();
        try {
            player = new MediaPlayer();
            player.setDataSource(filePath);
            player.prepare();

            padDuration = player.getDuration();
            progressBase = 0;
            progressStartTime = System.currentTimeMillis();

            player.setOnCompletionListener(mp -> {
                resetState();
                stopProgressUpdates();
                if (onCompletion != null) {
                    onCompletion.run();
                }
            });

            player.start();
            currentPadIndex = index;
            isPaused = false;
            currentProgressBar = progressBar;
            if (currentProgressBar != null) {
                currentProgressBar.setProgress(0);
            }
            startProgressUpdates();
        } catch (IOException e) {
            Toast.makeText(context, "Ошибка воспроизведения", Toast.LENGTH_SHORT).show();
            release();
        }
    }

    /**
     * Пауза / продолжение воспроизведения.
     */
    public void togglePause() {
        if (player == null || currentPadIndex == -1) {
            return;
        }
        if (isPaused) {
            progressStartTime = System.currentTimeMillis();
            player.start();
            isPaused = false;
            startProgressUpdates();
        } else {
            progressBase += System.currentTimeMillis() - progressStartTime;
            player.pause();
            isPaused = true;
            stopProgressUpdates();
        }
    }

    /**
     * Останавливает воспроизведение и сбрасывает состояние.
     */
    public void stopAll() {
        release();
        resetState();
        stopProgressUpdates();
    }

    private void release() {
        if (player != null) {
            try {
                player.stop();
            } catch (Exception ignored) {
            }
            player.release();
            player = null;
        }
    }

    private void resetState() {
        currentPadIndex = -1;
        isPaused = false;
        padDuration = 0;
        progressBase = 0;
        progressStartTime = 0;
        currentProgressBar = null;
    }

    private void startProgressUpdates() {
        stopProgressUpdates();
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (player == null || currentPadIndex == -1 || isPaused) {
                    return;
                }
                long played = progressBase + (System.currentTimeMillis() - progressStartTime);
                float fraction = padDuration > 0
                        ? Math.min(1f, (float) played / padDuration)
                        : 0f;

                if (currentProgressBar != null) {
                    currentProgressBar.setProgress((int) (fraction * 1000));
                }

                if (fraction < 1f) {
                    progressHandler.postDelayed(this, 30);
                }
            }
        };
        progressHandler.post(progressRunnable);
    }

    private void stopProgressUpdates() {
        if (progressRunnable != null) {
            progressHandler.removeCallbacks(progressRunnable);
            progressRunnable = null;
        }
    }
}
