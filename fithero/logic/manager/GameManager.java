package com.fithero.logic.manager;

import com.fithero.model.player.Avatar;
import com.fithero.model.exercise.ExerciseInfo;
import com.fithero.model.exercise.ExerciseRegistry;
import com.fithero.logic.calculator.ExpCalculator;
import com.fithero.model.player.Gender;
import com.fithero.model.achievement.Achievement;

import java.util.List;

public class GameManager {
    private Avatar player;
    private AchievementManager achievementManager; // 新增成就管理器

    public GameManager(Avatar player) {
        this.player = player;
        this.achievementManager = new AchievementManager(); // 在此初始化
    }

    public void submitAerobicWorkout(String exerciseName, double minutes) {
        if (!ExerciseRegistry.exists(exerciseName)) {
            System.out.println("錯誤：找不到此運動項目！");
            return;
        }
        ExerciseInfo info = ExerciseRegistry.getExercise(exerciseName);
        double userWeight = player.getProfile().getWeight();

        // 1. 計算熱量
        double calories = ExpCalculator.calculateAerobicCalories(info, minutes, userWeight);
        
        // 2. 計算經驗值 (純熱量公式)
        double earnedExp = ExpCalculator.calculateAerobicExp(calories);

        System.out.println("\n===== 紀錄運動: " + info.getName() + " (非重訓類) =====");
        System.out.printf("儲存消耗熱量: %.1f 大卡\n", calories);
        System.out.printf("獲得經驗值(純熱量轉換): %.1f\n", earnedExp);
        System.out.println("提示：此項目不影響肌肉成長。");

        // 3. 套用獎勵與檢查升級
        player.setCurrentExp(player.getCurrentExp() + earnedExp);
        checkLevelUp();

        // 結算後掃描成就
        triggerAchievementCheck();
    }

    public void submitResistanceWorkout(String exerciseName, double weightLifted, int reps, int sets) {
        if (!ExerciseRegistry.exists(exerciseName)) {
            System.out.println("錯誤：找不到此運動項目！");
            return;
        }
        ExerciseInfo info = ExerciseRegistry.getExercise(exerciseName);
        double userWeight = player.getProfile().getWeight();
        Gender gender = player.getProfile().getGender();

        // 1. 計算熱量與 1~10 級細化強度
        double calories = ExpCalculator.calculateResistanceCalories(info, weightLifted, reps, sets, userWeight);
        int intensity = ExpCalculator.calculateResistanceIntensity(info, weightLifted, userWeight, gender);
        
        // 2. 計算經驗值 (熱量 + 強度公式)
        double earnedExp = ExpCalculator.calculateResistanceExp(calories, intensity);

        System.out.println("\n===== 紀錄運動: " + info.getName() + " (重量訓練) =====");
        System.out.printf("儲存消耗熱量: %.1f 大卡\n", calories);
        System.out.println("重訓強度分級 (1~10): " + intensity + " 級");
        System.out.printf("獲得經驗值(熱量+強度): %.1f\n", earnedExp);

        // 3. 只有重訓才會計算並增長肌肉數據
        String target = info.getTargetMuscle();
        int muscleGain = intensity * 2;
        
        if (target.equals("FullBody")) {
            player.trainMuscle("Chest", intensity);
            player.trainMuscle("Back", intensity);
            player.trainMuscle("Legs", intensity);
            System.out.println("全身性重訓！全身肌肉獲得全面刺激！");
        } else {
            player.trainMuscle(target, muscleGain);
            System.out.println("【肌肉成長】" + target + " 肌肉量增加了 " + muscleGain);
        }

        // 4. 套用獎勵與檢查升級
        player.setCurrentExp(player.getCurrentExp() + earnedExp);
        checkLevelUp();

        // 結算後掃描成就
        triggerAchievementCheck();
    }

    // 封裝一個成就判定處理
    private void triggerAchievementCheck() {
        List<Achievement> newUnlocks = achievementManager.checkAndUnlock(player);
        if (!newUnlocks.isEmpty()) {
            System.out.println("\n【成就解鎖通知】");
            for (Achievement ach : newUnlocks) {
                System.out.println(">> 恭喜解鎖成就:「" + ach.getTitle() + "」— " + ach.getDescription());
            }
            System.out.println("=======================");
        }
    }

    private void checkLevelUp() {
        while (player.getCurrentExp() >= player.getMaxExp()) {
            player.setCurrentExp(player.getCurrentExp() - player.getMaxExp());
            player.setLevel(player.getLevel() + 1);
            player.setMaxExp(player.getMaxExp() * 1.2);
            System.out.println("升級了！恭喜你變得更強壯了！目前等級: Lv." + player.getLevel());
        }
    }

    public void printPlayerStatus() {
        System.out.println("\n--- " + player.getName() + " 狀態面板 ---");
        System.out.println("等級: Lv." + player.getLevel() + " | 經驗值: " + String.format("%.1f", player.getCurrentExp()) + " / " + String.format("%.1f", player.getMaxExp()));
        System.out.println("肌肉數據: " + player.getMuscleParts());
        System.out.println("--------------------------------");
    }

    // 提供公開的成就列表獲取（GUI 渲染成就牆時會用到）
    public List<Achievement> getAllAchievements() {
        return achievementManager.getAchievementList();
    }
}