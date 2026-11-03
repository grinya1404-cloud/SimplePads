package com.example.simplepad;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;

public class VoxService extends Service {

    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "simplepad_vox";

    private final VoxMonitor voxMonitor = new VoxMonitor();

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "com.example.simplepad.action.STOP_VOX".equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        startForeground(NOTIFICATION_ID, buildNotification());
        voxMonitor.start();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        voxMonitor.stop();
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
        return buildNotificationLegacy();
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

    private Notification buildNotificationLegacy() {
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

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Vox-мониторинг", NotificationManager.IMPORTANCE_LOW);
        channel.setSound(null, null);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) {
            nm.createNotificationChannel(channel);
        }
    }
}
