package com.fithero.model.player;

import java.util.HashMap;
import java.util.Map;

public class Avatar {
    private String name;
    private int level;
    private double currentExp;
    private double maxExp;
    private Map<String, Integer> muscleParts;
    private UserProfile profile; // 綁定使用者體態資料

    public Avatar(String name, double height, double weight, Gender gender) {
        this.name = name;
        this.level = 1;
        this.currentExp = 0.0;
        this.maxExp = 100.0;
        this.profile = new UserProfile(height, weight, gender);
        
        this.muscleParts = new HashMap<>();
        this.muscleParts.put("Chest", 0);
        this.muscleParts.put("Back", 0);
        this.muscleParts.put("Core", 0);
        this.muscleParts.put("Arms", 0);
        this.muscleParts.put("Legs", 0);
    }

    public String getName() { return name; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public double getCurrentExp() { return currentExp; }
    public void setCurrentExp(double currentExp) { this.currentExp = currentExp; }
    public double getMaxExp() { return maxExp; }
    public void setMaxExp(double maxExp) { this.maxExp = maxExp; }
    public Map<String, Integer> getMuscleParts() { return muscleParts; }
    public UserProfile getProfile() { return profile; }

    public void trainMuscle(String part, int amount) {
        if (muscleParts.containsKey(part)) {
            int currentVal = muscleParts.get(part);
            muscleParts.put(part, currentVal + amount);
        }
    }
}