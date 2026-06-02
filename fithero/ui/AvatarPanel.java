package fithero.ui;

import fithero.logic.manager.PlayerState;
import fithero.model.exercise.MuscleGroup;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JPanel;

/**
 * 自訂畫布面板：負責首頁虛擬火柴人體態演進繪製與右上角內嵌式戰力雷達圖
 */
public class AvatarPanel extends JPanel {
    private PlayerState player;
    private static final String FONT_FAMILY = "Microsoft JhengHei";
    
    // 定義雷達圖五角對應肌群 (正上方出發，順時針旋轉)
    private final MuscleGroup[] radarMuscles = {
        MuscleGroup.CHEST, // 正上
        MuscleGroup.ARMS,  // 右上
        MuscleGroup.LEGS,  // 右下
        MuscleGroup.ABS,   // 左下
        MuscleGroup.BACK   // 左上
    };

    public AvatarPanel(PlayerState player) {
        this.player = player;
        setPreferredSize(new Dimension(610, 690));
        setBackground(new Color(0x282c37));
    }

    public void setPlayer(PlayerState player) {
        this.player = player;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawBackground(g);
        drawHeader(g);

        // 調整火柴人中心點，稍微往左傾，留出右側空間給雷達圖
        int centerX = getWidth() / 2 - 80;
        int top = 120;
        
        drawBaseBody(g, centerX, top);
        drawMuscles(g, centerX, top);
        drawEmbeddedRadarChart(g);
        drawEmbeddedPlayerStats(g);

        g.dispose();
    }

