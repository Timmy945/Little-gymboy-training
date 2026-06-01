package fithero.logic.manager;

import fithero.model.achievement.Achievement;
import fithero.model.exercise.MuscleGroup;
import fithero.model.player.Avatar;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 100項榮譽成就即時檢測與防抹除持久化中心
 */
public class AchievementManager {
    private final List<Achievement> achievementList = new ArrayList<>();
    private final Path achFile = Path.of("data", "unlocked_achievements.properties");
    private final Properties achProps = new Properties();

    public AchievementManager() {
        init100Achievements();
        loadUnlockedStates(); 
    }

    private void init100Achievements() {
        // 1. 簡單成就 (40個)
        for (int i = 1; i <= 10; i++) {
            achievementList.add(new Achievement("LV_S_" + i, "等級初階拓荒者 T" + i, "人偶等級達到 Lv." + i, "簡單", false));
        }
        for (MuscleGroup group : MuscleGroup.values()) {
            for (int j = 1; j <= 6; j++) {
                int targetVal = j * 5;
                achievementList.add(new Achievement(group.name() + "_S_" + targetVal, group.displayName() + " 起步突破 " + targetVal, "部位肌肉量達到 " + targetVal + " 點", "簡單", false));
            }
        }

        // 2. 普通成就 (30個)
        for (int i = 11; i <= 20; i++) {
            achievementList.add(new Achievement("LV_M_" + i, "進階核心開拓者 M" + (i-10), "人偶等級達到 Lv." + i, "普通", false));
        }
        for (MuscleGroup group : MuscleGroup.values()) {
            for (int j = 1; j <= 4; j++) {
                int targetVal = 30 + (j * 5);
                achievementList.add(new Achievement(group.name() + "_M_" + targetVal, group.displayName() + " 核心覺醒 " + targetVal, "部位肌肉量達到 " + targetVal + " 點", "普通", false));
            }
        }

        // 3. 困難成就 (20個)
        for (int i = 21; i <= 25; i++) {
            achievementList.add(new Achievement("LV_H_" + i, "鐵血鍛造大師 H" + (i-20), "人偶等級達到 Lv." + i, "困難", false));
        }
        for (MuscleGroup group : MuscleGroup.values()) {
            for (int j = 1; j <= 3; j++) {
                int targetVal = 50 + (j * 10);
                achievementList.add(new Achievement(group.name() + "_H_" + targetVal, group.displayName() + " 解放突破 " + targetVal, "部位肌肉量達到 " + targetVal + " 點", "困難", false));
            }
        }

        // 4. 極困難成就 (5個)
        achievementList.add(new Achievement("LV_MAX", "超越臨界點的究極體", "人偶總等級突破 Lv.50 大關", "極困難", false));
        achievementList.add(new Achievement("CHEST_GOD", "奧林匹亞防彈神胸", "胸肌數據突破 150 點", "極困難", false));
        achievementList.add(new Achievement("BACK_GOD", "制霸賽場的撒旦之背", "背肌數據突破 150 點", "極困難", false));
        achievementList.add(new Achievement("LEGS_GOD", "粉碎地表的泰坦重腿", "腿部數據突破 150 點", "極困難", false));
        achievementList.add(new Achievement("ARMS_GOD", "撕裂袖口的雷神麒麟臂", "手臂數據突破 150 點", "極困難", false));

        // 5. 隱藏成就 (5個)
        achievementList.add(new Achievement("SECRET_LAZY", "基因退化狂潮", "被系統執行偷懶懲罰累計達到 3 次", "簡單", true));
        achievementList.add(new Achievement("SECRET_STREAK", "多靈果血脈繼承者", "連續三週以上維持無偷懶加成狀態", "困難", true));
        achievementList.add(new Achievement("SECRET_ALL_30", "黃金比例終極巨巨", "全身所有肌肉部位數據皆突破 30 點", "普通", true));
        achievementList.add(new Achievement("SECRET_WEIGHT", "重組生物學特徵", "在個人資料分頁中成功修改過 5 次特徵", "簡單", true));
        achievementList.add(new Achievement("SECRET_GOD_REPS", "瘋狂力竭次數突破", "單次推舉訓練數量超過 50 下", "困難", true));
    }

    private void loadUnlockedStates() {
        if (Files.exists(achFile)) {
            try (var reader = Files.newBufferedReader(achFile, StandardCharsets.UTF_8)) {
                achProps.load(reader);
                for (Achievement ach : achievementList) {
                    if ("true".equals(achProps.getProperty("ach." + ach.getId() + ".unlocked"))) {
                        ach.setUnlocked(true);
                    }
                }
            } catch (IOException ignored) {}
        }
    }

    private void saveUnlockedState(String id) {
        achProps.setProperty("ach." + id + ".unlocked", "true");
        try {
            Files.createDirectories(achFile.getParent());
            try (var writer = Files.newBufferedWriter(achFile, StandardCharsets.UTF_8)) {
                achProps.store(writer, "FitQuest Unlocked Achievements Archive");
            }
        } catch (IOException ignored) {}
    }

    public List<Achievement> checkAndUnlock(Avatar player) {
        List<Achievement> newlyUnlocked = new ArrayList<>();
        Map<MuscleGroup, Integer> muscles = player.getMuscleParts();

        for (Achievement ach : achievementList) {
            if (ach.isUnlocked()) continue;

            boolean conditionMet = false;
            String id = ach.getId();

            if (id.startsWith("LV_")) {
                int reqLv = Integer.parseInt(id.substring(id.lastIndexOf("_") + 1).replace("MAX", "50"));
                if (player.getLevel() >= reqLv) conditionMet = true;
            } else if (id.contains("_S_") || id.contains("_M_") || id.contains("_H_")) {
                String partStr = id.substring(0, id.indexOf("_"));
                MuscleGroup group = MuscleGroup.valueOf(partStr);
                int reqVal = Integer.parseInt(id.substring(id.lastIndexOf("_") + 1));
                if (muscles.getOrDefault(group, 0) >= reqVal) conditionMet = true;
            } else if (id.equals("CHEST_GOD") && muscles.getOrDefault(MuscleGroup.CHEST, 0) >= 150) conditionMet = true;
            else if (id.equals("BACK_GOD") && muscles.getOrDefault(MuscleGroup.BACK, 0) >= 150) conditionMet = true;
            else if (id.equals("LEGS_GOD") && muscles.getOrDefault(MuscleGroup.LEGS, 0) >= 150) conditionMet = true;
            else if (id.equals("ARMS_GOD") && muscles.getOrDefault(MuscleGroup.ARMS, 0) >= 150) conditionMet = true;
            else if (id.equals("SECRET_ALL_30")) {
                if (muscles.getOrDefault(MuscleGroup.CHEST, 0) >= 30 && muscles.getOrDefault(MuscleGroup.BACK, 0) >= 30 &&
                    muscles.getOrDefault(MuscleGroup.LEGS, 0) >= 30 && muscles.getOrDefault(MuscleGroup.ARMS, 0) >= 30 &&
                    muscles.getOrDefault(MuscleGroup.ABS, 0) >= 30) conditionMet = true;
            }

            if (conditionMet) {
                ach.setUnlocked(true);
                saveUnlockedState(id);
                newlyUnlocked.add(ach);
            }
        }
        return newlyUnlocked;
    }

    public List<Achievement> getAchievementList() { return achievementList; }
}