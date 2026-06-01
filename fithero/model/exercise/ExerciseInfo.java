package fithero.model.exercise;

/**
 * 單一運動項目的科學屬性模型
 */
public class ExerciseInfo {
    private final String name;
    private final boolean isAerobic; 
    private final double metValue;   
    private final MuscleGroup targetMuscle; // 升級為強型別，杜絕字串拼錯 Bug
    private final boolean isBodyweight; 
    private final double difficultyCoefficient; 

    // 有氧運動建構子
    public ExerciseInfo(String name, double metValue, MuscleGroup targetMuscle) {
        this.name = name;
        this.isAerobic = true;
        this.metValue = metValue;
        this.targetMuscle = targetMuscle;
        this.isBodyweight = false;
        this.difficultyCoefficient = 1.0;
    }

    // 重訓運動建構子
    public ExerciseInfo(String name, MuscleGroup targetMuscle, boolean isBodyweight, double difficultyCoefficient) {
        this.name = name;
        this.isAerobic = false;
        this.metValue = 0.0;
        this.targetMuscle = targetMuscle;
        this.isBodyweight = isBodyweight;
        this.difficultyCoefficient = difficultyCoefficient;
    }

    public String getName() { return name; }
    public boolean isAerobic() { return isAerobic; }
    public double getMetValue() { return metValue; }
    public MuscleGroup getTargetMuscle() { return targetMuscle; }
    public boolean isBodyweight() { return isBodyweight; }
    public double getDifficultyCoefficient() { return difficultyCoefficient; }
}