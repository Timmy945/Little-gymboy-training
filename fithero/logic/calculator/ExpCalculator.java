package fithero.logic.calculator;

import fithero.model.exercise.ExerciseInfo;
import fithero.model.player.Gender;

/**
 * 遊戲化卡路里與經驗值科學計算引擎
 */
public class ExpCalculator {

    public static double calculateAerobicCalories(ExerciseInfo info, double minutes, double weight) {
        // 修正：將 getMetValue() 修改為 info.getMet()，打通有氧卡路里管線
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
     * 採用非線性飽和增長函式，確保重量越重絕對嚴格遞增，但高重量區邊際效應遞減。
     * 動態引導玩家將訓練核心鎖定在最划算的「70%~80% 1RM 黃金肌肥大區間」！
     */
    public static int calculateResistanceIntensity(
            ExerciseInfo info, 
            double weightLifted, 
            double userWeight, 
            Gender gender, 
            int age, 
            double estimatedMuscleMass) {
        
        if (weightLifted <= 0) return 1; // 自重訓練保底 1 級

        // 因子 1：性別加權（女性生理肌肉上限放寬門檻，取得更好的 RPG 平衡體感）
        double genderFactor = (gender == Gender.MALE) ? 1.0 : 0.75;

        // 因子 2：年齡退化自然補償（30歲後每多一歲，門檻微降 0.5%，等同於對大齡巨巨進行遊戲加權）
        double ageFactor = 1.0;
        if (age > 30) {
            ageFactor = 1.0 - ((age - 30) * 0.005);
            if (ageFactor < 0.75) ageFactor = 0.75; // 設置最大保護防線
        }

        // 核心科學對齊：計算出該使用者的「當前生理肌力基準線」
        // 拋棄粗暴的總體重，改用「真實肌肉量 × 性別權重 × 年齡權重」
        double adaptiveMuscleBase = estimatedMuscleMass * genderFactor * ageFactor;

        // 計算相對舉重係數
        double relativeStrengthRatio = (weightLifted * info.getDifficultyMultiplier()) / adaptiveMuscleBase;

        // 希爾飽和型非線性遞增函式
        // 當 relativeStrengthRatio 落在 0.7 ~ 1.1 區間時斜率最陡峭，反饋最敏感；高重量區邊際收益自然平緩
        double rSquared = relativeStrengthRatio * relativeStrengthRatio;
        double k = 0.64; // 黃金阻尼常數

        int intensity = 1 + (int) Math.floor(9 * (rSquared / (rSquared + k)));

        // 設定科學安全防護邊界 (1 ~ 10 級)
        if (intensity > 10) intensity = 10;
        if (intensity < 1)  intensity = 1;

        return intensity;
    }

    public static double calculateAerobicExp(double calories) {
        return calories * 0.5; 
    }

    public static double calculateResistanceExp(double calories, int intensity) {
        return (calories * 0.3) + (intensity * 20.0);
    }
}