package fitquest;

import java.util.EnumMap;
import java.util.Map;

public class PlayerState {
    private int level = 1;
    private int xp = 0;
    private int upgradePoints = 0;
    private final EnumMap<MuscleGroup, Integer> muscleLevels = new EnumMap<>(MuscleGroup.class);

    public PlayerState() {
        for (MuscleGroup muscle : MuscleGroup.values()) {
            muscleLevels.put(muscle, 1);
        }
    }

    public int level() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.max(1, level);
    }

    public int xp() {
        return xp;
    }

    public void setXp(int xp) {
        this.xp = Math.max(0, xp);
    }

    public int upgradePoints() {
        return upgradePoints;
    }

    public void setUpgradePoints(int upgradePoints) {
        this.upgradePoints = Math.max(0, upgradePoints);
    }

    public int muscleLevel(MuscleGroup muscle) {
        return muscleLevels.getOrDefault(muscle, 1);
    }

    public void setMuscleLevel(MuscleGroup muscle, int level) {
        muscleLevels.put(muscle, Math.max(1, level));
    }

    public Map<MuscleGroup, Integer> muscleLevels() {
        return Map.copyOf(muscleLevels);
    }

    public int xpNeededForNextLevel() {
        return 80 + level * 40;
    }

    public int addWorkout(WorkoutType type, int amount) {
        int gainedXp = type.calculateXp(amount);
        xp += gainedXp;
        while (xp >= xpNeededForNextLevel()) {
            xp -= xpNeededForNextLevel();
            level++;
            upgradePoints += 2;
        }
        return gainedXp;
    }

    public boolean upgrade(MuscleGroup muscle) {
        if (upgradePoints <= 0) {
            return false;
        }
        muscleLevels.put(muscle, muscleLevel(muscle) + 1);
        upgradePoints--;
        return true;
    }
}
