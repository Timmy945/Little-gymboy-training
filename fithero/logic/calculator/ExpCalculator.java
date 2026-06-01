package fithero.logic.calculator;

import fithero.model.exercise.ExerciseInfo;
import fithero.model.player.Gender;

/**
 * 遊戲化卡路里與經驗值科學計算引擎
 */
public class ExpCalculator {

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

    public static int calculateResistanceIntensity(ExerciseInfo info, double weightLifted, double userWeight, Gender gender) {
        if (info.isBodyweight()) return 3; 
        
        double baseRatio = weightLifted / userWeight;
        double adjustedRatio = baseRatio * info.getDifficultyCoefficient();
        
        if (gender == Gender.FEMALE) {
            adjustedRatio *= 1.4;
        }

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

    public static double calculateAerobicExp(double calories) {
        return calories * 0.5; 
    }

    public static double calculateResistanceExp(double calories, int intensity) {
        return (calories * 0.3) + (intensity * 15.0);
    }
}