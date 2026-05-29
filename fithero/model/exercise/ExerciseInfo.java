package com.fithero.model.exercise;

public class ExerciseInfo {
    private String name;
    private boolean isAerobic; // true: 有氧, false: 重訓
    private double metValue;   // 有氧專用 (METs)
    private String targetMuscle; // 刺激的主肌肉群 ("Chest", "Legs", "Back", "Core", "Arms", "FullBody")
    private boolean isBodyweight; // 重訓專用 (是否為自重訓練)
    private double difficultyCoefficient; // 重訓動作難度係數

    // 有氧運動建構子
    public ExerciseInfo(String name, double metValue, String targetMuscle) {
        this.name = name;
        this.isAerobic = true;
        this.metValue = metValue;
        this.targetMuscle = targetMuscle;
        this.isBodyweight = false;
        this.difficultyCoefficient = 1.0;
    }

    // 重訓運動建構子
    public ExerciseInfo(String name, String targetMuscle, boolean isBodyweight, double difficultyCoefficient) {
        this.name = name;
        this.isAerobic = false;
        this.metValue = 0.0;
        this.targetMuscle = targetMuscle;
        this.isBodyweight = isBodyweight;
        this.difficultyCoefficient = difficultyCoefficient;
    }

    // Getters
    public String getName() { return name; }
    public boolean isAerobic() { return isAerobic; }
    public double getMetValue() { return metValue; }
    public String getTargetMuscle() { return targetMuscle; }
    public boolean isBodyweight() { return isBodyweight; }
    public double getDifficultyCoefficient() { return difficultyCoefficient; }
}