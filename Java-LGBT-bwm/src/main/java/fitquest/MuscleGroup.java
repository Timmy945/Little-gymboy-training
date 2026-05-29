package fitquest;

import java.awt.Color;

public enum MuscleGroup {
    CHEST("胸肌", new Color(220, 70, 70)),
    ARMS("手臂", new Color(240, 150, 45)),
    ABS("腹肌", new Color(70, 145, 230)),
    LEGS("腿部", new Color(70, 170, 110)),
    BACK("背部", new Color(145, 95, 210));

    private final String displayName;
    private final Color color;

    MuscleGroup(String displayName, Color color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String displayName() {
        return displayName;
    }

    public Color color() {
        return color;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
