package com.fithero.logic.manager;

import com.fithero.model.achievement.Achievement;
import com.fithero.model.player.Avatar;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AchievementManager {
    private List<Achievement> achievementList;

    public AchievementManager() {
        this.achievementList = new ArrayList<>();
        initAchievements();
    }

    // 1. 初始化定義你的養成遊戲成就
    private void initAchievements() {
        // 等級類成就
        achievementList.add(new Achievement("LV_5", "健身入門者", "人偶等級達到 Lv.5"));
        achievementList.add(new Achievement("LV_20", "健身狂熱份子", "人偶等級達到 Lv.20"));
        
        // 肌肉部位突破類成就 (各部位 > 50)
        achievementList.add(new Achievement("CHEST_50", "防彈胸肌", "胸肌 (Chest) 數據突破 50"));
        achievementList.add(new Achievement("BACK_50", "倒三角之鬼", "背肌 (Back) 數據突破 50"));
        achievementList.add(new Achievement("LEGS_50", "大力金剛腿", "腿部 (Legs) 數據突破 50"));
        achievementList.add(new Achievement("ARMS_50", "麒麟臂", "手臂 (Arms) 數據突破 50"));
        achievementList.add(new Achievement("CORE_50", "巧克力腹肌", "核心 (Core) 數據突破 50"));
        
        // 全能綜合類成就
        achievementList.add(new Achievement("ALL_ROUNDER", "終極巨巨", "所有肌肉部位數據皆突破 30"));
    }

    // return List<Achievement> 回傳本次「新解鎖」的成就清單（方便 GUI 跳出通知）
    public List<Achievement> checkAndUnlock(Avatar player) {
        List<Achievement> newlyUnlocked = new ArrayList<>();
        Map<String, Integer> muscles = player.getMuscleParts();

        for (Achievement ach : achievementList) {
            // 如果已經解鎖過了，就跳過不重複檢查
            if (ach.isUnlocked()) { continue; }

            boolean conditionMet = false;

            // 依據 ID 撰寫科學的判定邏輯
            switch (ach.getId()) {
                case "LV_5":
                    if (player.getLevel() >= 5) { conditionMet = true; }
                    break;
                case "LV_20":
                    if (player.getLevel() >= 20) { conditionMet = true; }
                    break;
                case "CHEST_50":
                    if (muscles.get("Chest") >= 50) { conditionMet = true; }
                    break;
                case "BACK_50":
                    if (muscles.get("Back") >= 50) { conditionMet = true; }
                    break;
                case "LEGS_50":
                    if (muscles.get("Legs") >= 50) { conditionMet = true; }
                    break;
                case "ARMS_50":
                    if (muscles.get("Arms") >= 50) { conditionMet = true; }
                    break;
                case "CORE_50":
                    if (muscles.get("Core") >= 50) { conditionMet = true; }
                    break;
                case "ALL_ROUNDER":
                    if (muscles.get("Chest") >= 30 && 
                        muscles.get("Back") >= 30 && 
                        muscles.get("Legs") >= 30 && 
                        muscles.get("Arms") >= 30 && 
                        muscles.get("Core") >= 30) {
                        conditionMet = true;
                    }
                    break;
            }

            // 如果滿足條件，正式解鎖並丟入新解鎖清單
            if (conditionMet) {
                ach.setUnlocked(true);
                newlyUnlocked.add(ach);
            }
        }
        return newlyUnlocked;
    }

    // 獲取目前所有的成就狀態 (供未來成就面板 GUI 顯示使用)
    public List<Achievement> getAchievementList() {
        return achievementList;
    }
}