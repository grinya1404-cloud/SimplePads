package com.example.simplepad;

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

    private final Runnable checkRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isMonitoring) {
                return;
            }
            updateState(checkViaAudioManager());
            handler.postDelayed(this, 2000);
        }
    };

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            if (Intent.ACTION_HEADSET_PLUG.equals(intent.getAction())) {
                updateState(checkViaAudioManager());
            }
        }
    };

    public HeadsetMonitor(Context context, OnHeadsetStateChangedListener listener) {
        this.context = context.getApplicationContext();
        this.audioManager = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);
        this.listener = listener;
    }

    public void start() {
        if (isMonitoring) {
            return;
        }
        isMonitoring = true;
        handler.post(checkRunnable);

        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        context.registerReceiver(receiver, filter);

        updateState(checkViaAudioManager());
    }

    public void stop() {
        if (!isMonitoring) {
            return;
        }
        isMonitoring = false;
        handler.removeCallbacks(checkRunnable);
        try {
            context.unregisterReceiver(receiver);
        } catch (Exception ignored) {
        }
    }

    private boolean checkViaAudioManager() {
        if (audioManager == null) {
            return false;
        }
        AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS);
        for (AudioDeviceInfo device : devices) {
            if (device.getType() == AudioDeviceInfo.TYPE_WIRED_HEADSET) {
                return true;
            }
        }
        return false;
    }

    private void updateState(boolean newState) {
        if (newState != lastState) {
            lastState = newState;
            if (listener != null) {
                listener.onStateChanged(newState);
            }
        }
    }
}
