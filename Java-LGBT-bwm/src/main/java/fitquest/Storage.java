package fitquest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Storage {
    private final Path dataDir;
    private final Path playerFile;
    private final Path workoutsFile;

    public Storage(Path dataDir) {
        this.dataDir = dataDir;
        this.playerFile = dataDir.resolve("player.properties");
        this.workoutsFile = dataDir.resolve("workouts.csv");
    }

    public PlayerState loadPlayer() {
        PlayerState player = new PlayerState();
        if (!Files.exists(playerFile)) {
            return player;
        }

        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(playerFile, StandardCharsets.UTF_8)) {
            properties.load(reader);
            player.setLevel(parseInt(properties.getProperty("level"), 1));
            player.setXp(parseInt(properties.getProperty("xp"), 0));
            player.setUpgradePoints(parseInt(properties.getProperty("upgradePoints"), 0));
            for (MuscleGroup muscle : MuscleGroup.values()) {
                String key = "muscle." + muscle.name();
                player.setMuscleLevel(muscle, parseInt(properties.getProperty(key), 1));
            }
        } catch (IOException ex) {
            System.err.println("Could not load player data: " + ex.getMessage());
        }
        return player;
    }

    public void savePlayer(PlayerState player) {
        try {
            Files.createDirectories(dataDir);
            Properties properties = new Properties();
            properties.setProperty("level", String.valueOf(player.level()));
            properties.setProperty("xp", String.valueOf(player.xp()));
            properties.setProperty("upgradePoints", String.valueOf(player.upgradePoints()));
            for (MuscleGroup muscle : MuscleGroup.values()) {
                properties.setProperty("muscle." + muscle.name(), String.valueOf(player.muscleLevel(muscle)));
            }
            try (var writer = Files.newBufferedWriter(playerFile, StandardCharsets.UTF_8)) {
                properties.store(writer, "FitQuest player data");
            }
        } catch (IOException ex) {
            System.err.println("Could not save player data: " + ex.getMessage());
        }
    }

    public List<WorkoutEntry> loadWorkouts() {
        List<WorkoutEntry> workouts = new ArrayList<>();
        if (!Files.exists(workoutsFile)) {
            return workouts;
        }

        try {
            for (String line : Files.readAllLines(workoutsFile, StandardCharsets.UTF_8)) {
                if (!line.isBlank()) {
                    workouts.add(WorkoutEntry.fromCsvLine(line));
                }
            }
        } catch (RuntimeException | IOException ex) {
            System.err.println("Could not load workout data: " + ex.getMessage());
        }
        return workouts;
    }

    public void saveWorkouts(List<WorkoutEntry> workouts) {
        try {
            Files.createDirectories(dataDir);
            List<String> lines = workouts.stream().map(WorkoutEntry::toCsvLine).toList();
            Files.write(workoutsFile, lines, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            System.err.println("Could not save workout data: " + ex.getMessage());
        }
    }

    private int parseInt(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
