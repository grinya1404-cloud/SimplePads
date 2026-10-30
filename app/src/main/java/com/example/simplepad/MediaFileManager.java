package com.example.simplepad;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MediaFileManager {

  // Папка для диктофона (Music/SimplePad, с запасным вариантом)
  public static File getRecordDirectory(Context context) {
    // Пытаемся использовать Music/SimplePad
    File musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
    File appDir = new File(musicDir, "SimplePad");

    // Проверяем, можно ли туда писать
    if (appDir.exists() || appDir.mkdirs()) {
      if (appDir.canWrite()) {
        return appDir;
      }
    }

    // Если не получилось — используем внутреннюю память приложения
    return context.getFilesDir();
  }

  // Генерация пути для новой записи диктофона
  public static String createRecordFilePath(Context context) {
    File dir = getRecordDirectory(context);
    String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
        .format(new Date());
    return new File(dir, "REC_" + timeStamp + ".m4a").getAbsolutePath();
  }

  // Копирование файла из системного проводника во внутреннюю память приложения
  public static String copyAudioToInternalStorage(Context context, Uri sourceUri, int padIndex) {
    try {
      File destFile = new File(context.getFilesDir(), "imported_" + padIndex + ".m4a");
      InputStream is = context.getContentResolver().openInputStream(sourceUri);
      FileOutputStream os = new FileOutputStream(destFile);

      if (is == null)
        return null;

      byte[] buffer = new byte[4096];
      int length;
      while ((length = is.read(buffer)) > 0) {
        os.write(buffer, 0, length);
      }
      os.close();
      is.close();

      return destFile.getAbsolutePath();
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  // Форматирование длительности из миллисекунд (например, "1:23")
  public static String formatDuration(int durationMs) {
    int seconds = (durationMs / 1000) % 60;
    int minutes = (durationMs / 1000) / 60;
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
  }

  // Получение длительности файла
  public static int getAudioDuration(String path) {
    try {
      android.media.MediaPlayer mp = new android.media.MediaPlayer();
      mp.setDataSource(path);
      mp.prepare();
      int duration = mp.getDuration();
      mp.release();
      return duration;
    } catch (Exception e) {
      return 0;
    }
  }
}