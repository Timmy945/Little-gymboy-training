package fithero.sandbox;

import fithero.logic.manager.PlayerState;
import fithero.model.player.Gender;
import fithero.model.exercise.MuscleGroup;
import fithero.model.workout.WorkoutEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 獨立測試沙盒：用於在不開圖形介面的情況下，深度驗證重訓公式與肌群增長分流。
 */
public class ScientificCheckSandbox {
    public static void main(String[] args) {
        System.out.println("======  [科學核心驗證沙盒啟動] ======");

        PlayerState playerState = new PlayerState("小明", 175.0, 70.0, Gender.MALE);
        
        int initialLevel = playerState.level();
        int initialXp = playerState.xp();
        var initialMuscles = playerState.getAvatar().getMuscleParts();
        
        System.out.println("[初始狀態]：玩家等級: Lv." + initialLevel + " | 經驗值: " + initialXp);
        System.out.println("[初始肌肉量]：胸肌: " + initialMuscles.get(MuscleGroup.CHEST) + " | 背肌: " + initialMuscles.get(MuscleGroup.BACK));

        // 虛擬模擬所需的歷史日誌與計畫設定
        List<WorkoutEntry> dummyHistory = new ArrayList<>();
        Properties dummyPlanProps = new Properties();

        System.out.println("\n進行模擬結算測試...");
        // 對接全新計算管線參數
        playerState.submitResistanceWorkout("槓鈴臥推", 100.0, 8, 3, dummyHistory, dummyPlanProps);

        System.out.println("\n====== 📊 [驗證數據比對報告] ======");
        int finalLevel = playerState.level();
        int finalXp = playerState.xp();
        var finalMuscles = playerState.getAvatar().getMuscleParts();

        System.out.println("1. 經驗值增長檢查：");
        System.out.println("   >> 完訓後經驗值: " + finalXp + " XP (初始為 " + initialXp + " XP)");
        if (finalXp > initialXp || finalLevel > initialLevel) {
            System.out.println("   [PASS] 經驗值/等級計算成功！");
        } else {
            System.err.println("   [FAIL] 數據未變動！");
        }

        System.out.println("\n2. 部位肌肉科學分流檢查：");
        System.out.println("   >> 胸肌 最新數值: " + finalMuscles.get(MuscleGroup.CHEST) + " (因臥推應顯著成長)");
        System.out.println("   >> 背肌 最新數值: " + finalMuscles.get(MuscleGroup.BACK) + " (應保持為 0)");

        if (finalMuscles.get(MuscleGroup.CHEST) > 0 && finalMuscles.get(MuscleGroup.BACK) == 0) {
            System.out.println("\n [測試結論：PASS] 核心部位數據分流完全精準！");
        } else {
            System.err.println("\n [測試結論：FAIL] 結算邏輯存在嚴重漏洞！");
        }
        System.out.println("==============================================");
    }
}