    private void drawBackground(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, new Color(0x303645), 0, getHeight(), new Color(0x222733)));
        g.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);

        g.setColor(new Color(0x3a4050));
        for (int y = 96; y < getHeight() - 150; y += 48) { 
            g.drawLine(34, y, getWidth() - 34, y);
        }
    }

    private void drawHeader(Graphics2D g) {
        g.setColor(new Color(0xf4f6fb));
        g.setFont(new Font(FONT_FAMILY, Font.BOLD, 30));
        g.drawString("角色體態與特徵分析", 32, 42);

        g.setColor(new Color(0xb7c0d1));
        g.setFont(new Font(FONT_FAMILY, Font.PLAIN, 14));
        g.drawString("用真實訓練打破基因限制，系統自動演進肌群與五維指標。", 34, 67);
    }

    private void drawEmbeddedRadarChart(Graphics2D g) {
        int rcX = getWidth() - 150;
        int rcY = getHeight() / 2 - 40;
        int maxR = 90;

        // 1. 繪製 3 層背景蜘蛛網格
        g.setStroke(new BasicStroke(1f));
        g.setColor(new Color(0x3a4050));
        for (int ring = 1; ring <= 3; ring++) {
            int r = maxR * ring / 3;
            Path2D poly = createPentagonPath(rcX, rcY, r);
            g.draw(poly);
        }

        // 2. 繪製軸線與能力覆蓋圖層
        Path2D playerPath = new Path2D.Double();
        int maxVisualLevel = 20;

        for (int i = 0; i < 5; i++) {
            double angle = Math.toRadians(i * 72 - 90);
            
            int endX = (int) (rcX + maxR * Math.cos(angle));
            int endY = (int) (rcY + maxR * Math.sin(angle));
            g.setColor(new Color(0x3a4050));
            g.drawLine(rcX, rcY, endX, endY);

            MuscleGroup m = radarMuscles[i];
            int lvl = player.muscleLevel(m);
            double ratio = (double) Math.min(maxVisualLevel, lvl) / maxVisualLevel;
            int rValue = (int) (maxR * ratio);

            int pX = (int) (rcX + rValue * Math.cos(angle));
            int pY = (int) (rcY + rValue * Math.sin(angle));

            if (i == 0) playerPath.moveTo(pX, pY);
            else playerPath.lineTo(pX, pY);
        }
        playerPath.closePath();

        g.setColor(new Color(47, 155, 255, 110));
        g.fill(playerPath);
        g.setColor(new Color(0x5aa9ff));
        g.setStroke(new BasicStroke(2f));
        g.draw(playerPath);

        // 3. 渲染五角外側的文字肌群標籤
        g.setFont(new Font(FONT_FAMILY, Font.BOLD, 12));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(new Color(0xf4f6fb));

        for (int i = 0; i < 5; i++) {
            double angle = Math.toRadians(i * 72 - 90);
            MuscleGroup m = radarMuscles[i];
            String txt = m.displayName() + " L" + player.muscleLevel(m);

            int tX = (int) (rcX + (maxR + 18) * Math.cos(angle));
            int tY = (int) (rcY + (maxR + 18) * Math.sin(angle));

            int finalX = tX - fm.stringWidth(txt) / 2;
            int finalY = tY + fm.getAscent() / 2 - 2;
            g.drawString(txt, finalX, finalY);
        }
    }

    private Path2D createPentagonPath(int cx, int cy, int r) {
        Path2D path = new Path2D.Double();
        for (int i = 0; i < 5; i++) {
            double angle = Math.toRadians(i * 72 - 90);
            double x = cx + r * Math.cos(angle);
            double y = cy + r * Math.sin(angle);
            if (i == 0) path.moveTo(x, y);
            else path.lineTo(x, y);
        }
        path.closePath();
        return path;
    }

    private void drawEmbeddedPlayerStats(Graphics2D g) {
        int x = 34;
        int y = getHeight() - 110;
        int cardWidth = getWidth() - 68;
        int cardHeight = 84;

        g.setColor(new Color(0x1a1d24));
        g.fill(new RoundRectangle2D.Double(x, y, cardWidth, cardHeight, 14, 14));
        g.setColor(new Color(0x3a4050));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(new RoundRectangle2D.Double(x, y, cardWidth, cardHeight, 14, 14));

        g.setFont(new Font(FONT_FAMILY, Font.BOLD, 16));
        g.setColor(new Color(0xf4f6fb));
        String titleBadge = player.getAvatar().getCurrentTitle();
        g.drawString(player.getAvatar().getName() + " 【" + titleBadge + "】", x + 18, y + 30);

        String lvText = "等級: Lv." + player.level();
        FontMetrics metrics = g.getFontMetrics();
        g.setColor(new Color(0x5aa9ff)); 
        g.drawString(lvText, x + cardWidth - metrics.stringWidth(lvText) - 18, y + 30);

        int barX = x + 18;
        int barY = y + 46;
        int barWidth = cardWidth - 36;
        int barHeight = 20;

        g.setColor(new Color(0x20242d)); 
        g.fill(new RoundRectangle2D.Double(barX, barY, barWidth, barHeight, 8, 8));

        double ratio = (double) player.xp() / player.xpNeededForNextLevel();
        int progressWidth = (int) (barWidth * Math.min(1.0, ratio));

        if (progressWidth > 0) {
            g.setColor(new Color(0x2f9bff)); 
            g.fill(new RoundRectangle2D.Double(barX, barY, progressWidth, barHeight, 8, 8));
        }

        g.setFont(new Font(FONT_FAMILY, Font.BOLD, 11));
        g.setColor(Color.WHITE);
        String progressStr = "XP: " + player.xp() + " / " + player.xpNeededForNextLevel();
        g.drawString(progressStr, barX + (barWidth - g.getFontMetrics().stringWidth(progressStr)) / 2, barY + 14);
    }

    private void drawBaseBody(Graphics2D g, int x, int y) {
        g.setColor(new Color(0xdde4f0));
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        g.draw(new Ellipse2D.Double(x - 34, y, 68, 68));
        g.draw(new Line2D.Double(x, y + 68, x, y + 230));
        g.draw(new Line2D.Double(x - 90, y + 110, x + 90, y + 110));
        g.draw(new Line2D.Double(x - 68, y + 118, x - 110, y + 250));
        g.draw(new Line2D.Double(x + 68, y + 118, x + 110, y + 250));
        g.draw(new Line2D.Double(x, y + 230, x - 70, y + 400));
        g.draw(new Line2D.Double(x, y + 230, x + 70, y + 400));

        g.setColor(new Color(0x7f8ca3));
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(x - 64, y + 85, x + 64, y + 85));
        g.draw(new Line2D.Double(x - 48, y + 210, x + 48, y + 210));
    }

    private void drawMuscles(Graphics2D g, int x, int y) {
        for (MuscleGroup group : MuscleGroup.values()) {
            int level = player.muscleLevel(group);
            g.setColor(group.color());
            g.setStroke(strokeFor(level));
            
            // 根據肌群列舉動態分流繪製
            switch (group) {
                case CHEST -> {
                    g.drawArc(x - 66, y + 96, 66 + level * 4, 66 + level * 3, 8, 166);
                    g.drawArc(x - level * 4, y + 96, 66 + level * 4, 66 + level * 3, 6, 166);
                }
                case ARMS -> {
                    g.drawOval(x - 130 - level * 2, y + 140, 44 + level * 5, 80 + level * 5);
                    g.drawOval(x + 86 - level * 2, y + 140, 44 + level * 5, 80 + level * 5);
                }
                case ABS -> {
                    int width = 32 + level * 3; int height = 22 + level * 2;
                    for (int row = 0; row < 3; row++) {
                        g.drawRoundRect(x - width - 4, y + 150 + row * 30, width, height, 10, 10);
                        g.drawRoundRect(x + 4, y + 150 + row * 30, width, height, 10, 10);
                    }
                }
                case LEGS -> {
                    g.drawOval(x - 86 - level * 2, y + 270, 50 + level * 6, 120 + level * 5);
                    g.drawOval(x + 36 - level * 2, y + 270, 50 + level * 6, 120 + level * 5);
                }
                case BACK -> {
                    g.drawArc(x - 90 - level * 2, y + 85, 86 + level * 4, 150 + level * 5, 82, 120);
                    g.drawArc(x + 4 - level * 2, y + 85, 86 + level * 4, 150 + level * 5, -22, 120);
                }
            }
        }
    }

    private BasicStroke strokeFor(int level) {
        float width = Math.min(18f, 3.0f + level * 1.5f);
        return new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }
}