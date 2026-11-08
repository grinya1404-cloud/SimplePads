package com.grinya.simplepad;

public class Pad {
    public String label;
    public String filePath;
    public float volume; // 0.0f ... 1.0f

    public Pad(String label, String filePath) {
        this.label = label;
        this.filePath = filePath;
        this.volume = 1.0f; // По умолчанию 100%
    }
}