package fitquest;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import javax.swing.JPanel;

public class AvatarPanel extends JPanel {
    private PlayerState player;

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

        int centerX = getWidth() / 2;
        int top = 108;
        drawBaseBody(g, centerX, top);
        drawMuscles(g, centerX, top);
        drawLegend(g);

        g.dispose();
    }

    private void drawBackground(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, new Color(0x303645), 0, getHeight(), new Color(0x222733)));
        g.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);

        g.setColor(new Color(0x3a4050));
        for (int y = 96; y < getHeight() - 40; y += 48) {
            g.drawLine(34, y, getWidth() - 34, y);
        }
    }

    private void drawHeader(Graphics2D g) {
        g.setColor(new Color(0xf4f6fb));
        g.setFont(new Font("Dialog", Font.BOLD, 30));
        g.drawString("角色體態", 32, 42);

        g.setColor(new Color(0xb7c0d1));
        g.setFont(new Font("Dialog", Font.PLAIN, 15));
        g.drawString("用訓練換 XP，再把升級點投入肌群。", 34, 67);

        g.setColor(new Color(0x5aa9ff));
        g.setFont(new Font("Dialog", Font.BOLD, 18));
        g.drawString("Lv." + player.level(), getWidth() - 92, 42);
    }

    private void drawBaseBody(Graphics2D g, int x, int y) {
        g.setColor(new Color(0xdde4f0));
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        g.draw(new Ellipse2D.Double(x - 42, y, 84, 84));
        g.draw(new Line2D.Double(x, y + 84, x, y + 272));
        g.draw(new Line2D.Double(x - 112, y + 132, x + 112, y + 132));
        g.draw(new Line2D.Double(x - 86, y + 142, x - 142, y + 300));
        g.draw(new Line2D.Double(x + 86, y + 142, x + 142, y + 300));
        g.draw(new Line2D.Double(x, y + 272, x - 88, y + 480));
        g.draw(new Line2D.Double(x, y + 272, x + 88, y + 480));

        g.setColor(new Color(0x7f8ca3));
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(x - 78, y + 100, x + 78, y + 100));
        g.draw(new Line2D.Double(x - 58, y + 252, x + 58, y + 252));
    }

    private void drawMuscles(Graphics2D g, int x, int y) {
        drawBack(g, x, y);
        drawChest(g, x, y);
        drawArms(g, x, y);
        drawAbs(g, x, y);
        drawLegs(g, x, y);
    }

    private void drawChest(Graphics2D g, int x, int y) {
        int level = player.muscleLevel(MuscleGroup.CHEST);
        g.setColor(MuscleGroup.CHEST.color());
        g.setStroke(strokeFor(level));
        g.drawArc(x - 82, y + 114, 82 + level * 5, 82 + level * 4, 8, 166);
        g.drawArc(x - level * 5, y + 114, 82 + level * 5, 82 + level * 4, 6, 166);
    }

    private void drawArms(Graphics2D g, int x, int y) {
        int level = player.muscleLevel(MuscleGroup.ARMS);
        g.setColor(MuscleGroup.ARMS.color());
        g.setStroke(strokeFor(level));
        g.drawOval(x - 160 - level * 2, y + 166, 54 + level * 6, 96 + level * 6);
        g.drawOval(x + 106 - level * 2, y + 166, 54 + level * 6, 96 + level * 6);
    }

    private void drawAbs(Graphics2D g, int x, int y) {
        int level = player.muscleLevel(MuscleGroup.ABS);
        g.setColor(MuscleGroup.ABS.color());
        g.setStroke(strokeFor(level));
        int width = 40 + level * 4;
        int height = 26 + level * 2;
        for (int row = 0; row < 3; row++) {
            g.drawRoundRect(x - width - 6, y + 190 + row * 38, width, height, 14, 14);
            g.drawRoundRect(x + 6, y + 190 + row * 38, width, height, 14, 14);
        }
    }

    private void drawLegs(Graphics2D g, int x, int y) {
        int level = player.muscleLevel(MuscleGroup.LEGS);
        g.setColor(MuscleGroup.LEGS.color());
        g.setStroke(strokeFor(level));
        g.drawOval(x - 108 - level * 2, y + 320, 62 + level * 7, 144 + level * 6);
        g.drawOval(x + 46 - level * 2, y + 320, 62 + level * 7, 144 + level * 6);
    }

    private void drawBack(Graphics2D g, int x, int y) {
        int level = player.muscleLevel(MuscleGroup.BACK);
        g.setColor(MuscleGroup.BACK.color());
        g.setStroke(strokeFor(level));
        g.drawArc(x - 112 - level * 2, y + 100, 106 + level * 5, 182 + level * 6, 82, 120);
        g.drawArc(x + 6 - level * 2, y + 100, 106 + level * 5, 182 + level * 6, -22, 120);
    }

    private void drawLegend(Graphics2D g) {
        int x = 34;
        int y = getHeight() - 142;

        g.setColor(new Color(40, 44, 55, 225));
        g.fillRoundRect(x - 14, y - 28, 250, 126, 16, 16);
        g.setColor(new Color(0x3a4050));
        g.drawRoundRect(x - 14, y - 28, 250, 126, 16, 16);

        g.setFont(new Font("Dialog", Font.BOLD, 14));
        for (MuscleGroup muscle : MuscleGroup.values()) {
            g.setColor(muscle.color());
            g.fillOval(x, y - 12, 12, 12);
            g.setColor(new Color(0xf4f6fb));
            g.drawString(muscle.displayName() + " Lv." + player.muscleLevel(muscle), x + 20, y);
            y += 22;
        }
    }

    private BasicStroke strokeFor(int level) {
        float width = Math.min(20f, 3.5f + level * 1.8f);
        return new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }
}
