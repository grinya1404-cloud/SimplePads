package com.grinya.simplepad;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;

public class HeadsetMonitor {

    public interface OnHeadsetStateChangedListener {
        void onStateChanged(boolean isHeadsetWithMicConnected);
    }

    private final Context context;
    private final AudioManager audioManager;
    private final OnHeadsetStateChangedListener listener;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isMonitoring = false;
    private boolean lastState = false;

    // Периодическая проверка (раз в 2 секунды)
    private final Runnable checkRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isMonitoring) return;
            updateState(checkViaAudioManager());
            handler.postDelayed(this, 2000);
        }
    };

    // Мгновенная реакция на подключение/отключение
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            if (Intent.ACTION_HEADSET_PLUG.equals(intent.getAction())) {
                int state = intent.getIntExtra("state", 0);
                int mic = intent.getIntExtra("microphone", 0);
                // Если воткнули гарнитуру с микрофоном
                boolean connected = (state == 1 && mic == 1);
                // Но для надёжности перепроверяем через AudioManager
                updateState(checkViaAudioManager() || connected);
            }
        }
    };

    public HeadsetMonitor(Context context, OnHeadsetStateChangedListener listener) {
        this.context = context.getApplicationContext();
        this.audioManager = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);
        this.listener = listener;
    }

    public void start() {
        if (isMonitoring) return;
        isMonitoring = true;

        // 1. Запускаем периодическую проверку
        handler.post(checkRunnable);

        // 2. Подписываемся на системное событие
        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        context.registerReceiver(receiver, filter);

        // 3. Сразу проверяем текущее состояние
        updateState(checkViaAudioManager());
    }

    public void stop() {
        if (!isMonitoring) return;
        isMonitoring = false;
        handler.removeCallbacks(checkRunnable);
        try {
            context.unregisterReceiver(receiver);
        } catch (Exception ignored) {}
    }

    // Проверка через AudioManager.getDevices()
    private boolean checkViaAudioManager() {
        if (audioManager == null) return false;
        AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS);
        for (AudioDeviceInfo device : devices) {
            if (device.getType() == AudioDeviceInfo.TYPE_WIRED_HEADSET) {
                return true;
            }
        }
        return false;
    }

    // Обновляем состояние только если оно изменилось (оптимизация)
    private void updateState(boolean newState) {
        if (newState != lastState) {
            lastState = newState;
            if (listener != null) {
                listener.onStateChanged(newState);
            }
        }
    }
}