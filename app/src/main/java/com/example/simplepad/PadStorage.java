package com.example.simplepad;

import android.content.Context;
import android.content.SharedPreferences;

public class PadStorage {
    private static final int MAX_PADS = 25;
    private SharedPreferences prefs;

    public PadStorage(Context context) {
        prefs = context.getSharedPreferences("simplepad", Context.MODE_PRIVATE);
    }

    public void savePads(Pad[] pads) {
        SharedPreferences.Editor ed = prefs.edit();
        for (int i = 0; i < MAX_PADS; i++) {
            ed.putString("pad_" + i + "_label", pads[i].label);
            ed.putString("pad_" + i + "_path", pads[i].filePath);
        }
        ed.apply();
    }

    public Pad[] loadPads() {
        Pad[] pads = new Pad[MAX_PADS];
        for (int i = 0; i < MAX_PADS; i++) {
            String label = prefs.getString("pad_" + i + "_label", String.valueOf(i + 1));
            String path = prefs.getString("pad_" + i + "_path", null);
            pads[i] = new Pad(label, path);
        }
        return pads;
    }

    public int getGridIndex() {
        return prefs.getInt("gridIndex", 1);
    }

    public void saveGridIndex(int index) {
        prefs.edit().putInt("gridIndex", index).apply();
    }
}