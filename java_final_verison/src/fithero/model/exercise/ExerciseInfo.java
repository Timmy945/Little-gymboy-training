package fithero.model.exercise;

/**
 * 單一運動項目的科學屬性模型
 */
public class ExerciseInfo {
    private String name;
    private double met; // 有氧運動專用 (MET 值)
    private MuscleGroup targetMuscle; // 重訓專用
    private boolean isBodyweight;    // 重訓專用
    private double difficultyMultiplier; // 重訓專用

    // 有氧運動建構子
    public ExerciseInfo(String name, double met) {
        this.name = name;
        this.met = met;
        this.targetMuscle = null;
        this.isBodyweight = false;
        this.difficultyMultiplier = 0.0;
    }

    // 重訓運動建構子
    public ExerciseInfo(String name, MuscleGroup targetMuscle, boolean isBodyweight, double difficultyMultiplier) {
        this.name = name;
        this.met = 0.0;
        this.targetMuscle = targetMuscle;
        this.isBodyweight = isBodyweight;
        this.difficultyMultiplier = difficultyMultiplier;
    }

    public boolean isAerobic() {
        return this.targetMuscle == null;
    }

    public String getName() { return name; }
    public double getMet() { return met; }
    public MuscleGroup getTargetMuscle() { return targetMuscle; }
    public boolean isBodyweight() { return isBodyweight; }
    public double getDifficultyMultiplier() { return difficultyMultiplier; }
}