package com.example.simplepad;

public class Pad {
    public String label;
    public String filePath; // null = сэмпл не записан

    public Pad(String label, String filePath) {
        this.label = label;
        this.filePath = filePath;
    }
}