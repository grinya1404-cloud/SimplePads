package com.example.simplepad;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TopBarView extends LinearLayout {

    public TextView tvRecordIndicator;
    public TextView tvRecordTimer;
    public Button btnRecStart, btnRecPause, btnRecStop;

    public TopBarView(Context context) {
        super(context);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL | Gravity.END); // Прижимаем всё вправо
        setPadding(16, 16, 16, 8);
        setBackgroundColor(Color.parseColor("#2C2C2C"));

        // 1. Красная точка (скрыта по умолчанию)
        tvRecordIndicator = new TextView(context);
        tvRecordIndicator.setText("●");
        tvRecordIndicator.setTextSize(24);
        tvRecordIndicator.setTextColor(Color.RED);
        tvRecordIndicator.setPadding(0, 0, 8, 0);
        tvRecordIndicator.setVisibility(GONE);

        // 2. Таймер (Всегда на экране, чтобы кнопки не прыгали)
        tvRecordTimer = new TextView(context);
        tvRecordTimer.setText("00:00");
        tvRecordTimer.setTextSize(18);
        tvRecordTimer.setTextColor(Color.WHITE);
        tvRecordTimer.setPadding(0, 0, 16, 0);

        // 3. Кнопки
        btnRecStart = new Button(context);
        btnRecStart.setText("REC");
        btnRecStart.setTextColor(Color.WHITE);
        
        btnRecPause = new Button(context);
        btnRecPause.setText("⏸");
        btnRecPause.setEnabled(false);
        
        btnRecStop = new Button(context);
        btnRecStop.setText("⏹");
        btnRecStop.setEnabled(false);

        LayoutParams btnParams = new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
        btnParams.setMargins(8, 0, 8, 0);

        addView(tvRecordIndicator);
        addView(tvRecordTimer);
        addView(btnRecStart, btnParams);
        addView(btnRecPause, btnParams);
        addView(btnRecStop, btnParams);
    }
}