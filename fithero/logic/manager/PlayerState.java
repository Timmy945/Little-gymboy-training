package fithero.logic.manager;

import java.awt.Window;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import javax.swing.SwingUtilities;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.BorderFactory;
import javax.swing.SwingConstants;

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
    private double bodyFatPercent = 0.0;
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
    public double getBodyFatPercent() { return bodyFatPercent; }
    public void setBodyFatPercent(double bodyFatPercent) { this.bodyFatPercent = bodyFatPercent; }
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

    /**
     * BMR 雙引擎自動分流演算法
     */
    public double calculateBMR() {
        double w = avatar.getProfile().getWeight();
        double h = avatar.getProfile().getHeight();
        Gender g = avatar.getProfile().getGender();

        if (bodyFatPercent > 0.0) {
            // 引擎 A：Katch-McArdle 公式 (精準肌肉代謝模型)
            double ffm = w * (1.0 - (bodyFatPercent / 100.0)); // 計算除脂體重
            return 370.0 + (21.6 * ffm);
        } else {
            // 引擎 B：Mifflin-St. Jeor 公式 (大眾體型保底模型)
            if (g == Gender.MALE) {
                return (10 * w) + (6.25 * h) - (5 * age) + 5;
            } else {
                return (10 * w) + (6.25 * h) - (5 * age) - 161;
            }
        }
    }

    /**
     * 動態智慧行事曆活動量 TDEE 演算法 (滾動 7 天比對)
     */
    public double calculateTDEE(List<WorkoutEntry> fullHistory) {
        double bmr = calculateBMR();
        double activityMultiplier = 1.2; 
        
        if (fullHistory == null || fullHistory.isEmpty()) {
            return bmr * activityMultiplier;
        }

        LocalDate sevenDaysAgo = LocalDate.now().minusDays(7);
        double currentMuscle = estimateMuscleMass(); // 提取肌肉量
        fithero.model.player.Gender gender = avatar.getProfile().getGender(); // 提取性別

        for (WorkoutEntry entry : fullHistory) {
            LocalDate workoutDate = entry.time().toLocalDate();
            if (workoutDate.isAfter(sevenDaysAgo) || workoutDate.isEqual(sevenDaysAgo)) {
                ExerciseInfo info = ExerciseRegistry.getExercise(entry.getExerciseName());
                if (info != null) {
                    if (info.isAerobic()) {
                        activityMultiplier += 0.05; 
                    } else {
                        // 餵入 AII 強度所需之完整 6 大參數
                        int intensity = ExpCalculator.calculateResistanceIntensity(
                                info, entry.weight(), avatar.getProfile().getWeight(), gender, this.age, currentMuscle);
                        activityMultiplier += (intensity * 0.015); 
                    }
                }
            }
        }

        if (activityMultiplier > 1.95) activityMultiplier = 1.95;
        return bmr * activityMultiplier;
    }

    /**
     * 精密度估算全身真實肌肉量 (Muscle Mass) 幾何模型
     */
    public double estimateMuscleMass() {
        double w = avatar.getProfile().getWeight();
        Gender g = avatar.getProfile().getGender();
        
        // 1. 取得體脂率 (若無則依醫學 WHO 算式逆向反推)
        double fatPercent = this.bodyFatPercent;
        if (fatPercent <= 0.0) {
            // 若玩家沒填體脂率，採用醫學 WHO 體型算式由 BMI 與年齡進行二級逆向反推
            double bmi = calculateBMI();
            int genderCode = (g == Gender.MALE) ? 1 : 0;
            fatPercent = (1.20 * bmi) + (0.23 * age) - (10.8 * genderCode) - 5.4;
            if (fatPercent < 3.0) fatPercent = 3.0; // 生理脫水極限防護線
        }

        // 2. 計算精準的「除脂體重 (Fat-Free Mass, FFM)」
        double ffm = w * (1.0 - (fatPercent / 100.0));

        // 3. 採用 Jannsen 骨骼肌質量幾何解析公式
        // 考量到身高 (H) 與去脂體重對純肌肉的影響
        double h = avatar.getProfile().getHeight();
        double genderBonus = (g == Gender.MALE) ? 1.0 : 0.0;
        
        // Jannsen 核心算式：(FFM * 0.56) + (身高縮放權重) + 性別加權
        // 這能完美剔除體內骨骼（約占體重 15%）與內臟器官（約占體重 10%）的重量！
        double pureSkeletalMuscle = (ffm * 0.53) + (h * 0.02) + (1.2 * genderBonus);
        
        // 保底防護線
        return Math.max(5.0, pureSkeletalMuscle);
    }

    public double calculateRecommendedCalories(List<WorkoutEntry> fullHistory) {
        double tdee = calculateTDEE(fullHistory);
        return fitnessGoal == FitnessGoal.FAT_LOSS ? (tdee - 400.0) : (tdee + 300.0);
    }

    public int muscleLevel(MuscleGroup muscle) {
        int muscleValue = avatar.getMuscleParts().getOrDefault(muscle, 0);
        if (muscleValue <= 0) return 1;
        
        // 將阻尼加權優化為 0.7，大幅提升新手開局的第一眼體感
        int calculatedLevel = (int) Math.sqrt(muscleValue * 0.7) + 1;
        
        return Math.max(1, calculatedLevel); 
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
        double currentMuscleMass = estimateMuscleMass();
        double calories = ExpCalculator.calculateResistanceCalories(info, weightLifted, reps, sets, userWeight);
        int intensity = ExpCalculator.calculateResistanceIntensity(info, weightLifted, userWeight, gender, this.age, currentMuscleMass);
        double earnedExp = ExpCalculator.calculateResistanceExp(calories, intensity);

        // 【邏輯補齊】檢查並套用三週連擊 1.25 倍加成獎勵
        if (isStreakBonusActive(fullHistory, planProps)) {
            earnedExp *= 1.25;
        }

        // 【型別安全重構】直接帶入強型別 Enum 執行成長
        MuscleGroup target = info.getTargetMuscle();
        int muscleGain = (int) Math.round(intensity * sets * 0.6);
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

    public int checkLevelUp() {
        int levelsGained = 0;
        while (avatar.getCurrentExp() >= avatar.getMaxExp()) {
            avatar.setCurrentExp(avatar.getCurrentExp() - avatar.getMaxExp());
            avatar.setLevel(avatar.getLevel() + 1);
            avatar.setMaxExp(avatar.getMaxExp() * 1.2);
            levelsGained++;
        }
        return levelsGained; // 回傳這次升了幾級（例如連升 3 級就回傳 3）
    }

    public List<Achievement> triggerAchievementCheck() {
        return this.achievementManager.checkAndUnlock(avatar);
    }
}