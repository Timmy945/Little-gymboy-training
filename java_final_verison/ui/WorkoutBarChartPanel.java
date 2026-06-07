package fithero.ui;

import fithero.model.workout.WorkoutEntry;
import fithero.model.exercise.ExerciseInfo;
import fithero.model.exercise.ExerciseRegistry;
import fithero.model.exercise.MuscleGroup;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * 訓練量趨勢圖：顯示 6 類訓練堆疊、總量折線、平均線與週／月／年目標線
 */
public class WorkoutBarChartPanel extends JPanel {
    private static final Color PANEL_BG = new Color(0x20242d);
    private static final Color GRID = new Color(0x343b4a);
    private static final Color TEXT = new Color(0xf4f6fb);
    private static final Color MUTED = new Color(0xb7c0d1);
    private static final String FONT_FAMILY = "Microsoft JhengHei";

    // 運動科學六大分類色彩配置
    public static final Color COLOR_CHEST   = new Color(0xef4444); // 紅色：胸肌
    public static final Color COLOR_BACK    = new Color(0xf97316); // 橙色：背肌
    public static final Color COLOR_LEGS    = new Color(0xeab308); // 黃色：腿部
    public static final Color COLOR_ARMS    = new Color(0x22c55e); // 綠色：手臂
    public static final Color COLOR_ABS     = new Color(0x3b82f6); // 藍色：腹部
    public static final Color COLOR_AEROBIC = new Color(0xa855f7); // 紫色：有氧

    // 折線圖、目標線與達標色彩
    public static final Color LINE_COLOR = new Color(0x38bdf8);
    public static final Color TARGET_COLOR = new Color(0x10b981);
    public static final Color DANGER_COLOR = new Color(0xef4444);

    // 二維矩陣儲存數據：[12個時間軸][6個分類區間]
    private final double[][] stackedChartData = new double[12][6]; 
    private final double[] totalChartData = new double[12]; // 儲存當期總高度
    private final String[] chartLabels = new String[12];
    private double averageValue = 0;
    private double dailyTarget = 1000.0;
    private double dailyDangerThreshold = 3000.0;
    private boolean hasEnoughData = false;
    private String currentScale = "WEEK"; 

    public WorkoutBarChartPanel() {
        setBackground(PANEL_BG);
        setPreferredSize(new Dimension(610, 280));
    }

    public void setDailyTarget(double dailyTarget) {
        this.dailyTarget = Math.max(1.0, dailyTarget);
    }

    public void setDailyDangerThreshold(double dailyDangerThreshold) {
        this.dailyDangerThreshold = Math.max(dailyTarget + 1.0, dailyDangerThreshold);
    }

    public JPanel createLegendPanel() {
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        legend.setOpaque(false);
        legend.add(createLegendItem("胸", COLOR_CHEST));
        legend.add(createLegendItem("背", COLOR_BACK));
        legend.add(createLegendItem("腿", COLOR_LEGS));
        legend.add(createLegendItem("手臂", COLOR_ARMS));
        legend.add(createLegendItem("腹部", COLOR_ABS));
        legend.add(createLegendItem("有氧", COLOR_AEROBIC));
        legend.add(createLegendItem("危險值", DANGER_COLOR));
        return legend;
    }

