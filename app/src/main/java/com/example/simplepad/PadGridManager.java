package com.example.simplepad;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class PadGridManager {

    public static class PadView {
        public FrameLayout container;
        public ProgressBar progress;
        public TextView label;
        public TextView durationText;
    }

    public static final int COLOR_IDLE = 0xFF3A7BD5;
    public static final int COLOR_ACTIVE = 0xFF1FA84B;
    public static final int COLOR_PAUSED = 0xFFE8A33D;
    public static final int COLOR_PROGRESS = 0xAAFFFFFF;

    // Создает один пэд и добавляет его в GridLayout
    public static PadView createPadView(Context context, GridLayout grid, int index, int cols, String label) {
        FrameLayout container = new FrameLayout(context);
        container.setBackgroundColor(COLOR_IDLE);
        container.setClickable(true);
        container.setFocusable(true);

        // Прогресс-бар
        ProgressBar progress = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(1000);
        progress.setProgress(0);
        progress.setProgressTintList(ColorStateList.valueOf(COLOR_PROGRESS));
        progress.setPadding(0, 0, 0, 0);
        progress.setScaleY(65f);
        FrameLayout.LayoutParams progParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        progress.setLayoutParams(progParams);
        container.addView(progress);

        // Название
        TextView labelView = new TextView(context);
        labelView.setText(label);
        labelView.setTextSize(20);
        labelView.setTextColor(Color.WHITE);
        labelView.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams labelParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        labelParams.bottomMargin = 40; 
        labelView.setLayoutParams(labelParams);
        container.addView(labelView);

        // Длительность
        TextView durationView = new TextView(context);
        durationView.setTextSize(12);
        durationView.setTextColor(Color.parseColor("#CCCCCC"));
        durationView.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        durationView.setPadding(0, 0, 0, 8);
        FrameLayout.LayoutParams durParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        durationView.setLayoutParams(durParams);
        container.addView(durationView);

        // Параметры для сетки
        GridLayout.LayoutParams p = new GridLayout.LayoutParams();
        p.width = 0;
        p.height = 0;
        p.columnSpec = GridLayout.spec(index % cols, 1f);
        p.rowSpec = GridLayout.spec(index / cols, 1f);
        p.setMargins(6, 6, 6, 6);
        container.setLayoutParams(p);

        grid.addView(container);

        PadView pv = new PadView();
        pv.container = container;
        pv.progress = progress;
        pv.label = labelView;
        pv.durationText = durationView;
        return pv;
    }
}