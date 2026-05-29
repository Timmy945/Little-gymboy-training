package com.fithero.model.exercise;

import java.util.HashMap;
import java.util.Map;

public class ExerciseRegistry {
    private static final Map<String, ExerciseInfo> registry = new HashMap<>();

    static {
        // ==================== 有氧運動 (共 30 種) ====================
        // 1-5: 基礎慢跑與步行
        registry.put("慢跑 (輕鬆)", new ExerciseInfo("慢跑 (輕鬆)", 7.0, "Legs"));
        registry.put("快跑 (高強度)", new ExerciseInfo("快跑 (高強度)", 11.0, "Legs"));
        registry.put("健走", new ExerciseInfo("健走", 4.3, "Legs"));
        registry.put("散步", new ExerciseInfo("散步", 2.9, "Legs"));
        registry.put("越野跑", new ExerciseInfo("越野跑", 9.0, "Legs"));
        
        // 6-10: 水上與常規有氧
        registry.put("游泳 (蛙式)", new ExerciseInfo("游泳 (蛙式)", 5.3, "FullBody"));
        registry.put("游泳 (自由式)", new ExerciseInfo("游泳 (自由式)", 8.0, "FullBody"));
        registry.put("騎自行車 (休閒)", new ExerciseInfo("騎自行車 (休閒)", 6.0, "Legs"));
        registry.put("騎自行車 (競速)", new ExerciseInfo("騎自行車 (競速)", 10.0, "Legs"));
        registry.put("跳繩 (慢速)", new ExerciseInfo("跳繩 (慢速)", 8.8, "Legs"));
        
        // 11-15: 舞蹈與高強度
        registry.put("跳繩 (快速)", new ExerciseInfo("跳繩 (快速)", 12.3, "Legs"));
        registry.put("尊巴舞蹈 (Zumba)", new ExerciseInfo("尊巴舞蹈 (Zumba)", 6.5, "FullBody"));
        registry.put("嘻哈街舞", new ExerciseInfo("嘻哈街舞", 5.0, "FullBody"));
        registry.put("高強度有氧循環 (HIIT)", new ExerciseInfo("高強度有氧循環 (HIIT)", 8.0, "FullBody"));
        registry.put("Tabata 循環", new ExerciseInfo("Tabata 循環", 8.5, "Core"));
        
        // 16-20: 機台類有氧
        registry.put("飛輪車 (高強度)", new ExerciseInfo("飛輪車 (高強度)", 8.5, "Legs"));
        registry.put("登階機", new ExerciseInfo("登階機", 9.0, "Legs"));
        registry.put("划船機", new ExerciseInfo("划船機", 7.0, "Back"));
        registry.put("橢圓機", new ExerciseInfo("橢圓機", 5.0, "FullBody"));
        registry.put("有氧拳擊 (BodyCombat)", new ExerciseInfo("有氧拳擊 (BodyCombat)", 7.8, "Arms"));
        
        // 21-25: 球類運動 (有氧偏向)
        registry.put("籃球 (全場比賽)", new ExerciseInfo("籃球 (全場比賽)", 8.0, "Legs"));
        registry.put("籃球 (投籃練習)", new ExerciseInfo("籃球 (投籃練習)", 4.5, "Arms"));
        registry.put("足球", new ExerciseInfo("足球", 7.0, "Legs"));
        registry.put("羽毛球 (單打)", new ExerciseInfo("羽毛球 (單打)", 7.0, "Arms"));
        registry.put("羽毛球 (雙打)", new ExerciseInfo("羽毛球 (雙打)", 4.5, "Legs"));
        
        // 26-30: 其他球類與核心有氧
        registry.put("網球", new ExerciseInfo("網球", 7.3, "Arms"));
        registry.put("排球", new ExerciseInfo("排球", 4.0, "Legs"));
        registry.put("桌球 (乒乓球)", new ExerciseInfo("桌球 (乒乓球)", 4.0, "Arms"));
        registry.put("壁球", new ExerciseInfo("壁球", 12.0, "Legs"));
        registry.put("棒式核心維持", new ExerciseInfo("棒式核心維持", 3.3, "Core"));


        // ==================== 重訓 / 阻力訓練 (共 40 種) ====================
        // 31-40: 胸部訓練 (Chest) - 槓鈴複合動作係數約 1.0，孤立/飛鳥動作係數較高
        registry.put("伏地挺身", new ExerciseInfo("伏地挺身", "Chest", true, 1.0));
        registry.put("槓鈴臥推", new ExerciseInfo("槓鈴臥推", "Chest", false, 1.0));
        registry.put("啞鈴臥推", new ExerciseInfo("啞鈴臥推", "Chest", false, 1.2));
        registry.put("上斜啞鈴臥推", new ExerciseInfo("上斜啞鈴臥推", "Chest", false, 1.3));
        registry.put("下斜槓鈴臥推", new ExerciseInfo("下斜槓鈴臥推", "Chest", false, 0.9));
        registry.put("機械夾胸", new ExerciseInfo("機械夾胸", "Chest", false, 1.8));
        registry.put("啞鈴飛鳥", new ExerciseInfo("啞鈴飛鳥", "Chest", false, 2.5));
        registry.put("滑輪纜繩交叉飛鳥", new ExerciseInfo("滑輪纜繩交叉飛鳥", "Chest", false, 2.2));
        registry.put("雙槓體撐 (胸肌偏向)", new ExerciseInfo("雙槓體撐 (胸肌偏向)", "Chest", true, 1.0));
        registry.put("鑽石伏地挺身", new ExerciseInfo("鑽石伏地挺身", "Chest", true, 1.1));

        // 41-50: 背部訓練 (Back) - 硬舉出力大係數低，滑輪下拉與飛鳥類係數較高
        registry.put("滑輪下拉", new ExerciseInfo("滑輪下拉", "Back", false, 1.2));
        registry.put("引體向上", new ExerciseInfo("引體向上", "Back", true, 1.0));
        registry.put("槓鈴划船", new ExerciseInfo("槓鈴划船", "Back", false, 1.1));
        registry.put("單臂啞鈴划船", new ExerciseInfo("單臂啞鈴划船", "Back", false, 1.4));
        registry.put("坐姿反衝划船", new ExerciseInfo("坐姿反衝划船", "Back", false, 1.3));
        registry.put("T桿划船", new ExerciseInfo("T桿划船", "Back", false, 1.0));
        registry.put("直臂滑輪下拉", new ExerciseInfo("直臂滑輪下拉", "Back", false, 1.8));
        registry.put("超人式背肌背屈", new ExerciseInfo("超人式背肌背屈", "Back", true, 1.0));
        registry.put("反向飛鳥 (後三角)", new ExerciseInfo("反向飛鳥 (後三角)", "Back", false, 3.5));
        registry.put("機械夾背", new ExerciseInfo("機械夾背", "Back", false, 1.5));

        // 51-60: 腿部訓練 (Legs) - 深蹲硬舉基礎推力極大，係數設定較低
        registry.put("徒手深蹲", new ExerciseInfo("徒手深蹲", "Legs", true, 1.0));
        registry.put("槓鈴深蹲", new ExerciseInfo("槓鈴深蹲", "Legs", false, 0.8));
        registry.put("傳統硬舉", new ExerciseInfo("傳統硬舉", "Legs", false, 0.7));
        registry.put("相撲硬舉", new ExerciseInfo("相撲硬舉", "Legs", false, 0.65));
        registry.put("啞鈴保加利亞分腿蹲", new ExerciseInfo("啞鈴保加利亞分腿蹲", "Legs", false, 1.6));
        registry.put("機械腿推舉", new ExerciseInfo("機械腿推舉", "Legs", false, 0.5)); // 腿推機有固定軌道且角度省力
        registry.put("機械腿伸展", new ExerciseInfo("機械腿伸展", "Legs", false, 1.5));
        registry.put("機械俯臥腿彎舉", new ExerciseInfo("機械俯臥腿彎舉", "Legs", false, 1.5));
        registry.put("啞鈴弓步蹲", new ExerciseInfo("啞鈴弓步蹲", "Legs", false, 1.4));
        registry.put("提踵 (小腿訓練)", new ExerciseInfo("提踵 (小腿訓練)", "Legs", true, 1.0));

        // 61-70: 肩、臂、核心訓練 (Arms / 肩膀 / Core) - 小肌群與側平舉動作係數極高
        registry.put("啞鈴二頭彎舉", new ExerciseInfo("啞鈴二頭彎舉", "Arms", false, 3.5));
        registry.put("槓鈴上舉肩推", new ExerciseInfo("槓鈴上舉肩推", "Arms", false, 1.4));
        registry.put("啞鈴側平舉", new ExerciseInfo("啞鈴側平舉", "Arms", false, 5.0)); // 側平舉用小重量即有高刺激度
        registry.put("滑輪三頭下壓", new ExerciseInfo("滑輪三頭下壓", "Arms", false, 2.0));
        registry.put("啞鈴錘式彎舉", new ExerciseInfo("啞鈴錘式彎舉", "Arms", false, 3.2));
        registry.put("仰臥起坐", new ExerciseInfo("仰臥起坐", "Core", true, 1.0));
        registry.put("捲腹", new ExerciseInfo("捲腹", "Core", true, 1.0));
        registry.put("俄羅斯轉體", new ExerciseInfo("俄羅斯轉體", "Core", true, 1.2));
        registry.put("懸垂舉腿", new ExerciseInfo("懸垂舉腿", "Core", true, 1.5));
        registry.put("鳥狗式核心穩定", new ExerciseInfo("鳥狗式核心穩定", "Core", true, 1.0));
    }

    public static ExerciseInfo getExercise(String name) {
        return registry.get(name);
    }
    
    public static boolean exists(String name) {
        return registry.containsKey(name);
    }
}