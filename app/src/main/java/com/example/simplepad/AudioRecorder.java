package com.example.simplepad;

import android.content.Context;
import android.content.Intent;
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

  // Ссылка на таймер, чтобы возобновлять его после паузы
  private TextView currentTimerView;

  private final Context context;

  // Фоновое прослушивание (Vox): на Android 8+ через VoxService, иначе локальный поток
  private VoxService voxService;
  private boolean useVoxService;

  public AudioRecorder(Context context) {
    this.context = context.getApplicationContext();
    timerHandler = new Handler(Looper.getMainLooper());
    useVoxService = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O;
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

      currentTimerView = timerView; // Сохраняем ссылку

      if (currentTimerView != null)
        startTimer(currentTimerView);
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
      } catch (Exception ignored) {
      }
    }
  }

  public void resume() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isRecording && isPaused) {
      try {
        recorder.resume();
        isPaused = false;
        startTime = System.currentTimeMillis();

        // Возобновляем таймер!
        if (currentTimerView != null)
          startTimer(currentTimerView);
      } catch (Exception ignored) {
      }
    }
  }

  public void stop() {
    if (!isRecording || recorder == null)
      return;
    try {
      recorder.stop();
    } catch (Exception ignored) {
    }
    recorder.release();
    recorder = null;
    isRecording = false;
    isPaused = false;
    stopTimer();
  }

  public boolean isRecording() {
    return isRecording;
  }

  public boolean isPaused() {
    return isPaused;
  }

  private void startTimer(TextView tv) {
    stopTimer(); // Убираем старые коллбэки
    timerRunnable = new Runnable() {
      @Override
      public void run() {
        if (!isRecording || isPaused)
          return;
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

  

  // --- Для фонового прослушивания (Vox-индикатор) ---
  // На Android 8+ используется VoxService (foreground service), иначе локальный поток
  private android.media.AudioRecord backgroundRecorder;
  private boolean isBackgroundListening = false;
  private Thread backgroundThread;
  private int backgroundAmplitude = 0;

  public void startBackgroundListening() {
    if (isBackgroundListening)
      return;
    isBackgroundListening = true;

    if (useVoxService) {
      Intent intent = new Intent(context, VoxService.class);
      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        context.startForegroundService(intent);
      } else {
        context.startService(intent);
      }
      voxService = null; // уровень придёт через колбэк ниже, если нужен
      return;
    }

    startLegacyListening();
  }

  public void stopBackgroundListening() {
    if (!isBackgroundListening)
      return;
    isBackgroundListening = false;

    if (useVoxService) {
      Intent stopIntent = new Intent(context, VoxService.class);
      stopIntent.setAction("com.example.simplepad.action.STOP_VOX");
      context.stopService(stopIntent);
      voxService = null;
      return;
    }

    stopLegacyListening();
  }

  private void startLegacyListening() {
    int sampleRate = 44100;
    int channelConfig = android.media.AudioFormat.CHANNEL_IN_MONO;
    int audioFormat = android.media.AudioFormat.ENCODING_PCM_16BIT;
    int minBufferSize = android.media.AudioRecord.getMinBufferSize(sampleRate, channelConfig,
        audioFormat);

    try {
      backgroundRecorder = new android.media.AudioRecord(
          android.media.MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat,
          minBufferSize);

      if (backgroundRecorder.getState() != android.media.AudioRecord.STATE_INITIALIZED) {
        isBackgroundListening = false;
        return;
      }

      backgroundRecorder.startRecording();

      backgroundThread = new Thread(() -> {
        short[] buffer = new short[minBufferSize / 2];
        while (isBackgroundListening) {
          int read = backgroundRecorder.read(buffer, 0, buffer.length);
          if (read > 0) {
            int max = 0;
            for (int i = 0; i < read; i++) {
              if (Math.abs(buffer[i]) > max)
                max = Math.abs(buffer[i]);
            }
            backgroundAmplitude = max;
          }
        }
      });
      backgroundThread.start();

    } catch (Exception e) {
      isBackgroundListening = false;
    }
  }

  private void stopLegacyListening() {
    isBackgroundListening = false;
    if (backgroundThread != null) {
      try {
        backgroundThread.join(500);
      } catch (Exception ignored) {
      }
      backgroundThread = null;
    }
    if (backgroundRecorder != null) {
      try {
        backgroundRecorder.stop();
      } catch (Exception ignored) {
      }
      try {
        backgroundRecorder.release();
      } catch (Exception ignored) {
      }
      backgroundRecorder = null;
    }
  }

  public int getAmplitude() {
    if (recorder != null && isRecording) {
      try {
        return recorder.getMaxAmplitude();
      } catch (Exception e) {
        return 0;
      }
    }
    // На Android 8+ уровень приходит из VoxService, иначе из локального потока
    if (useVoxService) {
      VoxService svc = VoxService.getInstance();
      return svc != null ? svc.getAmplitude() : 0;
    }
    // Если запись не идёт, возвращаем уровень из фонового слушателя
    return backgroundAmplitude;
  }

  // Уровень из VoxService (если сервис активен) — заглушка для совместимости.
  // На Android 8+ уровень приходит через сервис; здесь оставим локальную переменную.
  public void setBackgroundAmplitude(int amplitude) {
    backgroundAmplitude = amplitude;
  }
}