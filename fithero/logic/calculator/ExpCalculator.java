package com.fithero.logic.calculator;

import com.fithero.model.exercise.ExerciseInfo;
import com.fithero.model.player.Gender;

public class ExpCalculator {
    // ------------------ 1. 熱量計算 (維持原樣) ------------------
    public static double calculateAerobicCalories(ExerciseInfo info, double minutes, double weight) {
        return (minutes / 60.0) * info.getMetValue() * weight;
    }

    public static double calculateResistanceCalories(ExerciseInfo info, double weightLifted, int reps, int sets, double userWeight) {
        if (info.isBodyweight()) {
            return (userWeight * 0.1) * reps * sets * 0.05;
        } else {
            double totalVolume = weightLifted * reps * sets;
            return (totalVolume * 0.06) * (userWeight / 70.0);
        }
    }

    // ------------------ 2. 強度計算 (有氧歸零，重訓細化至10級) ------------------
    public static int calculateResistanceIntensity(ExerciseInfo info, double weightLifted, double userWeight, Gender gender) {
        if (info.isBodyweight()) return 3; // 自重動作維持基礎強度 3
        
        // 1. 基礎重量體重比
        double baseRatio = weightLifted / userWeight;
        
        // 2. 乘上該動作的難度修正
        double adjustedRatio = baseRatio * info.getDifficultyCoefficient();
        
        // 3. 乘上性別修正係數 (生理統計上女性上限力量約男性的 65%~70%，故女性強度加權 1.4 倍)
        if (gender == Gender.FEMALE) {
            adjustedRatio *= 1.4;
        }

        // 4. 根據最終的調整比率 (Score) 給予 1~10 級判定
        if (adjustedRatio < 0.2)  return 1;
        if (adjustedRatio < 0.4)  return 2;
        if (adjustedRatio < 0.6)  return 3;
        if (adjustedRatio < 0.8)  return 4;
        if (adjustedRatio < 1.0)  return 5;
        if (adjustedRatio < 1.2)  return 6;
        if (adjustedRatio < 1.4)  return 7;
        if (adjustedRatio < 1.6)  return 8;
        if (adjustedRatio < 1.9)  return 9;
        return 10; 
    }

    // ------------------ 3. 遊戲經驗值公式 ------------------
    // 有氧運動經驗值：純看熱量
    public static double calculateAerobicExp(double calories) {
        return calories * 0.5; 
    }

    // 重量訓練經驗值：熱量 + 重訓強度加權
    public static double calculateResistanceExp(double calories, int intensity) {
        return (calories * 0.3) + (intensity * 15.0);
    }
}