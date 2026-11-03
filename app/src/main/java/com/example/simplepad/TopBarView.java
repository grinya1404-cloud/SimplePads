package com.example.simplepad;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class TopBarView extends LinearLayout {

  // Кнопки и индикатор остаются публичными, так как MainActivity их использует
  public TextView tvRecordIndicator;
  public TextView tvRecordTimer;
  public ProgressBar voxLevelBar;
  public Button btnRecStart, btnRecPause, btnRecStop;

  // Надписи делаем приватными — управляем ими через методы
  private TextView tvVoxAutoLabel;
  private TextView tvVoxPadLabel;

  public TopBarView(Context context) {
    super(context);
    setOrientation(HORIZONTAL);
    setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
    setPadding(16, 16, 16, 8);
    setBackgroundColor(Color.parseColor("#2C2C2C"));

    tvRecordIndicator = new TextView(context);
    tvRecordIndicator.setText("●");
    tvRecordIndicator.setTextSize(24);
    tvRecordIndicator.setTextColor(Color.RED);
    tvRecordIndicator.setPadding(0, 0, 8, 0);
    tvRecordIndicator.setVisibility(INVISIBLE);

    tvRecordTimer = new TextView(context);
    tvRecordTimer.setText("--:--"); // или "--:--" без пробелов
    tvRecordTimer.setTextSize(16);
    tvRecordTimer.setTextColor(Color.WHITE);
    tvRecordTimer.setPadding(0, 0, 16, 0);
    tvRecordTimer.setTypeface(android.graphics.Typeface.MONOSPACE);

    // --- Контейнер для Vox-индикатора и надписей ---
    LinearLayout voxContainer = new LinearLayout(context);
    voxContainer.setOrientation(LinearLayout.VERTICAL);
    voxContainer.setGravity(Gravity.CENTER);
    LayoutParams voxContainerParams = new LayoutParams(120, LayoutParams.WRAP_CONTENT);
    voxContainerParams.setMargins(8, 0, 8, 0);

    tvVoxAutoLabel = new TextView(context);
    tvVoxAutoLabel.setText("auto");
    tvVoxAutoLabel.setTextSize(10);
    tvVoxAutoLabel.setTextColor(Color.GRAY);
    tvVoxAutoLabel.setGravity(Gravity.CENTER);
    voxContainer.addView(tvVoxAutoLabel);

    voxLevelBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
    voxLevelBar.setMax(32767);
    voxLevelBar.setProgress(0);
    voxLevelBar.setPadding(0, 0, 0, 0);
    voxLevelBar.setVisibility(VISIBLE);
    LayoutParams voxParams = new LayoutParams(LayoutParams.MATCH_PARENT, 20);
    voxLevelBar.setLayoutParams(voxParams);
    voxContainer.addView(voxLevelBar);

    tvVoxPadLabel = new TextView(context);
    tvVoxPadLabel.setText("pad");
    tvVoxPadLabel.setTextSize(10);
    tvVoxPadLabel.setTextColor(Color.GRAY);
    tvVoxPadLabel.setGravity(Gravity.CENTER);
    voxContainer.addView(tvVoxPadLabel);

    addView(tvRecordIndicator);
    addView(tvRecordTimer);
    addView(voxContainer, voxContainerParams);

    // Кнопки...записи
    btnRecStart = new Button(context);
    btnRecStart.setText("REC");
    btnRecStart.setTextColor(Color.WHITE);

    btnRecPause = new Button(context);
    btnRecPause.setText("⏸");
    btnRecPause.setEnabled(false);

    btnRecStop = new Button(context);
    btnRecStop.setText("⏹");
    btnRecStop.setEnabled(false);

    // Пропорции: REC = 40%, Pause = 30%, Stop = 30%
    // Веса: 4f, 3f, 3f (сумма 10f)
    LayoutParams recParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 4f);
    recParams.setMargins(8, 0, 8, 0);

    LayoutParams pauseParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 3f);
    pauseParams.setMargins(8, 0, 8, 0);

    LayoutParams stopParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 3f);
    stopParams.setMargins(8, 0, 8, 0);

    addView(btnRecStart, recParams);
    addView(btnRecPause, pauseParams);
    addView(btnRecStop, stopParams);
  }

  // --- Инкапсулированные методы для управления надписями ---

  public void setAutoLabelActive(boolean active) {
    int color = active ? Color.RED : Color.GRAY;
    if (tvVoxAutoLabel.getCurrentTextColor() != color) {
      tvVoxAutoLabel.setTextColor(color);
    }
  }

  public void setPadLabelActive(boolean active) {
    int color = active ? Color.RED : Color.GRAY;
    if (tvVoxPadLabel.getCurrentTextColor() != color) {
      tvVoxPadLabel.setTextColor(color);
    }
  }

  // Показать/скрыть индикатор Vox (для внутреннего использования)
  public void setVoxLevelVisible(boolean visible) {
    voxLevelBar.setVisibility(visible ? VISIBLE : INVISIBLE);
    if (!visible)
      voxLevelBar.setProgress(0);
  }
}