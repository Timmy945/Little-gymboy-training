package fitquest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class WorkoutEntry {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final LocalDateTime time;
    private final WorkoutType type;
    private final int amount;
    private final int xp;

    public WorkoutEntry(LocalDateTime time, WorkoutType type, int amount, int xp) {
        this.time = time;
        this.type = type;
        this.amount = amount;
        this.xp = xp;
    }

    public LocalDateTime time() {
        return time;
    }

    public WorkoutType type() {
        return type;
    }

    public int amount() {
        return amount;
    }

    public int xp() {
        return xp;
    }

    public String displayTime() {
        return FORMATTER.format(time);
    }

    public String toCsvLine() {
        return time + "," + type.name() + "," + amount + "," + xp;
    }

    public static WorkoutEntry fromCsvLine(String line) {
        String[] parts = line.split(",", -1);
        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid workout row: " + line);
        }
        LocalDateTime time = LocalDateTime.parse(parts[0]);
        WorkoutType type = WorkoutType.valueOf(parts[1]);
        int amount = Integer.parseInt(parts[2]);
        int xp = Integer.parseInt(parts[3]);
        return new WorkoutEntry(time, type, amount, xp);
    }
}
