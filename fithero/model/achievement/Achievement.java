package fithero.model.achievement;

/**
 * 成就實體模型
 */
public class Achievement {
    private final String id;
    private final String title;
    private final String description;
    private final String difficulty; 
    private final boolean isHidden;  
    private boolean unlocked;

    public Achievement(String id, String title, String description, String difficulty, boolean isHidden) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.difficulty = difficulty;
        this.isHidden = isHidden;
        this.unlocked = false;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getDifficulty() { return difficulty; }
    public boolean isHidden() { return isHidden; }
    public boolean isUnlocked() { return unlocked; }
    public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }
}