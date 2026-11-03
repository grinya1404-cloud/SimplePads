package com.example.simplepad;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class TopBarView extends LinearLayout {

    private final TextView tvRecordTimer;
    private final ProgressBar voxLevelBar;
    private final Button btnRecStart;
    private final Button btnRecPause;
    private final Button btnRecStop;
    private final TextView tvHeadsetIcon;
    private final View recDotView;

    private final TextView tvVoxAutoLabel;
    private final TextView tvVoxPadLabel;

    public TopBarView(Context context) {
        super(context);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        setPadding(16, 16, 16, 8);
        setBackgroundColor(Color.parseColor("#2C2C2C"));

        tvHeadsetIcon = createSmallText(context, "HS", Color.GRAY);
        LayoutParams hsParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        hsParams.setMargins(0, 0, 8, 0);
        addView(tvHeadsetIcon, hsParams);

        tvRecordTimer = createTimerText(context);
        addView(tvRecordTimer);

        LinearLayout voxContainer = new LinearLayout(context);
        voxContainer.setOrientation(VERTICAL);
        voxContainer.setGravity(Gravity.CENTER);
        LayoutParams voxContainerParams = new LayoutParams(120, LayoutParams.WRAP_CONTENT);
        voxContainerParams.setMargins(8, 0, 8, 0);

        tvVoxAutoLabel = createSmallText(context, "auto", Color.GRAY);
        voxContainer.addView(tvVoxAutoLabel);

        voxLevelBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        voxLevelBar.setMax(32767);
        voxLevelBar.setProgress(0);
        voxLevelBar.setPadding(0, 0, 0, 0);
        voxLevelBar.setVisibility(VISIBLE);
        LayoutParams voxParams = new LayoutParams(LayoutParams.MATCH_PARENT, 20);
        voxLevelBar.setLayoutParams(voxParams);
        voxContainer.addView(voxLevelBar);

        tvVoxPadLabel = createSmallText(context, "pad", Color.GRAY);
        voxContainer.addView(tvVoxPadLabel);

        addView(voxContainer, voxContainerParams);

        int buttonHeight = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 56, getResources().getDisplayMetrics());

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

        recDotView = new View(context);
        GradientDrawable dotBg = new GradientDrawable();
        dotBg.setShape(GradientDrawable.OVAL);
        dotBg.setColor(Color.RED);
        recDotView.setBackground(dotBg);

        int dotSize = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 12, getResources().getDisplayMetrics());
        FrameLayout.LayoutParams dotParams = new FrameLayout.LayoutParams(dotSize, dotSize);
        dotParams.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
        dotParams.leftMargin = 16;
        recDotView.setLayoutParams(dotParams);
        recDotView.setVisibility(View.INVISIBLE);
        recContainer.addView(recDotView);

        addView(recContainer, recContainerParams);

        btnRecPause = new Button(context);
        btnRecPause.setText("⏸");
        btnRecPause.setEnabled(false);
        btnRecPause.setSingleLine(true);
        btnRecPause.setGravity(Gravity.CENTER);
        btnRecPause.setPadding(0, 0, 0, 0);
        LayoutParams pauseParams = new LayoutParams(0, buttonHeight, 3f);
        pauseParams.setMargins(8, 0, 8, 0);
        addView(btnRecPause, pauseParams);

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

    private static TextView createSmallText(Context context, String text, int color) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextSize(10);
        tv.setTextColor(color);
        tv.setGravity(Gravity.CENTER);
        return tv;
    }

    private static TextView createTimerText(Context context) {
        TextView tv = new TextView(context);
        tv.setText("--:--");
        tv.setTextSize(16);
        tv.setTextColor(Color.WHITE);
        tv.setPadding(0, 0, 16, 0);
        tv.setTypeface(android.graphics.Typeface.MONOSPACE);
        return tv;
    }

    public TextView getRecordTimer() {
        return tvRecordTimer;
    }

    public ProgressBar getVoxLevelBar() {
        return voxLevelBar;
    }

    public Button getBtnRecStart() {
        return btnRecStart;
    }

    public Button getBtnRecPause() {
        return btnRecPause;
    }

    public Button getBtnRecStop() {
        return btnRecStop;
    }

    public TextView getTvHeadsetIcon() {
        return tvHeadsetIcon;
    }

    public void setRecordingDot(boolean visible) {
        recDotView.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    }

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
        if (!visible) {
            voxLevelBar.setProgress(0);
        }
    }
}
