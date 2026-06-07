package fithero.logic.calculator;

import fithero.model.exercise.ExerciseInfo;
import fithero.model.player.Gender;

/**
 * 遊戲化卡路里與經驗值科學計算引擎 (動態控速防通膨優化版)
 */
public class ExpCalculator {

    public static double calculateAerobicCalories(ExerciseInfo info, double minutes, double weight) {
        return (minutes / 60.0) * info.getMet() * weight;
    }

    public static double calculateResistanceCalories(ExerciseInfo info, double weightLifted, int reps, int sets, double userWeight) {
        if (info.isBodyweight()) {
            return (userWeight * 0.1) * reps * sets * 0.05;
        } else {
            double totalVolume = weightLifted * reps * sets;
            return (totalVolume * 0.06) * (userWeight / 70.0);
        }
    }

    /**
     * 結合 性別、年齡、真實肌肉量 推算最符合個體生理極限的真實強度 (1~10級)
     */
    public static int calculateResistanceIntensity(
            ExerciseInfo info, 
            double weightLifted, 
            double userWeight, 
            Gender gender, 
            int age, 
            double estimatedMuscleMass) {
        
        double effectiveWeight = weightLifted;
        if (info.isBodyweight() && weightLifted <= 0) {
            effectiveWeight = userWeight * 0.65; 
        } else if (effectiveWeight <= 0) {
            return 1; 
        }

        double genderFactor = (gender == Gender.MALE) ? 1.0 : 0.75;

        double ageFactor = 1.0;
        if (age > 30) {
            ageFactor = 1.0 - ((age - 30) * 0.005);
            if (ageFactor < 0.75) ageFactor = 0.75; 
        }

        double adaptiveMuscleBase = estimatedMuscleMass * genderFactor * ageFactor;
        double relativeStrengthRatio = (effectiveWeight * info.getDifficultyMultiplier()) / adaptiveMuscleBase;

        double rSquared = relativeStrengthRatio * relativeStrengthRatio;
        
        // 將阻尼常數由 0.64 擴展至 1.44，拉長肌力回饋拋物線，平緩開局強度
        double k = 1.44; 

        int intensity = 1 + (int) Math.floor(9 * (rSquared / (rSquared + k)));

        if (intensity > 10) intensity = 10;
        if (intensity < 1)  intensity = 1;

        return intensity;
    }

    public static double calculateAerobicExp(double calories) {
        return calories * 0.5; 
    }

    // 將強度基礎獎勵由 20.0 去通膨下修至 4.0，徹底拉長升級週期
    public static double calculateResistanceExp(double calories, int intensity) {
        return (calories * 0.3) + (intensity * 4.0);
    }
}