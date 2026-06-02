package fithero.model.exercise;

import java.util.HashMap;
import java.util.Map;

/**
 * 70種科學運動中央註冊表資料庫（完全去耦合完美版）
 */
public class ExerciseRegistry {
    private static final Map<String, ExerciseInfo> registry = new HashMap<>();

    static {
        // ==================== 有氧運動 (共 30 種) ====================
        registry.put("慢跑 (輕鬆)", new ExerciseInfo("慢跑 (輕鬆)", 7.0));
        registry.put("快跑 (高強度)", new ExerciseInfo("快跑 (高強度)", 11.0));
        registry.put("健走", new ExerciseInfo("健走", 4.3));
        registry.put("散步", new ExerciseInfo("散步", 2.9));
        registry.put("越野跑", new ExerciseInfo("越野跑", 9.0));
        
        registry.put("游泳 (蛙式)", new ExerciseInfo("游泳 (蛙式)", 5.3));
        registry.put("游泳 (自由式)", new ExerciseInfo("游泳 (自由式)", 8.0));
        registry.put("騎自行車 (休閒)", new ExerciseInfo("騎自行車 (休閒)", 6.0));
        registry.put("騎自行車 (競速)", new ExerciseInfo("騎自行車 (競速)", 10.0));
        registry.put("跳繩 (慢速)", new ExerciseInfo("跳繩 (慢速)", 8.8));
        
        registry.put("跳繩 (快速)", new ExerciseInfo("跳繩 (快速)", 12.3));
        registry.put("尊巴舞蹈 (Zumba)", new ExerciseInfo("尊巴舞蹈 (Zumba)", 6.5));
        registry.put("嘻哈街舞", new ExerciseInfo("嘻哈街舞", 5.0));
        registry.put("高強度有氧循環 (HIIT)", new ExerciseInfo("高強度有氧循環 (HIIT)", 8.0));
        registry.put("Tabata 循環", new ExerciseInfo("Tabata 循環", 8.5));
        
        registry.put("飛輪車 (高強度)", new ExerciseInfo("飛輪車 (高強度)", 8.5));
        registry.put("登階機", new ExerciseInfo("登階機", 9.0));
        registry.put("划船機", new ExerciseInfo("划船機", 7.0));
        registry.put("橢圓機", new ExerciseInfo("橢圓機", 5.0));
        registry.put("有氧拳擊 (BodyCombat)", new ExerciseInfo("有氧拳擊 (BodyCombat)", 7.8));
        
        registry.put("籃球 (全場比賽)", new ExerciseInfo("籃球 (全場比賽)", 8.0));
        registry.put("籃球 (投籃練習)", new ExerciseInfo("籃球 (投籃練習)", 4.5));
        registry.put("足球", new ExerciseInfo("足球", 7.0));
        registry.put("羽毛球 (單打)", new ExerciseInfo("羽毛球 (單打)", 7.0));
        registry.put("羽毛球 (雙打)", new ExerciseInfo("羽毛球 (雙打)", 4.5));
        
        registry.put("網球", new ExerciseInfo("網球", 7.3));
        registry.put("排球", new ExerciseInfo("排球", 4.0));
        registry.put("桌球 (乒乓球)", new ExerciseInfo("桌球 (乒乓球)", 4.0));
        registry.put("壁球", new ExerciseInfo("壁球", 12.0));
        registry.put("棒式核心維持", new ExerciseInfo("棒式核心維持", 3.3));

        // ==================== 重訓 / 阻力訓練 (共 40 種) ====================
        registry.put("伏地挺身", new ExerciseInfo("伏地挺身", MuscleGroup.CHEST, true, 1.0));
        registry.put("槓鈴臥推", new ExerciseInfo("槓鈴臥推", MuscleGroup.CHEST, false, 1.0));
        registry.put("啞鈴臥推", new ExerciseInfo("啞鈴臥推", MuscleGroup.CHEST, false, 1.2));
        registry.put("上斜啞鈴臥推", new ExerciseInfo("上斜啞鈴臥推", MuscleGroup.CHEST, false, 1.3));
        registry.put("下斜槓鈴臥推", new ExerciseInfo("下斜槓鈴臥推", MuscleGroup.CHEST, false, 0.9));
        registry.put("機械夾胸", new ExerciseInfo("機械夾胸", MuscleGroup.CHEST, false, 1.8));
        registry.put("啞鈴飛鳥", new ExerciseInfo("啞鈴飛鳥", MuscleGroup.CHEST, false, 2.5));
        registry.put("滑輪纜繩交叉飛鳥", new ExerciseInfo("滑輪纜繩交叉飛鳥", MuscleGroup.CHEST, false, 2.2));
        registry.put("雙槓體撐 (胸肌偏向)", new ExerciseInfo("雙槓體撐 (胸肌偏向)", MuscleGroup.CHEST, true, 1.0));
        registry.put("鑽石伏地挺身", new ExerciseInfo("鑽石伏地挺身", MuscleGroup.CHEST, true, 1.1));

        registry.put("滑輪下拉", new ExerciseInfo("滑輪下拉", MuscleGroup.BACK, false, 1.2));
        registry.put("引體向上", new ExerciseInfo("引體向上", MuscleGroup.BACK, true, 1.0));
        registry.put("槓鈴划船", new ExerciseInfo("槓鈴划船", MuscleGroup.BACK, false, 1.1));
        registry.put("單臂啞鈴划船", new ExerciseInfo("單臂啞鈴划船", MuscleGroup.BACK, false, 1.4));
        registry.put("坐姿反衝划船", new ExerciseInfo("坐姿反衝划船", MuscleGroup.BACK, false, 1.3));
        registry.put("T桿划船", new ExerciseInfo("T桿划船", MuscleGroup.BACK, false, 1.0));
        registry.put("直臂滑輪下拉", new ExerciseInfo("直臂滑輪下拉", MuscleGroup.BACK, false, 1.8));
        registry.put("超人式背肌背屈", new ExerciseInfo("超人式背肌背屈", MuscleGroup.BACK, true, 1.0));
        registry.put("反向飛鳥 (後三角)", new ExerciseInfo("反向飛鳥 (後三角)", MuscleGroup.BACK, false, 3.5));
        registry.put("機械夾背", new ExerciseInfo("機械夾背", MuscleGroup.BACK, false, 1.5));

        registry.put("徒手深蹲", new ExerciseInfo("徒手深蹲", MuscleGroup.LEGS, true, 1.0));
        registry.put("槓鈴深蹲", new ExerciseInfo("槓鈴深蹲", MuscleGroup.LEGS, false, 0.8));
        registry.put("傳統硬舉", new ExerciseInfo("傳統硬舉", MuscleGroup.LEGS, false, 0.7));
        registry.put("相撲硬舉", new ExerciseInfo("相撲硬舉", MuscleGroup.LEGS, false, 0.65));
        registry.put("啞鈴保加利亞分腿蹲", new ExerciseInfo("啞鈴保加利亞分腿蹲", MuscleGroup.LEGS, false, 1.6));
        registry.put("機械腿推舉", new ExerciseInfo("機械腿推舉", MuscleGroup.LEGS, false, 0.5)); 
        registry.put("機械腿伸展", new ExerciseInfo("機械腿伸展", MuscleGroup.LEGS, false, 1.5));
        registry.put("機械俯臥腿彎舉", new ExerciseInfo("機械俯臥腿彎舉", MuscleGroup.LEGS, false, 1.5));
        registry.put("啞鈴弓步蹲", new ExerciseInfo("啞鈴弓步蹲", MuscleGroup.LEGS, false, 1.4));
        registry.put("提踵 (小腿訓練)", new ExerciseInfo("提踵 (小腿訓練)", MuscleGroup.LEGS, true, 1.0));

        registry.put("啞鈴二頭彎舉", new ExerciseInfo("啞鈴二頭彎舉", MuscleGroup.ARMS, false, 3.5));
        registry.put("槓鈴上舉肩推", new ExerciseInfo("槓鈴上舉肩推", MuscleGroup.ARMS, false, 1.4));
        registry.put("啞鈴側平舉", new ExerciseInfo("啞鈴側平举", MuscleGroup.ARMS, false, 5.0)); 
        registry.put("滑輪三頭下壓", new ExerciseInfo("滑輪三頭下壓", MuscleGroup.ARMS, false, 2.0));
        registry.put("啞鈴錘式彎舉", new ExerciseInfo("啞鈴錘式彎舉", MuscleGroup.ARMS, false, 3.2));
        registry.put("仰臥起坐", new ExerciseInfo("仰臥起坐", MuscleGroup.ABS, true, 1.0));
        registry.put("捲腹", new ExerciseInfo("捲腹", MuscleGroup.ABS, true, 1.0));
        registry.put("俄羅斯轉體", new ExerciseInfo("俄羅斯轉體", MuscleGroup.ABS, true, 1.2));
        registry.put("懸垂舉腿", new ExerciseInfo("懸垂舉腿", MuscleGroup.ABS, true, 1.5));
        registry.put("鳥狗式核心穩定", new ExerciseInfo("鳥狗式核心穩定", MuscleGroup.ABS, true, 1.0));
    }

    public static ExerciseInfo getExercise(String name) { return registry.get(name); }
    public static boolean exists(String name) { return registry.containsKey(name); }
}