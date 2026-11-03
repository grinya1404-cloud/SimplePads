package com.example.simplepad;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class TopBarView extends LinearLayout {

  public TextView tvRecordTimer;
  public ProgressBar voxLevelBar;
  public Button btnRecStart, btnRecPause, btnRecStop;
  public TextView tvHeadsetIcon;
  public View recDotView; // Красный кружок поверх кнопки

  private TextView tvVoxAutoLabel;
  private TextView tvVoxPadLabel;

  public TopBarView(Context context) {
    super(context);
    setOrientation(HORIZONTAL);
    setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
    setPadding(16, 16, 16, 8);
    setBackgroundColor(Color.parseColor("#2C2C2C"));

    // --- Надпись HS (слева) ---
    tvHeadsetIcon = new TextView(context);
    tvHeadsetIcon.setText("HS");
    tvHeadsetIcon.setTextSize(10);
    tvHeadsetIcon.setTextColor(Color.GRAY);
    tvHeadsetIcon.setGravity(Gravity.CENTER);
    LayoutParams hsParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    hsParams.setMargins(0, 0, 8, 0);
    addView(tvHeadsetIcon, hsParams);

    // --- Таймер ---
    tvRecordTimer = new TextView(context);
    tvRecordTimer.setText("--:--");
    tvRecordTimer.setTextSize(16);
    tvRecordTimer.setTextColor(Color.WHITE);
    tvRecordTimer.setPadding(0, 0, 16, 0);
    tvRecordTimer.setTypeface(android.graphics.Typeface.MONOSPACE);
    addView(tvRecordTimer);

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

    addView(voxContainer, voxContainerParams);

    // --- Кнопки записи ---
    // Высота кнопок (например, 56dp)
    int buttonHeight = (int) android.util.TypedValue.applyDimension(
            android.util.TypedValue.COMPLEX_UNIT_DIP, 56, getResources().getDisplayMetrics());

    // Контейнер для кнопки REC и кружка поверх неё
    FrameLayout recContainer = new FrameLayout(context);
    LayoutParams recContainerParams = new LayoutParams(0, buttonHeight, 4f);
    recContainerParams.setMargins(8, 0, 8, 0);

    btnRecStart = new Button(context);
    btnRecStart.setText("REC");
    btnRecStart.setTextColor(Color.WHITE);
    btnRecStart.setSingleLine(true);
    btnRecStart.setGravity(Gravity.CENTER);
    btnRecStart.setPadding(0, 0, 0, 0);
    FrameLayout.LayoutParams btnRecParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
    btnRecStart.setLayoutParams(btnRecParams);
    recContainer.addView(btnRecStart);

    // Красный кружок (слой поверх кнопки)
    recDotView = new View(context);
    GradientDrawable dotBg = new GradientDrawable();
    dotBg.setShape(GradientDrawable.OVAL);
    dotBg.setColor(Color.RED);
    recDotView.setBackground(dotBg);

    int dotSize = (int) android.util.TypedValue.applyDimension(
            android.util.TypedValue.COMPLEX_UNIT_DIP, 12, getResources().getDisplayMetrics());
    FrameLayout.LayoutParams dotParams = new FrameLayout.LayoutParams(dotSize, dotSize);
    dotParams.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
    dotParams.leftMargin = 16; // Отступ слева
    recDotView.setLayoutParams(dotParams);
    recDotView.setVisibility(View.INVISIBLE); // Скрыт по умолчанию
    recContainer.addView(recDotView);

    addView(recContainer, recContainerParams);

    // Кнопка Pause
    btnRecPause = new Button(context);
    btnRecPause.setText("⏸");
    btnRecPause.setEnabled(false);
    btnRecPause.setSingleLine(true);
    btnRecPause.setGravity(Gravity.CENTER);
    btnRecPause.setPadding(0, 0, 0, 0);
    LayoutParams pauseParams = new LayoutParams(0, buttonHeight, 3f);
    pauseParams.setMargins(8, 0, 8, 0);
    addView(btnRecPause, pauseParams);

    // Кнопка Stop
    btnRecStop = new Button(context);
    btnRecStop.setText("⏹");
    btnRecStop.setEnabled(false);
    btnRecStop.setSingleLine(true);
    btnRecStop.setGravity(Gravity.CENTER);
    btnRecStop.setPadding(0, 0, 0, 0);
    LayoutParams stopParams = new LayoutParams(0, buttonHeight, 3f);
    stopParams.setMargins(8, 0, 8, 0);
    addView(btnRecStop, stopParams);
  }

  // Управление кружком (вместо изменения текста)
  public void setRecButtonText(boolean isRecording) {
    if (recDotView != null) {
      recDotView.setVisibility(isRecording ? View.VISIBLE : View.INVISIBLE);
    }
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

  public void setVoxLevelVisible(boolean visible) {
    voxLevelBar.setVisibility(visible ? VISIBLE : INVISIBLE);
    if (!visible)
      voxLevelBar.setProgress(0);
  }
}