package com.example.simplepad;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

  private static final String[] GRID_NAMES = { "3×3", "4×4", "4×5", "5×5" };
  private static final int[][] GRID_SIZES = { { 3, 3 }, { 4, 4 }, { 4, 5 }, { 5, 5 } };
  private static final int MAX_PADS = 25;

  private static final int COLOR_IDLE = 0xFF3A7BD5;
  private static final int COLOR_ACTIVE = 0xFF1FA84B;
  private static final int COLOR_PAUSED = 0xFFE8A33D;
  private static final int COLOR_PROGRESS = 0xAAFFFFFF;

  private static final int REQ_AUDIO = 100;

  private static class PadView {
    FrameLayout container;
    ProgressBar progress;
    TextView label;
  }

  private Pad[] pads = new Pad[MAX_PADS];
  private PadView[] padViews = new PadView[MAX_PADS];

  private int gridIndex = 1;
  private GridLayout grid;

  private AudioPlayer audioPlayer;
  private AudioRecorder audioRecorder;

  private SharedPreferences prefs;

  // --- Диктофон (Верхняя панель) ---
  private TextView tvRecordIndicator;
  private TextView tvRecordTimer;
  private Button btnRecStart, btnRecPause, btnRecStop;
  private boolean isRecordingMode = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    prefs = getSharedPreferences("simplepad", Context.MODE_PRIVATE);
    audioPlayer = new AudioPlayer();
    audioRecorder = new AudioRecorder();

    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(Color.parseColor("#1E1E1E"));

    // ===== ВЕРХНЯЯ ПАНЕЛЬ ДИКТОФОНА =====
    LinearLayout topBar = new LinearLayout(this);
    topBar.setOrientation(LinearLayout.HORIZONTAL);
    topBar.setGravity(Gravity.CENTER_VERTICAL);
    topBar.setPadding(16, 16, 16, 8);
    topBar.setBackgroundColor(Color.parseColor("#2C2C2C"));

    tvRecordIndicator = new TextView(this);
    tvRecordIndicator.setText("⚪");
    tvRecordIndicator.setTextSize(24);
    tvRecordIndicator.setTextColor(Color.RED);
    tvRecordIndicator.setPadding(0, 0, 8, 0);

    tvRecordTimer = new TextView(this);
    tvRecordTimer.setText("00:00");
    tvRecordTimer.setTextSize(18);
    tvRecordTimer.setTextColor(Color.WHITE);
    tvRecordTimer.setPadding(0, 0, 16, 0);
    tvRecordTimer.setVisibility(TextView.GONE);

    btnRecStart = new Button(this);
    btnRecStart.setText("🎤");
    btnRecPause = new Button(this);
    btnRecPause.setText("⏸");
    btnRecStop = new Button(this);
    btnRecStop.setText("⏹");

    btnRecPause.setEnabled(false);
    btnRecStop.setEnabled(false);

    LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    btnParams.setMargins(8, 0, 8, 0);

    topBar.addView(tvRecordIndicator);
    topBar.addView(tvRecordTimer);
    topBar.addView(btnRecStart, btnParams);
    topBar.addView(btnRecPause, btnParams);
    topBar.addView(btnRecStop, btnParams);

    // ===== СЕТКА ПЭДОВ =====
    grid = new GridLayout(this);
    LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
    grid.setLayoutParams(gridParams);
    grid.setPadding(8, 8, 8, 8);

    // ===== НИЖНЯЯ ПАНЕЛЬ (Управление пэдами) =====
    LinearLayout bottomBar = new LinearLayout(this);
    bottomBar.setOrientation(LinearLayout.HORIZONTAL);
    bottomBar.setGravity(Gravity.CENTER);
    bottomBar.setPadding(8, 8, 8, 16);

    Button btnPause = new Button(this);
    btnPause.setText("⏸  Pause");
    Button btnStop = new Button(this);
    btnStop.setText("⏹  Stop");
    Button btnSettings = new Button(this);
    btnSettings.setText("⚙");

    LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0,
        LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
    bp.setMargins(8, 0, 8, 0);
    btnPause.setLayoutParams(bp);
    btnStop.setLayoutParams(bp);

    LinearLayout.LayoutParams bpSmall = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    bpSmall.setMargins(8, 0, 8, 0);
    btnSettings.setLayoutParams(bpSmall);

    btnPause.setOnClickListener(v -> {
      audioPlayer.togglePause();
      refreshPadColors();
    });
    btnStop.setOnClickListener(v -> {
      audioPlayer.stopAll();
      refreshPadColors();
    });
    btnSettings.setOnClickListener(v -> showGridDialog());

    bottomBar.addView(btnPause);
    bottomBar.addView(btnStop);
    bottomBar.addView(btnSettings);

    root.addView(topBar);
    root.addView(grid);
    root.addView(bottomBar);
    setContentView(root);

    loadPads();
    gridIndex = prefs.getInt("gridIndex", 1);
    applyGrid();

    // ===== ЛОГИКА КНОПОК ДИКТОФОНА =====

    btnRecStart.setOnClickListener(v -> {
      if (isRecordingMode)
        return;

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(
          Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
        requestAudioPermission();
        return;
      }

      String path = createRecordFilePath();
      if (path == null)
        return;

      if (audioRecorder.start(MainActivity.this, path, tvRecordTimer)) {
        isRecordingMode = true;
        tvRecordIndicator.setText("🔴");
        tvRecordTimer.setVisibility(TextView.VISIBLE);
        btnRecStart.setEnabled(false);
        btnRecPause.setEnabled(true);
        btnRecStop.setEnabled(true);
        btnRecPause.setText("⏸");
      }
    });

    btnRecPause.setOnClickListener(v -> {
      if (!isRecordingMode)
        return;
      if (audioRecorder.isPaused()) {
        audioRecorder.resume();
        btnRecPause.setText("⏸");
        tvRecordIndicator.setText("🔴");
      } else {
        audioRecorder.pause();
        btnRecPause.setText("▶️");
        tvRecordIndicator.setText("⚪");
      }
    });

    btnRecStop.setOnClickListener(v -> {
      if (!isRecordingMode)
        return;
      audioRecorder.stop();
      isRecordingMode = false;
      tvRecordIndicator.setText("⚪");
      tvRecordTimer.setVisibility(TextView.GONE);
      tvRecordTimer.setText("00:00");
      btnRecStart.setEnabled(true);
      btnRecPause.setEnabled(false);
      btnRecStop.setEnabled(false);
      Toast.makeText(this, "Запись сохранена в Music/SimplePad", Toast.LENGTH_SHORT).show();
    });
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    audioPlayer.stopAll();
    audioRecorder.stop();
  }

  // ================= СЕТКА =================

  private void applyGrid() {
    int cols = GRID_SIZES[gridIndex][0];
    int rows = GRID_SIZES[gridIndex][1];
    int visible = cols * rows;

    if (audioPlayer.getCurrentPadIndex() >= visible) {
      audioPlayer.stopAll();
    }

    grid.removeAllViews();
    grid.setColumnCount(cols);
    grid.setRowCount(rows);

    for (int i = 0; i < visible; i++) {
      final int index = i;

      FrameLayout container = new FrameLayout(this);
      container.setBackgroundColor(COLOR_IDLE);
      container.setClickable(true);
      container.setFocusable(true);

      ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
      progress.setMax(1000);
      progress.setProgress(0);
      progress.setProgressTintList(ColorStateList.valueOf(COLOR_PROGRESS));
      progress.setPadding(0, 0, 0, 0);
      progress.setScaleY(65f);
      FrameLayout.LayoutParams progParams = new FrameLayout.LayoutParams(
          FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
      progress.setLayoutParams(progParams);
      container.addView(progress);

      TextView label = new TextView(this);
      label.setText(pads[i].label);
      label.setTextSize(20);
      label.setTextColor(Color.WHITE);
      label.setGravity(Gravity.CENTER);
      FrameLayout.LayoutParams labelParams = new FrameLayout.LayoutParams(
          FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
      label.setLayoutParams(labelParams);
      container.addView(label);

      container.setOnClickListener(v -> onPadTapped(index));
      container.setOnLongClickListener(v -> {
        showPadMenu(index);
        return true;
      });

      GridLayout.LayoutParams p = new GridLayout.LayoutParams();
      p.width = 0;
      p.height = 0;
      p.columnSpec = GridLayout.spec(i % cols, 1f);
      p.rowSpec = GridLayout.spec(i / cols, 1f);
      p.setMargins(6, 6, 6, 6);
      container.setLayoutParams(p);

      PadView pv = new PadView();
      pv.container = container;
      pv.progress = progress;
      pv.label = label;
      padViews[i] = pv;

      grid.addView(container);
    }

    prefs.edit().putInt("gridIndex", gridIndex).apply();
    refreshPadColors();
  }

  private void onPadTapped(int index) {
    if (pads[index].filePath == null) {
      showPadMenu(index);
      return;
    }

    if (index == audioPlayer.getCurrentPadIndex()) {
      audioPlayer.togglePause();
    } else {
      audioPlayer.playNew(this, index, pads[index].filePath, padViews[index].progress,
          this::refreshPadColors);
    }
    refreshPadColors();
  }

  private void refreshPadColors() {
    int cols = GRID_SIZES[gridIndex][0];
    int rows = GRID_SIZES[gridIndex][1];
    int visible = cols * rows;
    int currentPad = audioPlayer.getCurrentPadIndex();
    boolean isPaused = audioPlayer.isPaused();

    for (int i = 0; i < visible; i++) {
      PadView pv = padViews[i];
      if (pv == null)
        continue;

      int bg = COLOR_IDLE;
      if (i == currentPad) {
        bg = isPaused ? COLOR_PAUSED : COLOR_ACTIVE;
      }
      pv.container.setBackgroundColor(bg);

      if (i != currentPad) {
        pv.progress.setProgress(0);
      }
    }
  }

  // ================= МЕНЮ ПЭДА =================

  private void showPadMenu(final int index) {
    final String[] items = { "✏️ Переименовать", "🎤 Записать сэмпл", "📂 Загрузить из записей",
        "🗑 Удалить сэмпл", "❌ Отмена" };

    new AlertDialog.Builder(this).setTitle("Пэд: " + pads[index].label)
        .setItems(items, (dialog, which) -> {
          switch (which) {
            case 0:
              renamePad(index);
              break;
            case 1:
              recordSample(index);
              break;
            case 2:
              showRecordingsList(index);
              break;
            case 3:
              deleteSample(index);
              break;
          }
        }).show();
  }

  private void showRecordingsList(final int padIndex) {
    File dir = getRecordDirectory();
    File[] files = dir.listFiles((d, name) -> name.endsWith(".m4a"));
    if (files == null || files.length == 0) {
      Toast.makeText(this, "Папка с записями пуста", Toast.LENGTH_SHORT).show();
      return;
    }

    // Сортируем по дате (сначала новые)
    Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

    final String[] fileNames = new String[files.length];
    for (int i = 0; i < files.length; i++) {
      fileNames[i] = files[i].getName();
    }

    new AlertDialog.Builder(this).setTitle("Выберите запись")
        .setItems(fileNames, (dialog, which) -> {
          pads[padIndex].filePath = files[which].getAbsolutePath();
          savePads();
          Toast.makeText(this, "Запись привязана к пэду", Toast.LENGTH_SHORT).show();
        }).setNegativeButton("Отмена", null).show();
  }

  private void renamePad(final int index) {
    EditText input = new EditText(this);
    input.setText(pads[index].label);
    input.setSelection(input.getText().length());

    new AlertDialog.Builder(this).setTitle("Новое имя пэда").setView(input)
        .setPositiveButton("OK", (d, w) -> {
          String name = input.getText().toString().trim();
          if (!name.isEmpty()) {
            pads[index].label = name;
            if (padViews[index] != null)
              padViews[index].label.setText(name);
            savePads();
          }
        }).setNegativeButton("Отмена", null).show();
  }

  private void deleteSample(int index) {
    if (pads[index].filePath != null) {
      pads[index].filePath = null;
      savePads();
      if (audioPlayer.getCurrentPadIndex() == index)
        audioPlayer.stopAll();
      refreshPadColors();
      Toast.makeText(this, "Сэмпл удалён", Toast.LENGTH_SHORT).show();
    }
  }

  private void recordSample(final int index) {
    LinearLayout layout = new LinearLayout(this);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setPadding(40, 40, 40, 40);

    final TextView tvTime = new TextView(this);
    tvTime.setText("00:00");
    tvTime.setTextSize(36);
    tvTime.setGravity(Gravity.CENTER);

    final Button btnRec = new Button(this);
    btnRec.setText("🎤 Записать");

    layout.addView(tvTime);
    layout.addView(btnRec);

    final AlertDialog dialog = new AlertDialog.Builder(this)
        .setTitle("Запись сэмпла на пэд " + pads[index].label).setView(layout)
        .setNegativeButton("Закрыть", null).create();

    final String path = new File(getFilesDir(), "sample_" + index + ".m4a").getAbsolutePath();

    btnRec.setOnClickListener(v -> {
      if (!audioRecorder.isRecording()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(
            Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
          requestAudioPermission();
          return;
        }

        if (audioRecorder.start(this, path, tvTime)) {
          btnRec.setText("⏹ Стоп");
        }
      } else {
        audioRecorder.stop();
        btnRec.setText("🎤 Записать");
        tvTime.setText("Готово ✓");
        pads[index].filePath = path;
        savePads();
        Toast.makeText(this, "Сэмпл сохранён", Toast.LENGTH_SHORT).show();
      }
    });

    dialog.show();
  }

  // ================= ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ =================

  private File getRecordDirectory() {
    File musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
    File appDir = new File(musicDir, "SimplePad");
    if (!appDir.exists()) {
      appDir.mkdirs();
    }
    return appDir;
  }

  private String createRecordFilePath() {
    File dir = getRecordDirectory();
    if (!dir.exists())
      return null;

    String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
        .format(new Date());
    String fileName = "REC_" + timeStamp + ".m4a";
    return new File(dir, fileName).getAbsolutePath();
  }

  private void showGridDialog() {
    new AlertDialog.Builder(this).setTitle("Размер сетки")
        .setSingleChoiceItems(GRID_NAMES, gridIndex, (dialog, which) -> {
          if (which != gridIndex) {
            gridIndex = which;
            applyGrid();
            Toast.makeText(this, "Сетка: " + GRID_NAMES[which], Toast.LENGTH_SHORT).show();
          }
          dialog.dismiss();
        }).setNegativeButton("Отмена", null).show();
  }

  private void savePads() {
    SharedPreferences.Editor ed = prefs.edit();
    for (int i = 0; i < MAX_PADS; i++) {
      ed.putString("pad_" + i + "_label", pads[i].label);
      ed.putString("pad_" + i + "_path", pads[i].filePath);
    }
    ed.apply();
  }

  private void loadPads() {
    for (int i = 0; i < MAX_PADS; i++) {
      String label = prefs.getString("pad_" + i + "_label", String.valueOf(i + 1));
      String path = prefs.getString("pad_" + i + "_path", null);
      pads[i] = new Pad(label, path);
    }
  }

  private void requestAudioPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      if (checkSelfPermission(
          Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
        if (!shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
          showSettingsDialog();
        } else {
          requestPermissions(new String[] { Manifest.permission.RECORD_AUDIO }, REQ_AUDIO);
        }
      }
    }
  }

  private void showSettingsDialog() {
    new AlertDialog.Builder(this).setTitle("Нужно разрешение")
        .setMessage("Для записи нужен доступ к микрофону. Пожалуйста, разрешите его в настройках.")
        .setPositiveButton("Настройки", (dialog, which) -> {
          Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
          intent.setData(Uri.parse("package:" + getPackageName()));
          startActivity(intent);
        }).setNegativeButton("Отмена", null).show();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions,
      int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == REQ_AUDIO) {
      if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        Toast.makeText(this, "Микрофон разрешён", Toast.LENGTH_SHORT).show();
      } else {
        Toast.makeText(this, "Без разрешения запись не работает", Toast.LENGTH_LONG).show();
      }
    }
  }
}