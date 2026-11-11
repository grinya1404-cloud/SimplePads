package com.grinya.simplepad;

import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import android.content.Context;
import android.widget.ProgressBar;

import java.io.IOException;

public class AudioPlayer {

  private MediaPlayer player;
  private int currentPadIndex = -1;
  private boolean isPaused = false;

  private Handler progressHandler;
  private Runnable progressRunnable;
  private long padDuration = 0;
  private long progressStartTime = 0;
  private long progressBase = 0;
  private android.media.audiofx.LoudnessEnhancer loudnessEnhancer;

  private ProgressBar currentProgressBar;

  public AudioPlayer() {
    progressHandler = new Handler(Looper.getMainLooper());
  }

  public int getCurrentPadIndex() {
    return currentPadIndex;
  }

  public boolean isPaused() {
    return isPaused;
  }

  public void playNew(Context context, int index, String filePath, float volume,
      ProgressBar progressBar, Runnable onCompletion) {
    release();
    try {
      player = new MediaPlayer();
      player.setDataSource(filePath);
      player.prepare();

      // В playNew после player.prepare():
      if (volume > 1.0f) {
        try {
          // Отключаем старый эффект, если был
          if (loudnessEnhancer != null) {
            loudnessEnhancer.release();
            loudnessEnhancer = null;
          }

          loudnessEnhancer = new android.media.audiofx.LoudnessEnhancer(player.getAudioSessionId());

          // Переводим "volume" в дБ. Например, 200% = +6 дБ (примерно)
          // Формула: gainDb = 20 * log10(volume)
          float gainDb = (float) (20 * Math.log10(volume));
          int gainMb = (int) (gainDb * 100); // миллибеллы

          loudnessEnhancer.setTargetGain(gainMb);
          loudnessEnhancer.setEnabled(true);

          // Устанавливаем громкость MediaPlayer в 1.0, чтобы не было клиппинга
          player.setVolume(1.0f, 1.0f);
        } catch (Exception e) {
          // Если эффект не поддерживается, просто ставим громкость 1.0
          player.setVolume(1.0f, 1.0f);
        }
      } else {
        // Обычная громкость
        if (loudnessEnhancer != null) {
          loudnessEnhancer.release();
          loudnessEnhancer = null;
        }
        player.setVolume(volume, volume);
      }

      padDuration = player.getDuration();
      progressBase = 0;
      progressStartTime = System.currentTimeMillis();

      player.setVolume(volume, volume); // <--- Устанавливаем громкость
      player.start();
      player.setOnCompletionListener(mp -> {
        currentPadIndex = -1;
        isPaused = false;
        stopProgressUpdates();
        if (onCompletion != null)
          onCompletion.run();
      });

      currentPadIndex = index;
      isPaused = false;
      currentProgressBar = progressBar;
      if (currentProgressBar != null)
        currentProgressBar.setProgress(0);

      startProgressUpdates();
    } catch (IOException e) {
      Toast.makeText(context, "Ошибка воспроизведения", Toast.LENGTH_SHORT).show();
    }
  }

  public void togglePause() {
    if (player == null || currentPadIndex == -1)
      return;
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

  public void stopAll() {
    release();
    currentPadIndex = -1;
    isPaused = false;
    padDuration = 0;
    progressBase = 0;
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
    if (loudnessEnhancer != null) {
      try {
        loudnessEnhancer.release();
      } catch (Exception ignored) {
      }
      loudnessEnhancer = null;
    }
  }

  private void startProgressUpdates() {
    stopProgressUpdates();
    progressRunnable = new Runnable() {
      @Override
      public void run() {
        if (player == null || currentPadIndex == -1 || isPaused)
          return;

        long played = progressBase + (System.currentTimeMillis() - progressStartTime);
        float fraction = padDuration > 0 ? Math.min(1f, (float) played / padDuration) : 0f;

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