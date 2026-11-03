package com.example.simplepad;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class MediaFileManager {

    private MediaFileManager() {
    }

    // Папка для записей: Music/SimplePad с fallback на внутреннюю память
    public static File getRecordDirectory(Context context) {
        File musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
        File appDir = new File(musicDir, "SimplePad");

        if ((appDir.exists() || appDir.mkdirs()) && appDir.canWrite()) {
            return appDir;
        }
        return context.getFilesDir();
    }

    // Путь для новой записи диктофона
    public static String createRecordFilePath(Context context) {
        File dir = getRecordDirectory(context);
        String stamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
                .format(new Date());
        return new File(dir, "REC_" + stamp + ".m4a").getAbsolutePath();
    }

    // Копирование аудио из системного проводника во внутреннюю память приложения
    public static String copyAudioToInternalStorage(Context context, Uri sourceUri, int padIndex) {
        File destFile = new File(context.getFilesDir(), "imported_" + padIndex + ".m4a");
        InputStream is = null;
        FileOutputStream os = null;
        try {
            is = context.getContentResolver().openInputStream(sourceUri);
            if (is == null)
                return null;

            os = new FileOutputStream(destFile);
            byte[] buffer = new byte[8192];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            return destFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            closeQuietly(is);
            closeQuietly(os);
        }
    }

    // Длина файла в миллисекундах
    public static int getAudioDuration(String path) {
        MediaPlayer mp = null;
        try {
            mp = new MediaPlayer();
            mp.setDataSource(path);
            mp.prepare();
            return mp.getDuration();
        } catch (Exception e) {
            return 0;
        } finally {
            if (mp != null) {
                mp.release();
            }
        }
    }

    // Форматирование длительности: "М:СС"
    public static String formatDuration(int durationMs) {
        int seconds = (durationMs / 1000) % 60;
        int minutes = (durationMs / 1000) / 60;
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }

    private static void closeQuietly(java.io.Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (Exception ignored) {
            }
        }
    }
}
