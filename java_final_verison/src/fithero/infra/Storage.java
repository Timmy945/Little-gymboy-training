package fithero.infra;

import fithero.logic.manager.PlayerState;
import fithero.logic.manager.FitnessGoal;
import fithero.model.player.Avatar;
import fithero.model.player.Gender;
import fithero.model.exercise.MuscleGroup;
import fithero.model.workout.WorkoutEntry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 核心儲存管理員：具備多執行緒防鎖死機制，負責玩家數據與 CSV 日誌的永久保存。
 */
public class Storage {
    private final Path dataDir;
    private final Path playerFile;
    private final Path workoutsFile;

    public Storage(Path dataDir) {
        this.dataDir = dataDir;
        this.playerFile = dataDir.resolve("player.properties");
        this.workoutsFile = dataDir.resolve("workouts.csv");
    }

    /**
     * 讀取玩家資料（含多執行緒同步鎖，防止背景雷達衝突）
     */
    public synchronized PlayerState loadPlayer() {
        if (!Files.exists(playerFile)) {
            System.out.println("[讀檔系統] 找不到現有存檔，自動初始化全新玩家資料。");
            return new PlayerState("小明", 175.0, 70.0, Gender.MALE);
        }

        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(playerFile, StandardCharsets.UTF_8)) {
            properties.load(reader);

            String name = properties.getProperty("profile.name", "小明");
            double height = parseDouble(properties.getProperty("profile.height"), 175.0);
            double weight = parseDouble(properties.getProperty("profile.weight"), 70.0);
            Gender gender = Gender.valueOf(properties.getProperty("profile.gender", "MALE"));

            PlayerState playerState = new PlayerState(name, height, weight, gender);
            Avatar avatar = playerState.getAvatar();

            // 還原最新升級擴充的科學核心特徵
            playerState.setAge(parseInt(properties.getProperty("profile.age"), 25));
            playerState.setTargetWeight(parseDouble(properties.getProperty("profile.targetWeight"), weight));
            playerState.setBodyFatPercent(parseDouble(properties.getProperty("profile.bodyFatPercent"), 0.0));
            playerState.setFitnessGoal(FitnessGoal.valueOf(properties.getProperty("profile.fitnessGoal", "FAT_LOSS")));

            avatar.setLevel(parseInt(properties.getProperty("level"), 1));
            avatar.setCurrentExp(parseDouble(properties.getProperty("xp"), 0.0));
            avatar.setMaxExp(parseDouble(properties.getProperty("maxExp"), 100.0));

            // 【強型別安全重構】利用 Enum 迴圈精準還原後台真實肌肉數據
            for (MuscleGroup group : MuscleGroup.values()) {
                String key = "muscle.raw." + group.name();
                int rawValue = parseInt(properties.getProperty(key), 0);
                avatar.getMuscleParts().put(group, rawValue);
            }

            System.out.println("[讀檔系統] 成功讀取玩家 [" + name + "] 的健身養成進度。");
            return playerState;

        } catch (Exception ex) {
            System.err.println("[讀檔系統] 讀取異常，降級回傳保底對象: " + ex.getMessage());
            return new PlayerState("小明", 175.0, 70.0, Gender.MALE);
        }
    }

    /**
     * 儲存玩家資料（含多執行緒同步鎖，防止寫入時遭受背景執行緒讀取破壞）
     */
    public synchronized void savePlayer(PlayerState playerState) {
        try {
            Files.createDirectories(dataDir);
            Properties properties = new Properties();
            Avatar avatar = playerState.getAvatar();

            // 1. 儲存個人化生物特徵與目標
            properties.setProperty("profile.name", avatar.getName());
            properties.setProperty("profile.height", String.valueOf(avatar.getProfile().getHeight()));
            properties.setProperty("profile.weight", String.valueOf(avatar.getProfile().getWeight()));
            properties.setProperty("profile.gender", avatar.getProfile().getGender().name());
            properties.setProperty("profile.age", String.valueOf(playerState.getAge()));
            properties.setProperty("profile.targetWeight", String.valueOf(playerState.getTargetWeight()));
            properties.setProperty("profile.bodyFatPercent", String.valueOf(playerState.getBodyFatPercent()));
            properties.setProperty("profile.fitnessGoal", playerState.getFitnessGoal().name());

            // 2. 儲存遊戲核心進度
            properties.setProperty("level", String.valueOf(avatar.getLevel()));
            properties.setProperty("xp", String.valueOf(avatar.getCurrentExp()));
            properties.setProperty("maxExp", String.valueOf(avatar.getMaxExp()));

            // 3. 【強型別安全重構】儲存真正的科學肌肉量 Enum 映射
            for (Map.Entry<MuscleGroup, Integer> entry : avatar.getMuscleParts().entrySet()) {
                properties.setProperty("muscle.raw." + entry.getKey().name(), String.valueOf(entry.getValue()));
            }

            try (var writer = Files.newBufferedWriter(playerFile, StandardCharsets.UTF_8)) {
                properties.store(writer, "FitQuest Scientific Core Player Data");
                System.out.println("[存檔系統] 玩家持久化數據同步成功。");
            }
        } catch (IOException ex) {
            System.err.println("[存檔系統] 儲存失敗: " + ex.getMessage());
        }
    }

    public synchronized List<WorkoutEntry> loadWorkouts() {
        List<WorkoutEntry> workouts = new ArrayList<>();
        if (!Files.exists(workoutsFile)) return workouts;

        try {
            for (String line : Files.readAllLines(workoutsFile, StandardCharsets.UTF_8)) {
                if (!line.isBlank()) {
                    workouts.add(WorkoutEntry.fromCsvLine(line));
                }
            }
            System.out.println("[讀檔系統] 成功載入 " + workouts.size() + " 筆歷史運動紀錄。");
        } catch (Exception ex) {
            System.err.println("[讀檔系統] 歷史紀錄載入失敗: " + ex.getMessage());
        }
        return workouts;
    }

    public synchronized void saveWorkouts(List<WorkoutEntry> workouts) {
        try {
            Files.createDirectories(dataDir);
            List<String> lines = workouts.stream().map(WorkoutEntry::toCsvLine).toList();
            Files.write(workoutsFile, lines, StandardCharsets.UTF_8);
            System.out.println("[存檔系統] 歷史運動日誌 CSV 更新成功。");
        } catch (IOException ex) {
            System.err.println("[存檔系統] 歷史日誌更新失敗: " + ex.getMessage());
        }
    }

    private int parseInt(String value, int fallback) {
        try { return Integer.parseInt(value); } catch (Exception e) { return fallback; }
    }

    private double parseDouble(String value, double fallback) {
        try { return Double.parseDouble(value); } catch (Exception e) { return fallback; }
    }
}