    private JLabel createLegendItem(String text, Color color) {
        JLabel item = new JLabel("● " + text);
        item.setFont(new Font(FONT_FAMILY, Font.BOLD, 11));
        item.setForeground(color);
        item.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 2));
        return item;
    }

    public void setScaleMode(String scaleMode, List<WorkoutEntry> workouts) {
        this.currentScale = scaleMode;
        LocalDate today = LocalDate.now();
        double totalVolumeSum = 0;
        hasEnoughData = workouts != null
                && workouts.stream().map(entry -> entry.time().toLocalDate()).distinct().limit(2).count() >= 2;

        // 初始化資料庫
        for (int i = 0; i < 12; i++) {
            totalChartData[i] = 0;
            chartLabels[i] = "";
            for (int j = 0; j < 6; j++) stackedChartData[i][j] = 0;
        }

        int activeBarCount = scaleMode.equals("WEEK") ? 7 : (scaleMode.equals("MONTH") ? 5 : 12);

        if (scaleMode.equals("WEEK")) {
            String[] weekNames = {"日", "一", "二", "三", "四", "五", "六"};
            int currentDayVal = today.getDayOfWeek().getValue();
            LocalDate sundayOfThisWeek = (currentDayVal == 7) ? today : today.minusDays(currentDayVal);
            
            for (int i = 0; i < 7; i++) {
                LocalDate targetDate = sundayOfThisWeek.plusDays(i);
                chartLabels[i] = weekNames[i];
                populateStackedData(i, targetDate, targetDate, workouts);
            }
        } else if (scaleMode.equals("MONTH")) {
            chartLabels[0] = "1-6日";   chartLabels[1] = "7-12日";  chartLabels[2] = "13-18日";
            chartLabels[3] = "19-24日";  chartLabels[4] = "25-30日";
            
            YearMonth yearMonth = YearMonth.of(today.getYear(), today.getMonthValue());
            int lastDay = yearMonth.lengthOfMonth();
            chartLabels[4] = "25-" + lastDay + "日";

            populateStackedData(0, today.withDayOfMonth(1), today.withDayOfMonth(6), workouts);
            populateStackedData(1, today.withDayOfMonth(7), today.withDayOfMonth(12), workouts);
            populateStackedData(2, today.withDayOfMonth(13), today.withDayOfMonth(18), workouts);
            populateStackedData(3, today.withDayOfMonth(19), today.withDayOfMonth(24), workouts);
            populateStackedData(4, today.withDayOfMonth(25), today.withDayOfMonth(lastDay), workouts);
        } else if (scaleMode.equals("YEAR")) {
            String[] monthNames = {"一月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "十二月"};
            for (int i = 0; i < 12; i++) {
                chartLabels[i] = monthNames[i];
                YearMonth ym = YearMonth.of(today.getYear(), i + 1);
                populateStackedData(i, ym.atDay(1), ym.atEndOfMonth(), workouts);
            }
        }

        // 計算各柱總和與全域平均值
        for (int i = 0; i < activeBarCount; i++) {
            double barSum = 0;
            for (int j = 0; j < 6; j++) {
                barSum += stackedChartData[i][j];
            }
            totalChartData[i] = barSum;
            totalVolumeSum += barSum;
        }
        averageValue = totalVolumeSum / (double) activeBarCount;
        repaint();
    }

    private void populateStackedData(int slotIndex, LocalDate start, LocalDate end, List<WorkoutEntry> workouts) {
        if (workouts == null) return;
        for (WorkoutEntry entry : workouts) {
            LocalDate d = entry.time().toLocalDate();
            if ((d.isAfter(start) || d.isEqual(start)) && (d.isBefore(end) || d.isEqual(end))) {
                double volume = entry.trainingVolume();
                ExerciseInfo info = ExerciseRegistry.getExercise(entry.getExerciseName());
                
                if (info == null) {
                    stackedChartData[slotIndex][5] += volume;
                    continue;
                }

                if (info.isAerobic()) {
                    stackedChartData[slotIndex][5] += volume;
                } else {
                    MuscleGroup group = info.getTargetMuscle();
                    if (group == null) {
                        stackedChartData[slotIndex][4] += volume;
                        continue;
                    }
                    switch (group) {
                        case CHEST -> stackedChartData[slotIndex][0] += volume;
                        case BACK  -> stackedChartData[slotIndex][1] += volume;
                        case LEGS  -> stackedChartData[slotIndex][2] += volume;
                        case ARMS  -> stackedChartData[slotIndex][3] += volume;
                        case ABS   -> stackedChartData[slotIndex][4] += volume;
                        default    -> stackedChartData[slotIndex][4] += volume;
                    }
                }
            }
        }
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
        int left = 46; int right = 28; int top = 48; int bottom = 42;
        int chartWidth = width - left - right;
        int chartHeight = height - top - bottom;
        int baseline = top + chartHeight;

        int activeBarCount = currentScale.equals("WEEK") ? 7 : (currentScale.equals("MONTH") ? 5 : 12);
        
        double currentScaleTarget;
        double currentScaleDanger;
        String targetLineLabel;
        String dangerLineLabel;

        if (currentScale.equals("WEEK")) {
            currentScaleTarget = dailyTarget;
            currentScaleDanger = dailyDangerThreshold;
            targetLineLabel = "今日目標線";
            dangerLineLabel = "危險值";
        } else if (currentScale.equals("MONTH")) {
            currentScaleTarget = dailyTarget * 6.0;
            currentScaleDanger = dailyDangerThreshold * 6.0;
            targetLineLabel = "區間目標線";
            dangerLineLabel = "區間危險值";
        } else { // YEAR
            currentScaleTarget = dailyTarget * 30.0;
            currentScaleDanger = dailyDangerThreshold * 30.0;
            targetLineLabel = "每月目標線";
            dangerLineLabel = "每月危險值";
        }

        // 尋找最大上限值以動態縮放 Y 軸，防止柱子衝出螢幕
        double max = 300; 
        for (int i = 0; i < activeBarCount; i++) {
            if (totalChartData[i] > max) max = totalChartData[i];
        }
        if (max < currentScaleDanger) {
            max = currentScaleDanger * 1.1;
        } else {
            max = max * 1.15;
        }

        // 背景
        g.setColor(PANEL_BG);
        g.fillRoundRect(0, 0, width, height, 12, 12);

        if (!hasEnoughData) {
            g.setColor(MUTED);
            g.setFont(new Font(FONT_FAMILY, Font.PLAIN, 11));
            g.drawString("目前訓練資料較少，累積更多紀錄後會產生完整趨勢。", left, 24);
        }

        // 繪製背景網格線
        g.setColor(GRID);
        g.setStroke(new BasicStroke(1f));
        for (int i = 0; i <= 3; i++) {
            int y = top + (baseline - top) * i / 3;
            g.drawLine(left, y, width - right, y);
        }

        // 目標線會依週、月、年尺度換算。
        int yAlertLine = baseline - (int) (currentScaleTarget * chartHeight / max);
        g.setColor(TARGET_COLOR);
        g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[] {4f, 6f}, 0));
        g.drawLine(left, yAlertLine, width - right, yAlertLine);

        int yDangerLine = baseline - (int) (currentScaleDanger * chartHeight / max);
        g.setColor(DANGER_COLOR);
        g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[] {3f, 5f}, 0));
        g.drawLine(left, yDangerLine, width - right, yDangerLine);
        
        // 長條圖寬度計算
        int slot = chartWidth / activeBarCount;
        int barWidth = Math.max(16, Math.min(48, (int) (slot * 0.55)));
        
        g.setFont(new Font(FONT_FAMILY, Font.PLAIN, 11));
        FontMetrics metrics = g.getFontMetrics();

        int[] linePointsX = new int[activeBarCount];
        int[] linePointsY = new int[activeBarCount];

        // 第一階段：依序繪製 6 層堆疊長條圖
        Color[] layerColors = {COLOR_CHEST, COLOR_BACK, COLOR_LEGS, COLOR_ARMS, COLOR_ABS, COLOR_AEROBIC};
        
        for (int i = 0; i < activeBarCount; i++) {
            int currentBottomY = baseline;
            int x = left + slot * i + (slot - barWidth) / 2;

            linePointsX[i] = x + barWidth / 2;

            if (totalChartData[i] == 0) {
                g.setColor(GRID);
                g.fillRect(x, baseline - 2, barWidth, 2);
            } else {
                for (int j = 0; j < 6; j++) {
                    double val = stackedChartData[i][j];
                    if (val <= 0) continue;

                    int layerHeight = (int) (val * chartHeight / max);
                    if (layerHeight < 2) layerHeight = 2;

                    int y = currentBottomY - layerHeight;
                    g.setColor(layerColors[j]);
                    g.fillRect(x, y, barWidth, layerHeight);

                    currentBottomY = y;
                }
            }

            if (totalChartData[i] >= currentScaleDanger) {
                g.setColor(DANGER_COLOR);
                g.setFont(new Font(FONT_FAMILY, Font.BOLD, 11));
                g.drawString("危險", x + (barWidth / 2) - 8, currentBottomY - 6);
            } else if (totalChartData[i] >= currentScaleTarget) {
                g.setColor(TARGET_COLOR);
                g.setFont(new Font(FONT_FAMILY, Font.BOLD, 11));
                g.drawString("達標", x + (barWidth / 2) - 8, currentBottomY - 6);
            }

            g.setColor(MUTED);
            g.setFont(new Font(FONT_FAMILY, Font.PLAIN, 11));
            int labelX = x + (barWidth - metrics.stringWidth(chartLabels[i])) / 2;
            g.drawString(chartLabels[i], labelX, baseline + 20);

            linePointsY[i] = baseline - (int) (totalChartData[i] * chartHeight / max);
        }

        // 第二階段：重疊總高度折線圖
        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(LINE_COLOR);
        g.drawPolyline(linePointsX, linePointsY, activeBarCount);

        for (int i = 0; i < activeBarCount; i++) {
            g.setColor(PANEL_BG);
            g.fillOval(linePointsX[i] - 5, linePointsY[i] - 5, 10, 10);
            g.setColor(totalChartData[i] >= currentScaleDanger
                    ? DANGER_COLOR
                    : (totalChartData[i] >= currentScaleTarget ? TARGET_COLOR : LINE_COLOR));
            g.setStroke(new BasicStroke(2f));
            g.drawOval(linePointsX[i] - 5, linePointsY[i] - 5, 10, 10);
        }

        // 第三階段：繪製「平均刺激量」半透明平準線
        int yLine = baseline - (int) (averageValue * chartHeight / max);
        int endX = width - right;
        g.setColor(new Color(157, 207, 255, 160)); 
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[] {6f, 6f}, 0));
        g.drawLine(left, yLine, endX, yLine);

        // 渲染右側平均值狀態看板
        String avgLabel = "平均訓練量: " + String.format("%.0f", averageValue);
        g.setFont(new Font(FONT_FAMILY, Font.BOLD, 11));
        FontMetrics boldMetrics = g.getFontMetrics();
        int labelWidth = boldMetrics.stringWidth(avgLabel) + 14;
        int labelHeight = 22;
        int lx = Math.max(left, endX - labelWidth - 4);
        int ly = Math.max(top - 10, yLine - labelHeight - 4);

        g.setColor(new Color(0x263b55));
        g.fillRoundRect(lx, ly, labelWidth, labelHeight, 8, 8);
        
        g.setColor(averageValue >= currentScaleDanger
                ? DANGER_COLOR
                : (averageValue >= currentScaleTarget ? TARGET_COLOR : new Color(0x9dcfff)));
        g.drawRoundRect(lx, ly, labelWidth, labelHeight, 8, 8);
        g.setColor(TEXT);
        g.drawString(avgLabel, lx + 7, ly + 15);

        // 線條標籤最後繪製，避免被堆疊柱狀圖蓋住。
        drawThresholdLabel(g, targetLineLabel + " (" + (int) currentScaleTarget + ")", left + 4, yAlertLine, TARGET_COLOR);
        String dangerLabel = dangerLineLabel + " (" + (int) currentScaleDanger + ")";
        g.setFont(new Font(FONT_FAMILY, Font.BOLD, 10));
        int dangerWidth = g.getFontMetrics().stringWidth(dangerLabel) + 12;
        drawThresholdLabel(g, dangerLabel, width - right - dangerWidth - 4, yDangerLine - 35, DANGER_COLOR);

        g.dispose();
    }

    private void drawThresholdLabel(Graphics2D g, String text, int x, int lineY, Color color) {
        g.setFont(new Font(FONT_FAMILY, Font.BOLD, 10));
        FontMetrics metrics = g.getFontMetrics();
        int labelWidth = metrics.stringWidth(text) + 12;
        int labelHeight = 17;
        int y = Math.max(3, Math.min(getHeight() - labelHeight - 3, lineY - labelHeight - 2));

        g.setColor(PANEL_BG);
        g.fillRoundRect(x, y, labelWidth, labelHeight, 7, 7);
        g.setColor(color);
        g.drawRoundRect(x, y, labelWidth, labelHeight, 7, 7);
        g.drawString(text, x + 6, y + 12);
    }
}
