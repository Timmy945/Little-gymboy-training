package fithero.ui;

import fithero.model.workout.WorkoutEntry;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import javax.swing.JPanel;

/**
 * 統計趨勢長條圖組件：完整支援週、月、年三種規模區間的 XP 獲取動態分析
 */
public class WorkoutBarChartPanel extends JPanel {
    private static final Color PANEL_BG = new Color(0x20242d);
    private static final Color GRID = new Color(0x343b4a);
    private static final Color BAR = new Color(0x59667a);
    private static final Color ACTIVE_BAR = new Color(0x2f9bff);
    private static final Color TEXT = new Color(0xf4f6fb);
    private static final Color MUTED = new Color(0xb7c0d1);
    private static final String FONT_FAMILY = "Microsoft JhengHei";

    private final double[] chartData = new double[12]; 
    private final String[] chartLabels = new String[12];
    private double averageValue = 0;
    private String currentScale = "WEEK"; 

    public WorkoutBarChartPanel() {
        setBackground(PANEL_BG);
        setPreferredSize(new Dimension(610, 280));
    }

    public void setScaleMode(String scaleMode, List<WorkoutEntry> workouts) {
        this.currentScale = scaleMode;
        LocalDate today = LocalDate.now();
        double total = 0;

        for (int i = 0; i < 12; i++) {
            chartData[i] = 0;
            chartLabels[i] = "";
        }

        if (scaleMode.equals("WEEK")) {
            String[] weekNames = {"日", "一", "二", "三", "四", "五", "六"};
            int currentDayVal = today.getDayOfWeek().getValue();
            LocalDate sundayOfThisWeek = (currentDayVal == 7) ? today : today.minusDays(currentDayVal);
            
            for (int i = 0; i < 7; i++) {
                LocalDate targetDate = sundayOfThisWeek.plusDays(i);
                chartLabels[i] = weekNames[i];
                chartData[i] = sumXpForDateRange(targetDate, targetDate, workouts);
                total += chartData[i];
            }
            averageValue = total / 7.0;

        } else if (scaleMode.equals("MONTH")) {
            chartLabels[0] = "1-6日";
            chartLabels[1] = "7-12日";
            chartLabels[2] = "13-18日";
            chartLabels[3] = "19-24日";
            
            YearMonth yearMonth = YearMonth.of(today.getYear(), today.getMonthValue());
            int lastDay = yearMonth.lengthOfMonth();
            chartLabels[4] = "25-" + lastDay + "日";

            chartData[0] = sumXpForDateRange(today.withDayOfMonth(1), today.withDayOfMonth(6), workouts);
            chartData[1] = sumXpForDateRange(today.withDayOfMonth(7), today.withDayOfMonth(12), workouts);
            chartData[2] = sumXpForDateRange(today.withDayOfMonth(13), today.withDayOfMonth(18), workouts);
            chartData[3] = sumXpForDateRange(today.withDayOfMonth(19), today.withDayOfMonth(24), workouts);
            chartData[4] = sumXpForDateRange(today.withDayOfMonth(25), today.withDayOfMonth(lastDay), workouts);

            for (int i = 0; i < 5; i++) total += chartData[i];
            averageValue = total / 5.0;

        } else if (scaleMode.equals("YEAR")) {
            String[] monthNames = {"一月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "十二月"};
            for (int i = 0; i < 12; i++) {
                chartLabels[i] = monthNames[i];
                YearMonth ym = YearMonth.of(today.getYear(), i + 1);
                chartData[i] = sumXpForDateRange(ym.atDay(1), ym.atEndOfMonth(), workouts);
                total += chartData[i];
            }
            averageValue = total / 12.0;
        }
        repaint();
    }

    private double sumXpForDateRange(LocalDate start, LocalDate end, List<WorkoutEntry> workouts) {
        double sum = 0;
        for (WorkoutEntry entry : workouts) {
            LocalDate d = entry.time().toLocalDate();
            if ((d.isAfter(start) || d.isEqual(start)) && (d.isBefore(end) || d.isEqual(end))) {
                sum += entry.xp(); 
            }
        }
        return sum;
    }

    public void updateChartData(List<WorkoutEntry> workouts) {
        setScaleMode(this.currentScale, workouts);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth(); int height = getHeight();
        int left = 46; int right = 28; int top = 24; int bottom = 42;
        int chartWidth = width - left - right;
        int chartHeight = height - top - bottom;
        int baseline = top + chartHeight;

        int activeBarCount = currentScale.equals("WEEK") ? 7 : (currentScale.equals("MONTH") ? 5 : 12);
        
        double max = 100;
        for (int i = 0; i < activeBarCount; i++) {
            if (chartData[i] > max) max = chartData[i];
        }
        max = max * 1.15;

        g.setColor(PANEL_BG);
        g.fillRoundRect(0, 0, width, height, 12, 12);
        g.setColor(GRID);
        g.setStroke(new BasicStroke(1f));
        for (int i = 0; i <= 3; i++) {
            int y = top + (baseline - top) * i / 3;
            g.drawLine(left, y, width - right, y);
        }
        
        int slot = chartWidth / activeBarCount;
        int barWidth = Math.max(16, Math.min(54, (int) (slot * 0.6)));
        
        // 修正點：全域更換字型為強型別中文字型
        g.setFont(new Font(FONT_FAMILY, Font.PLAIN, 11));
        FontMetrics metrics = g.getFontMetrics();

        int highlightIndex = activeBarCount - 1;
        if (currentScale.equals("YEAR")) {
            highlightIndex = LocalDate.now().getMonthValue() - 1;
        }

        for (int i = 0; i < activeBarCount; i++) {
            int barHeight = (int) (chartData[i] * chartHeight / max);
            int x = left + slot * i + (slot - barWidth) / 2;
            int y = baseline - barHeight;

            g.setColor(i == highlightIndex ? ACTIVE_BAR : BAR);
            g.fill(new RoundRectangle2D.Double(x, y, barWidth, barHeight, 6, 6));

            g.setColor(MUTED);
            int labelX = x + (barWidth - metrics.stringWidth(chartLabels[i])) / 2;
            g.drawString(chartLabels[i], labelX, baseline + 20);
        }
        
        int yLine = baseline - (int) (averageValue * chartHeight / max);
        int endX = width - right;
        g.setColor(new Color(0x9dcfff));
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[] {5f, 5f}, 0));
        g.drawLine(left, yLine, endX, yLine);

        g.setStroke(new BasicStroke(1f));
        String avgLabel = "平均刺激: " + String.format("%.0f", averageValue) + " XP";
        int labelWidth = metrics.stringWidth(avgLabel) + 14;
        int labelHeight = 22;
        int lx = Math.max(left, endX - labelWidth - 4);
        int ly = Math.max(top + 2, yLine - labelHeight - 4);

        g.setColor(new Color(0x263b55));
        g.fillRoundRect(lx, ly, labelWidth, labelHeight, 8, 8);
        g.setColor(new Color(0x9dcfff));
        g.drawRoundRect(lx, ly, labelWidth, labelHeight, 8, 8);
        g.setColor(TEXT);
        g.drawString(avgLabel, lx + 7, ly + 15);

        g.dispose();
    }
}