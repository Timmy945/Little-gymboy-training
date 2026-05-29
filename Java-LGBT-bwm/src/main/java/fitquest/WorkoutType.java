package fitquest;

public enum WorkoutType {
    PUSH_UP("伏地挺身", MuscleGroup.CHEST, 5),
    SQUAT("深蹲", MuscleGroup.LEGS, 5),
    SIT_UP("仰臥起坐", MuscleGroup.ABS, 4),
    CURL("啞鈴彎舉", MuscleGroup.ARMS, 6),
    ROW("划船訓練", MuscleGroup.BACK, 6),
    RUNNING("跑步分鐘", MuscleGroup.LEGS, 8);

    private final String displayName;
    private final MuscleGroup mainMuscle;
    private final int xpPerUnit;

    WorkoutType(String displayName, MuscleGroup mainMuscle, int xpPerUnit) {
        this.displayName = displayName;
        this.mainMuscle = mainMuscle;
        this.xpPerUnit = xpPerUnit;
    }

    public String displayName() {
        return displayName;
    }

    public MuscleGroup mainMuscle() {
        return mainMuscle;
    }

    public int calculateXp(int amount) {
        return Math.max(0, amount) * xpPerUnit;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
