package com.example.simplepad;

public class Pad {
    private String label;
    private String filePath; // null = сэмпл не записан

    public Pad(String label, String filePath) {
        this.label = label;
        this.filePath = filePath;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}