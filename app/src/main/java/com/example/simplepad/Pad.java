package com.example.simplepad;

public class Pad {
    public String label;
    public String filePath;
    public float volume;
    public int startOffsetMs; // <--- Вот оно

    public Pad(String label, String filePath) {
        this.label = label;
        this.filePath = filePath;
        this.volume = 1.0f;
        this.startOffsetMs = 0;
    }
}