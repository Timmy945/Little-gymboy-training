package fithero.model.exercise;

/**
 * 核心動作媒介類型定義
 */
public enum WorkoutType {
    PUSH_UP("伏地挺身", MuscleGroup.CHEST),
    SQUAT("徒手深蹲", MuscleGroup.LEGS),
    SIT_UP("仰臥起坐", MuscleGroup.ABS),
    CURL("啞鈴二頭彎舉", MuscleGroup.ARMS),
    ROW("槓鈴划船", MuscleGroup.BACK),
    RUNNING("快跑 (高強度)", MuscleGroup.LEGS);

    private final String displayName;
    private final MuscleGroup defaultMuscle;

    WorkoutType(String displayName, MuscleGroup defaultMuscle) {
        this.displayName = displayName;
        this.defaultMuscle = defaultMuscle;
    }

    public String displayName() { return displayName; }

    public MuscleGroup mainMuscle() {
        ExerciseInfo info = ExerciseRegistry.getExercise(displayName);
        if (info == null) return defaultMuscle;
        return info.getTargetMuscle(); // 由於強型別升級，直接回傳 Enum 物件，再也不用寫繁雜的 switch
    }

    public static WorkoutType fromDisplayName(String name) {
        for (WorkoutType type : WorkoutType.values()) {
            if (type.displayName().equals(name)) return type;
        }
        
        ExerciseInfo info = ExerciseRegistry.getExercise(name);
        if (info != null) {
            if (info.isAerobic()) return RUNNING;
            return switch (info.getTargetMuscle()) {
                case CHEST -> PUSH_UP;
                case LEGS -> SQUAT;
                case BACK -> ROW;
                case ARMS -> CURL;
                default -> SIT_UP;
            };
        }
        return PUSH_UP; 
    }

    @Override
    public String toString() { return displayName; }
}