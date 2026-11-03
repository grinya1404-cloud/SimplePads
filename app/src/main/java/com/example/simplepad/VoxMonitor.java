package com.example.simplepad;

import android.media.AudioRecord;
import android.media.MediaRecorder;

/**
 * Фоновый мониторинг уровня микрофона (Vox).
 * Читает амплитуру в отдельном потоке, не ведёт запись файла.
 */
public class VoxMonitor {

    private AudioRecord recorder;
    private boolean isListening = false;
    private Thread thread;
    private int amplitude = 0;

    /**
     * Запускает мониторинг. Вызывать после проверки разрешения RECORD_AUDIO.
     */
    public void start() {
        if (isListening) {
            return;
        }
        isListening = true;

        int sampleRate = 44100;
        int channelConfig = android.media.AudioFormat.CHANNEL_IN_MONO;
        int audioFormat = android.media.AudioFormat.ENCODING_PCM_16BIT;
        int minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);

        try {
            recorder = new AudioRecord(
                    MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat,
                    minBufferSize);
            if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
                isListening = false;
                return;
            }
            recorder.startRecording();

            thread = new Thread(() -> {
                short[] buffer = new short[minBufferSize / 2];
                while (isListening) {
                    int read = recorder.read(buffer, 0, buffer.length);
                    if (read > 0) {
                        int max = 0;
                        for (int i = 0; i < read; i++) {
                            int v = Math.abs(buffer[i]);
                            if (v > max) {
                                max = v;
                            }
                        }
                        amplitude = max;
                    }
                }
            });
            thread.start();
        } catch (Exception e) {
            isListening = false;
        }
    }

    /**
     * Останавливает мониторинг и освобождает ресурсы.
     */
    public void stop() {
        isListening = false;
        if (thread != null) {
            try {
                thread.join(500);
            } catch (Exception ignored) {
            }
            thread = null;
        }
        if (recorder != null) {
            try {
                recorder.stop();
            } catch (Exception ignored) {
            }
            try {
                recorder.release();
            } catch (Exception ignored) {
            }
            recorder = null;
        }
    }

    public int getAmplitude() {
        return amplitude;
    }

    public boolean isListening() {
        return isListening;
    }
}
