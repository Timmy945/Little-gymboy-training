package fithero.model.workout;

import fithero.model.exercise.WorkoutType;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 歷史運動完訓日誌實體（含精密 CSV 互轉引擎）
 */
public class WorkoutEntry {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final LocalDateTime time;
    private final String exerciseName;
    private final double weight; 
    private final int amount;    
    private final int sets;      
    private final int xp;        

    public WorkoutEntry(LocalDateTime time, String exerciseName, double weight, int amount, int sets, int xp) {
        this.time = time;
        // 防禦機制：強制過濾字串中的逗號，防止 CSV 格式遭受注入破壞
        this.exerciseName = exerciseName != null ? exerciseName.replace(",", " ") : "未知運動";
        this.weight = weight;
        this.amount = amount;
        this.sets = sets;
        this.xp = xp;
    }

    public WorkoutEntry(LocalDateTime time, String exerciseName, int totalMinutes, int xp) {
        this(time, exerciseName, 0.0, totalMinutes, 1, xp);
    }

    public LocalDateTime time() { return time; }
    public String getExerciseName() { return exerciseName; }
    public double weight() { return weight; }
    public int amount() { return amount; }
    public int sets() { return sets; }
    public int xp() { return xp; }
    public String displayTime() { return FORMATTER.format(time); }

    public WorkoutType type() {
        return WorkoutType.fromDisplayName(exerciseName);
    }

    public String toCsvLine() {
        return time + "," + exerciseName + "," + weight + "," + amount + "," + sets + "," + xp;
    }

    public static WorkoutEntry fromCsvLine(String line) {
        if (line == null || line.isBlank()) {
            throw new IllegalArgumentException("無法解構空白日誌");
        }
        String[] parts = line.split(",", -1);
        if (parts.length != 6) {
            if (parts.length == 4) { // 降級相容相容
                LocalDateTime time = LocalDateTime.parse(parts[0]);
                String name = parts[1];
                int amt = Integer.parseInt(parts[2]);
                int xp = Integer.parseInt(parts[3]);
                return new WorkoutEntry(time, name, 0.0, amt, 1, xp);
            }
            throw new IllegalArgumentException("存檔日誌格式損毀: " + line);
        }
        
        LocalDateTime time = LocalDateTime.parse(parts[0]);
        String name = parts[1];
        double weight = Double.parseDouble(parts[2]);
        int amount = Integer.parseInt(parts[3]);
        int sets = Integer.parseInt(parts[4]);
        int xp = Integer.parseInt(parts[5]);

        return new WorkoutEntry(time, name, weight, amount, sets, xp);
    }
}