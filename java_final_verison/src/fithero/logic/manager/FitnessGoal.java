package fithero.logic.manager;

/**
 * 健身增肌/減脂目標列舉
 */
public enum FitnessGoal {
    MUSCLE_GAIN("活躍增肌 (調配超額熱量)"),
    FAT_LOSS("科學減脂 (調配熱量赤字)");

    private final String displayName;
    FitnessGoal(String displayName) { this.displayName = displayName; }
    public String displayName() { return displayName; }
    @Override public String toString() { return displayName; }
}