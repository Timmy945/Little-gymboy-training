package fithero.model.player;

import fithero.model.exercise.MuscleGroup;
import java.util.EnumMap;
import java.util.Map;

/**
 * 遊戲化虛擬角色人偶核心模型
 */
public class Avatar {
    private String name;
    private int level;
    private double currentExp;
    private double maxExp;
    
    // 升級：使用 EnumMap 替代原本脆弱的 String Map，確保資料流編譯期安全
    private final Map<MuscleGroup, Integer> muscleParts;
    private final UserProfile profile; 

    public Avatar(String name, double height, double weight, Gender gender) {
        // 安全防線：暱稱同樣過濾逗號，防止與儲存管線衝突
        this.name = name != null ? name.replace(",", " ") : "新冒險者";
        this.level = 1;
        this.currentExp = 0.0;
        this.maxExp = 100.0;
        this.profile = new UserProfile(height, weight, gender);
        
        this.muscleParts = new EnumMap<>(MuscleGroup.class);
        for (MuscleGroup group : MuscleGroup.values()) {
            this.muscleParts.put(group, 0);
        }
    }

    public String getName() { return name; }
    public void setName(String name) { 
        this.name = name != null ? name.replace(",", " ") : "新冒險者"; 
    }
    
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public double getCurrentExp() { return currentExp; }
    public void setCurrentExp(double currentExp) { this.currentExp = currentExp; }
    public double getMaxExp() { return maxExp; }
    public void setMaxExp(double maxExp) { this.maxExp = maxExp; }
    
    public Map<MuscleGroup, Integer> getMuscleParts() { return muscleParts; }
    public UserProfile getProfile() { return profile; }

    /**
     * 長肌肉核心引擎：直接傳入強型別 Enum 執行成長
     */
    public void trainMuscle(MuscleGroup part, int amount) {
        if (part != null && muscleParts.containsKey(part)) {
            muscleParts.put(part, muscleParts.get(part) + amount);
        }
    }
}