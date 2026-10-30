package com.example.simplepad;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

public class VoxService extends Service {

    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "simplepad_vox";

    /** Текущий экземпляр сервиса, чтобы получать уровень амплитуды извне. */
    private static VoxService instance;

    public static VoxService getInstance() {
        return instance;
    }

    public int getAmplitude() {
        return backgroundAmplitude;
    }

    private AudioRecord backgroundRecorder;
    private boolean isListening = false;
    private Thread backgroundThread;
    private int backgroundAmplitude = 0;

    private Handler uiHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        uiHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "com.example.simplepad.action.STOP_VOX".equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        startForeground(NOTIFICATION_ID, buildNotification());
        startListening();
        return START_STICKY;
    }

    private void startListening() {
        if (isListening)
            return;
        isListening = true;

        int sampleRate = 44100;
        int channelConfig = android.media.AudioFormat.CHANNEL_IN_MONO;
        int audioFormat = android.media.AudioFormat.ENCODING_PCM_16BIT;
        int minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);

        try {
            backgroundRecorder = new AudioRecord(
                    MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat,
                    minBufferSize);

            if (backgroundRecorder.getState() != AudioRecord.STATE_INITIALIZED) {
                isListening = false;
                return;
            }

            backgroundRecorder.startRecording();

            backgroundThread = new Thread(() -> {
                short[] buffer = new short[minBufferSize / 2];
                while (isListening) {
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
            isListening = false;
        }
    }

    public void stopListening() {
        isListening = false;
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

    @Override
    public void onDestroy() {
        stopListening();
        instance = null;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Notification buildNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return buildNotificationO();
        }
        // Android 7 (API 24-25): каналы уведомлений не поддерживаются
        Intent stopIntent = new Intent(this, VoxService.class);
        stopIntent.setAction("com.example.simplepad.action.STOP_VOX");
        PendingIntent stopPending = PendingIntent.getService(
                this, 0, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new Notification.Builder(this)
                .setContentTitle("SimplePad")
                .setContentText("Vox-мониторинг микрофона")
                .setSmallIcon(android.R.drawable.ic_notification_overlay)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_media_pause, "Остановить мониторинг", stopPending)
                .build();
    }

    private Notification buildNotificationO() {
        createNotificationChannel();

        Intent stopIntent = new Intent(this, VoxService.class);
        stopIntent.setAction("com.example.simplepad.action.STOP_VOX");
        PendingIntent stopPending = PendingIntent.getService(
                this, 0, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("SimplePad")
                .setContentText("Vox-мониторинг микрофона")
                .setSmallIcon(android.R.drawable.ic_notification_overlay)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_media_pause, "Остановить мониторинг", stopPending)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Vox-мониторинг", NotificationManager.IMPORTANCE_LOW);
        channel.setSound(null, null);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null)
            nm.createNotificationChannel(channel);
    }
}
