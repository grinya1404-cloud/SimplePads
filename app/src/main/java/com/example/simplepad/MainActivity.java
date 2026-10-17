package com.example.simplepad; 

import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
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

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    prefs = getSharedPreferences("simplepad", Context.MODE_PRIVATE);
    audioPlayer = new AudioPlayer();
    audioRecorder = new AudioRecorder();

    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(Color.parseColor("#1E1E1E"));

    grid = new GridLayout(this);
    LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
    grid.setLayoutParams(gridParams);
    grid.setPadding(8, 8, 8, 8);

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

    root.addView(grid);
    root.addView(bottomBar);
    setContentView(root);

    loadPads();
    gridIndex = prefs.getInt("gridIndex", 1);
    applyGrid();
    // requestAudioPermission();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    audioPlayer.stopAll();
    audioRecorder.stop();
  }

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

  private void showPadMenu(final int index) {
    final String[] items = { "✏️ Переименовать", "🎤 Записать сэмпл", "🗑 Удалить сэмпл",
        "❌ Отмена" };
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
              deleteSample(index);
              break;
          }
        }).show();
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
      new File(pads[index].filePath).delete();
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
        // Проверяем разрешение перед стартом
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
        // Если пользователь уже отказывал, показываем диалог с переходом в настройки
        if (!shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
          showSettingsDialog();
        } else {
          requestPermissions(new String[] { Manifest.permission.RECORD_AUDIO }, REQ_AUDIO);
        }
      }
    }
  }

  private void showSettingsDialog() {
    new AlertDialog.Builder(this).setTitle("Нужно разрешение").setMessage(
        "Для записи сэмплов нужен доступ к микрофону. Пожалуйста, разрешите его в настройках приложения.")
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
        Toast.makeText(this, "Без разрешения на микрофон запись не работает", Toast.LENGTH_LONG)
            .show();
        // Если отказ был окончательным, предложим перейти в настройки
        if (!shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
          showSettingsDialog();
        }
      }
    }
  }
}