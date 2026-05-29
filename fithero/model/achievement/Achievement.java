package com.fithero.model.achievement;

public class Achievement {
    private String id;
    private String title;
    private String description;
    private boolean isUnlocked;

    public Achievement(String id, String title, String description) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.isUnlocked = false;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public boolean isUnlocked() { return isUnlocked; }
    public void setUnlocked(boolean unlocked) { isUnlocked = unlocked; }
}