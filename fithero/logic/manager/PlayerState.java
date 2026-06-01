package fithero.logic.manager;

import fithero.model.player.Avatar;
import fithero.model.player.Gender;
import fithero.model.exercise.MuscleGroup;
import fithero.model.exercise.ExerciseInfo;
import fithero.model.exercise.ExerciseRegistry;
import fithero.model.workout.WorkoutEntry;
import fithero.model.achievement.Achievement;
import fithero.logic.calculator.ExpCalculator;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;

/**
 * 核心遊戲狀態管理器（中央指揮調度大腦）
 */
public class PlayerState {
    private final Avatar avatar;
    private final AchievementManager achievementManager;
    
    private double targetWeight;
    private int age = 25; 
    private FitnessGoal fitnessGoal = FitnessGoal.FAT_LOSS; 

    public PlayerState(String name, double height, double weight, Gender gender) {
        this.avatar = new Avatar(name, height, weight, gender);
        this.achievementManager = new AchievementManager();
        this.targetWeight = weight;
    }

    public Avatar getAvatar() { return avatar; }
    public AchievementManager getAchievementManager() { return this.achievementManager; }
    public int level() { return avatar.getLevel(); }
    public int xp() { return (int) avatar.getCurrentExp(); }
    public int xpNeededForNextLevel() { return (int) avatar.getMaxExp(); }

    public double getTargetWeight() { return targetWeight; }
    public void setTargetWeight(double targetWeight) { this.targetWeight = targetWeight; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public FitnessGoal getFitnessGoal() { return fitnessGoal; }
    public void setFitnessGoal(FitnessGoal fitnessGoal) { this.fitnessGoal = fitnessGoal; }

    public String getWeightProgressString() {
        double currentWeight = avatar.getProfile().getWeight();
        double diff = currentWeight - targetWeight;
        if (diff == 0) return "已達成目標體重！";
        return diff > 0 ? String.format("距離目標體重還差：-%.1f kg", diff) 
                        : String.format("距離目標體重還差：+%.1f kg", Math.abs(diff));
    }

    public double calculateBMI() {
        double w = avatar.getProfile().getWeight();
        double hMeters = avatar.getProfile().getHeight() / 100.0;
        return w / (hMeters * hMeters);
    }

    public double calculateBMR() {
        double w = avatar.getProfile().getWeight();
        double h = avatar.getProfile().getHeight();
        Gender g = avatar.getProfile().getGender();
        return (g == Gender.MALE) ? (10 * w) + (6.25 * h) - (5 * age) + 5 
                                  : (10 * w) + (6.25 * h) - (5 * age) - 161;
    }

    public double calculateTDEE() {
        return calculateBMR() * 1.375;
    }

    public double calculateRecommendedCalories() {
        double tdee = calculateTDEE();
        return fitnessGoal == FitnessGoal.FAT_LOSS ? (tdee - 400.0) : (tdee + 300.0);
    }

    public int muscleLevel(MuscleGroup muscle) {
        int muscleValue = avatar.getMuscleParts().getOrDefault(muscle, 0);
        return Math.max(1, 1 + (muscleValue / 15)); 
    }

    public Map<MuscleGroup, Integer> muscleLevels() {
        EnumMap<MuscleGroup, Integer> map = new EnumMap<>(MuscleGroup.class);
        for (MuscleGroup m : MuscleGroup.values()) {
            map.put(m, muscleLevel(m));
        }
        return Map.copyOf(map);
    }

    public int submitAerobicWorkout(String exerciseName, double minutes, List<WorkoutEntry> fullHistory, java.util.Properties planProps) {
        if (!ExerciseRegistry.exists(exerciseName)) return 0;
        ExerciseInfo info = ExerciseRegistry.getExercise(exerciseName);
        double userWeight = avatar.getProfile().getWeight();
        
        double calories = ExpCalculator.calculateAerobicCalories(info, minutes, userWeight);
        double earnedExp = ExpCalculator.calculateAerobicExp(calories);

        // 【邏輯補齊】檢查並套用三週連擊 1.25 倍加成獎勵
        if (isStreakBonusActive(fullHistory, planProps)) {
            earnedExp *= 1.25;
        }

        avatar.setCurrentExp(avatar.getCurrentExp() + earnedExp);
        checkLevelUp();
        return (int) earnedExp; 
    }

    public int submitResistanceWorkout(String exerciseName, double weightLifted, int reps, int sets, List<WorkoutEntry> fullHistory, java.util.Properties planProps) {
        if (!ExerciseRegistry.exists(exerciseName)) return 0;
        ExerciseInfo info = ExerciseRegistry.getExercise(exerciseName);
        double userWeight = avatar.getProfile().getWeight();
        Gender gender = avatar.getProfile().getGender();

        double calories = ExpCalculator.calculateResistanceCalories(info, weightLifted, reps, sets, userWeight);
        int intensity = ExpCalculator.calculateResistanceIntensity(info, weightLifted, userWeight, gender);
        double earnedExp = ExpCalculator.calculateResistanceExp(calories, intensity);

        // 【邏輯補齊】檢查並套用三週連擊 1.25 倍加成獎勵
        if (isStreakBonusActive(fullHistory, planProps)) {
            earnedExp *= 1.25;
        }

        // 【型別安全重構】直接帶入強型別 Enum 執行成長
        MuscleGroup target = info.getTargetMuscle();
        int muscleGain = intensity * 2; 
        avatar.trainMuscle(target, muscleGain);

        avatar.setCurrentExp(avatar.getCurrentExp() + earnedExp);
        checkLevelUp();
        return (int) earnedExp;
    }

    public void applyLazyPenalty(double penaltyPercent) {
        double maxExp = avatar.getMaxExp();
        double penaltyAmount = maxExp * (penaltyPercent / 100.0);
        double newExp = avatar.getCurrentExp() - penaltyAmount;
        if (newExp < 0) newExp = 0.0; 
        avatar.setCurrentExp(newExp);
    }

    public boolean isStreakBonusActive(List<WorkoutEntry> workouts, java.util.Properties planProps) {
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 21; i++) {
            LocalDate checkDate = today.minusDays(i);
            if ("lazy".equals(planProps.getProperty("plan." + checkDate + ".status"))) return false; 
        }
        for (int w = 0; w < 3; w++) {
            LocalDate startOfWeek = today.minusDays((w + 1) * 7 - 1);
            LocalDate endOfWeek = today.minusDays(w * 7);
            boolean hasWorkoutInWeek = workouts.stream().anyMatch(entry -> {
                LocalDate d = entry.time().toLocalDate();
                return (d.isAfter(startOfWeek) || d.isEqual(startOfWeek)) && (d.isBefore(endOfWeek) || d.isEqual(endOfWeek));
            });
            if (!hasWorkoutInWeek) return false; 
        }
        return true;
    }

    private void checkLevelUp() {
        while (avatar.getCurrentExp() >= avatar.getMaxExp()) {
            avatar.setCurrentExp(avatar.getCurrentExp() - avatar.getMaxExp());
            avatar.setLevel(avatar.getLevel() + 1);
            avatar.setMaxExp(avatar.getMaxExp() * 1.2);
        }
    }

    public List<Achievement> triggerAchievementCheck() {
        return this.achievementManager.checkAndUnlock(avatar);
    }
}