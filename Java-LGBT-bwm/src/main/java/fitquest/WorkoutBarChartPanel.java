package fitquest;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JPanel;

public class WorkoutBarChartPanel extends JPanel {
    private static final Color PANEL_BG = new Color(0x20242d);
    private static final Color GRID = new Color(0x343b4a);
    private static final Color BAR = new Color(0x59667a);
    private static final Color ACTIVE_BAR = new Color(0x2f9bff);
    private static final Color TEXT = new Color(0xf4f6fb);
    private static final Color MUTED = new Color(0xb7c0d1);

    private final int[] minutes = {44, 57, 38, 62, 49, 53, 61};
    private final String[] labels = {"一", "二", "三", "四", "五", "六", "日"};
    private final int average = 52;

    public WorkoutBarChartPanel() {
        setBackground(PANEL_BG);
        setPreferredSize(new Dimension(610, 280));
        setMinimumSize(new Dimension(520, 250));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int left = 46;
        int right = 28;
        int top = 24;
        int bottom = 42;
        int chartWidth = width - left - right;
        int chartHeight = height - top - bottom;
        int baseline = top + chartHeight;
        int max = 70;

        drawPanel(g, width, height);
        drawGrid(g, left, right, top, baseline, width);
        drawBars(g, left, top, chartWidth, chartHeight, baseline, max);
        drawAverageLine(g, left, chartWidth, top, chartHeight, baseline, max);

        g.dispose();
    }

    private void drawPanel(Graphics2D g, int width, int height) {
        g.setColor(PANEL_BG);
        g.fillRoundRect(0, 0, width, height, 12, 12);
    }

    private void drawGrid(Graphics2D g, int left, int right, int top, int baseline, int width) {
        g.setColor(GRID);
        g.setStroke(new BasicStroke(1f));
        for (int i = 0; i <= 3; i++) {
            int y = top + (baseline - top) * i / 3;
            g.drawLine(left, y, width - right, y);
        }
    }

    private void drawBars(Graphics2D g, int left, int top, int chartWidth, int chartHeight, int baseline, int max) {
        int slot = chartWidth / minutes.length;
        int barWidth = Math.max(34, Math.min(58, (int) (slot * 0.58)));
        g.setFont(new Font("Dialog", Font.PLAIN, 13));
        FontMetrics metrics = g.getFontMetrics();

        for (int i = 0; i < minutes.length; i++) {
            int barHeight = minutes[i] * chartHeight / max;
            int x = left + slot * i + (slot - barWidth) / 2;
            int y = baseline - barHeight;

            g.setColor(i == minutes.length - 1 ? ACTIVE_BAR : BAR);
            g.fill(new RoundRectangle2D.Double(x, y, barWidth, barHeight, 12, 12));

            g.setColor(MUTED);
            int labelX = x + (barWidth - metrics.stringWidth(labels[i])) / 2;
            g.drawString(labels[i], labelX, baseline + 24);
        }
    }

    private void drawAverageLine(Graphics2D g, int left, int chartWidth, int top, int chartHeight, int baseline, int max) {
        int y = baseline - average * chartHeight / max;
        int endX = left + chartWidth;

        g.setColor(new Color(0x9dcfff));
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[] {8f, 7f}, 0));
        g.drawLine(left, y, endX, y);

        g.setStroke(new BasicStroke(1f));
        g.setFont(new Font("Dialog", Font.BOLD, 13));
        String label = "平均 52 分鐘";
        FontMetrics metrics = g.getFontMetrics();
        int labelWidth = metrics.stringWidth(label) + 18;
        int labelHeight = 28;
        int x = Math.max(left, endX - labelWidth);
        int labelY = Math.max(top + 4, y - labelHeight - 8);

        g.setColor(new Color(0x263b55));
        g.fillRoundRect(x, labelY, labelWidth, labelHeight, 14, 14);
        g.setColor(new Color(0x9dcfff));
        g.drawRoundRect(x, labelY, labelWidth, labelHeight, 14, 14);
        g.setColor(TEXT);
        g.drawString(label, x + 9, labelY + 19);
    }
}
