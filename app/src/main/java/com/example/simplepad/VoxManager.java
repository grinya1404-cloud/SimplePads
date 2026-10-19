package com.example.simplepad;

import android.os.Handler;
import android.os.Looper;

public class VoxManager {

  public interface VoxListener {
    void onVoxLevel(int level);

    void onVoxStart();

    void onVoxStop();
  }

  private final AudioRecorder recorder;
  private final PadStorage storage;
  private final VoxListener listener;

  private Handler handler;
  private boolean isRunning = false;
  private long silenceStartTime = 0;
  private boolean isRecordingActive = false;

  public VoxManager(AudioRecorder recorder, PadStorage storage, VoxListener listener) {
    this.recorder = recorder;
    this.storage = storage;
    this.listener = listener;
    this.handler = new Handler(Looper.getMainLooper());
  }

  public boolean isRecordingActive() {
    return isRecordingActive;
  }

  public void start() {
    if (isRunning)
      return;
    isRunning = true;
    isRecordingActive = false;
    silenceStartTime = 0;
    handler.post(loop);
  }

  public void stop() {
    isRunning = false;
    handler.removeCallbacks(loop);
    if (isRecordingActive) {
      recorder.stop();
      isRecordingActive = false;
      listener.onVoxStop();
    }
  }

  public void restart() {
    stop();
    start();
  }

  // Принудительно сообщаем Vox, что запись началась (например, по кнопке REC)
  public void forceStart() {
    isRecordingActive = true;
    silenceStartTime = 0;
  }

  public void setPaused(boolean paused) {
    // Заглушка, если нужно
  }

  private Runnable loop = new Runnable() {
    @Override
    public void run() {
      if (!isRunning)
        return;

      // Если запись НЕ идет, мы просто ждем.
      // Мы не читаем getMaxAmplitude(), чтобы не трогать уничтоженный recorder.
      if (!isRecordingActive) {
        int amp = recorder.getAmplitude();
        listener.onVoxLevel(amp);

        if (amp > storage.getVoxThreshold()) {
          listener.onVoxStart();
          isRecordingActive = true;
          silenceStartTime = 0;
        }
      } else {
        // Если запись ИДЕТ, тогда читаем уровень и следим за тишиной
        int amp = recorder.getAmplitude();
        listener.onVoxLevel(amp);

        if (amp < storage.getVoxThreshold()) {
          if (silenceStartTime == 0) {
            silenceStartTime = System.currentTimeMillis();
          } else if (System.currentTimeMillis() - silenceStartTime > storage.getVoxSilenceTimeout()
              * 1000L) {
            recorder.stop();
            isRecordingActive = false;
            silenceStartTime = 0;
            listener.onVoxStop();
          }
        } else {
          silenceStartTime = 0;
        }
      }

      handler.postDelayed(this, 50);
    }
  };
}