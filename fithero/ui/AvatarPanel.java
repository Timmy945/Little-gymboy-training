package fithero.ui;

import fithero.logic.manager.PlayerState;
import fithero.model.exercise.MuscleGroup;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
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
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * 頂級客製化畫布：融合 2D 正反雙面肌肉動態四階級縮放引擎、五維戰力雷達圖與內嵌式角色經驗卡
 */
public class AvatarPanel extends JPanel {
    private PlayerState player;
    private static final String FONT_FAMILY = "Microsoft JhengHei";

    private static final int CANVAS_SIZE = 256;
    private static final int PREVIEW_MARGIN = 28;
    private static final int COMPOSE_SIZE = CANVAS_SIZE + PREVIEW_MARGIN * 2;

    // 關節調整物理步長
    private static final int HAND_LEVEL_OUT_STEP = 2;
    private static final int CHEST_LEVEL_HAND_OUT_STEP = 2;
    private static final int LEG_LEVEL_OUT_STEP = 2;
    private static final int LEG_LEVEL_DOWN_STEP = 1;

    public enum AvatarPart {
        BODY("body", "body", "身體"),
        CHEST("chest", "chest", "胸部"),
        ABDOMEN("abdomen", "abdomen", "腹肌"),
        HAND("hand", "Hand", "手臂", true),   
        LEG("leg", "Leg", "腿部", true),       
        BACK("back", "back", "背肌");

        public final String folder;
        public final String fileName;
        public final String label;
        public final boolean hasLeftRightImages;

        AvatarPart(String folder, String fileName, String label) {
            this(folder, fileName, label, false);
        }

        AvatarPart(String folder, String fileName, String label, boolean hasLeftRightImages) {
            this.folder = folder;
            this.fileName = fileName;
            this.label = label;
            this.hasLeftRightImages = hasLeftRightImages;
        }
    }

    // 🧠【動態四階級生理模型】
    private class LevelMetaData {
        int fileLevel;     // 實體對應圖檔 lv1 ~ lv4
        double scaleBonus; // 階級內微幅線條膨脹放大率

        LevelMetaData(int rawLevel) {
            if (rawLevel <= 10) {
                this.fileLevel = 1;
                this.scaleBonus = 1.0 + (Math.max(1, rawLevel) - 1) * 0.015; 
            } else if (rawLevel <= 25) {
                this.fileLevel = 2;
                this.scaleBonus = 1.0 + (rawLevel - 11) * 0.015;             
            } else if (rawLevel <= 50) {
                this.fileLevel = 3;
                this.scaleBonus = 1.0 + (rawLevel - 26) * 0.012;             
            } else {
                this.fileLevel = 4;
                this.scaleBonus = Math.min(1.25, 1.0 + (rawLevel - 51) * 0.01); 
            }
        }
    }

    private final Path assetRoot;
    private final Map<AvatarPart, TreeSet<Integer>> assetLevelMap = new EnumMap<>(AvatarPart.class);
    private boolean assetsAvailable = false;

    private final MuscleGroup[] radarMuscles = {
        MuscleGroup.CHEST, MuscleGroup.ARMS, MuscleGroup.LEGS, MuscleGroup.ABS, MuscleGroup.BACK
    };

    public AvatarPanel(PlayerState player) {
        this.player = player;
        setPreferredSize(new Dimension(610, 690));
        setBackground(new Color(0x282c37));

        // 🛠️ 採用多級路徑自動適配探测器，確保不論在 IDE 還是編譯後的 jar 都能咬合資產
        Path rootOpt1 = Paths.get("assets").toAbsolutePath();
        Path rootOpt2 = Paths.get("..", "assets").toAbsolutePath();
        if (Files.isDirectory(rootOpt1)) {
            this.assetRoot = rootOpt1;
        } else if (Files.isDirectory(rootOpt2)) {
            this.assetRoot = rootOpt2;
        } else {
            this.assetRoot = rootOpt1; // 保底預設
        }
        
        reloadAssetsConfiguration();
    }

    public void reloadAssetsConfiguration() {
        try {
            if (Files.isDirectory(assetRoot)) {
                scanAllAssetLevels();
                // 擴大資產包可用性查核線：只要有 body 或 head 存在即視為綠燈可用
                assetsAvailable = Files.exists(assetRoot.resolve("body/lv1_body.png")) || 
                                  Files.exists(assetRoot.resolve("head/head.png"));
            } else {
                assetsAvailable = false;
            }
        } catch (Exception e) {
            assetsAvailable = false;
        }
    }

    public void setPlayer(PlayerState player) {
        this.player = player;
        reloadAssetsConfiguration(); 
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawBackground(g);
        drawGridLines(g);
        drawHeader(g);

        if (assetsAvailable) {
            drawDynamicLayeredAvatars(g);
        } else {
            drawFallbackStickman(g, getWidth() / 2 - 150, 150);
        }

        drawEmbeddedRadarChart(g);
        drawEmbeddedPlayerStats(g);

        g.dispose();
    }

    private void drawDynamicLayeredAvatars(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        BufferedImage frontImg = composeFrontAvatar();
        BufferedImage backImg = composeBackAvatar();

        // 調整比例，留出更舒適的寬度給右側雷達圖
        int availableWidth = getWidth() - 280; 
        int availableHeight = getHeight() - 220;  
        int combinedWidth = frontImg.getWidth() + backImg.getWidth();

        double scale = Math.min(availableWidth / (double) combinedWidth, availableHeight / (double) frontImg.getHeight());
        int drawWidth = (int) (frontImg.getWidth() * scale);
        int drawHeight = (int) (frontImg.getHeight() * scale);

        // 人偶擺放起點稍微靠左
        int startX = 22;
        int y = 95 + (availableHeight - drawHeight) / 2;

        g.drawImage(frontImg, startX, y, drawWidth, drawHeight, null);
        g.drawImage(backImg, startX + drawWidth - 10, y, drawWidth, drawHeight, null); 
    }

    private BufferedImage composeFrontAvatar() {
        BufferedImage img = new BufferedImage(COMPOSE_SIZE, COMPOSE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            
            drawPart(g2, AvatarPart.BODY, player.level(), null, 0, 0);

            int legLvl = player.muscleLevel(MuscleGroup.LEGS);
            int legOffset = Math.max(0, legLvl - 1);
            drawPart(g2, AvatarPart.LEG, legLvl, "left", -(legOffset * LEG_LEVEL_OUT_STEP), legOffset * LEG_LEVEL_DOWN_STEP);
            drawPart(g2, AvatarPart.LEG, legLvl, "right", (legOffset * LEG_LEVEL_OUT_STEP), legOffset * LEG_LEVEL_DOWN_STEP);

            int chestLvl = player.muscleLevel(MuscleGroup.CHEST);
            int armLvl = player.muscleLevel(MuscleGroup.ARMS);
            int handOffsetX = (Math.max(0, armLvl - 1) * HAND_LEVEL_OUT_STEP) + (Math.max(0, chestLvl - 1) * CHEST_LEVEL_HAND_OUT_STEP);
            drawPart(g2, AvatarPart.HAND, armLvl, "left", -handOffsetX, -10);
            drawPart(g2, AvatarPart.HAND, armLvl, "right", handOffsetX, -10);

            drawPart(g2, AvatarPart.CHEST, chestLvl, null, 0, 0);
            drawPart(g2, AvatarPart.ABDOMEN, player.muscleLevel(MuscleGroup.ABS), null, 0, 0);

            drawFixedAsset(g2, "head/back_head.png", 0, 0);
            drawFixedAsset(g2, "head/head.png", 0, 0);
            drawFixedAsset(g2, "emoji/emoji.png", 0, 0);
        } finally { 
            g2.dispose(); 
        }
        return img;
    }

    private BufferedImage composeBackAvatar() {
        BufferedImage img = new BufferedImage(COMPOSE_SIZE, COMPOSE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            
            drawBackPart(g2, AvatarPart.BODY, player.level(), null, 0, 0);

            int legLvl = player.muscleLevel(MuscleGroup.LEGS);
            int legOffset = Math.max(0, legLvl - 1);
            drawBackPart(g2, AvatarPart.LEG, legLvl, "left", -(legOffset * LEG_LEVEL_OUT_STEP), legOffset * LEG_LEVEL_DOWN_STEP);
            drawBackPart(g2, AvatarPart.LEG, legLvl, "right", (legOffset * LEG_LEVEL_OUT_STEP), legOffset * LEG_LEVEL_DOWN_STEP);

            int armLvl = player.muscleLevel(MuscleGroup.ARMS);
            int handOffsetX = Math.max(0, armLvl - 1) * HAND_LEVEL_OUT_STEP;
            drawBackPart(g2, AvatarPart.HAND, armLvl, "left", -handOffsetX, -10);
            drawBackPart(g2, AvatarPart.HAND, armLvl, "right", handOffsetX, -10);

            drawPart(g2, AvatarPart.BACK, player.muscleLevel(MuscleGroup.BACK), null, 0, 0);
            drawFixedAsset(g2, "head/back_head.png", 0, 0);
        } finally { 
            g2.dispose(); 
        }
        return img;
    }

    private void drawPart(Graphics2D g, AvatarPart part, int rawLevel, String side, int x, int y) {
        LevelMetaData meta = new LevelMetaData(rawLevel);
        
        // 精準防禦組員設計的正面駝峰命名 (例如：lv1_leftHand.png)
        String camelPartName = part.name().substring(0, 1).toLowerCase() + part.name().substring(1).toLowerCase();
        if (part == AvatarPart.HAND) camelPartName = "Hand";
        if (part == AvatarPart.LEG) camelPartName = "Leg";

        String fileName = (side != null) 
            ? "lv" + meta.fileLevel + "_" + side + camelPartName + ".png" 
            : "lv" + meta.fileLevel + "_" + part.fileName + ".png";
        
        BufferedImage img = readAssetImage(part.folder + "/" + fileName);
        if (img == null || img.getWidth() <= 1) return; 

        int finalW = (int) (img.getWidth() * meta.scaleBonus);
        int finalH = (int) (img.getHeight() * meta.scaleBonus);
        
        int pivotOffsetX = (img.getWidth() - finalW) / 2;
        int pivotOffsetY = (img.getHeight() - finalH) / 2;

        g.drawImage(img, PREVIEW_MARGIN + x + pivotOffsetX, PREVIEW_MARGIN + y + pivotOffsetY, finalW, finalH, null);
    }

    private void drawBackPart(Graphics2D g, AvatarPart part, int rawLevel, String side, int x, int y) {
        LevelMetaData meta = new LevelMetaData(rawLevel);
        
        // 🛠️【超級容錯優化】因應組員背面四肢全小寫命名 Bug，在這裡自動切換小寫檔名比對
        String lowerPartName = part.name().toLowerCase();

        String backName = (side != null) 
            ? "lv" + meta.fileLevel + "_back_" + side + lowerPartName + ".png" 
            : "lv" + meta.fileLevel + "_back_" + part.fileName + ".png";
        
        Path targetFile = assetRoot.resolve(part.folder).resolve(backName);
        if (Files.isRegularFile(targetFile)) {
            BufferedImage img = readAssetImage(part.folder + "/" + backName);
            if (img != null && img.getWidth() > 1) {
                int finalW = (int) (img.getWidth() * meta.scaleBonus);
                int finalH = (int) (img.getHeight() * meta.scaleBonus);
                int pivotOffsetX = (img.getWidth() - finalW) / 2;
                int pivotOffsetY = (img.getHeight() - finalH) / 2;
                g.drawImage(img, PREVIEW_MARGIN + x + pivotOffsetX, PREVIEW_MARGIN + y + pivotOffsetY, finalW, finalH, null);
                return; 
            }
        }
        
        // 完美自動退讓防線
        drawPart(g, part, rawLevel, side, x, y);
    }

    private void drawFixedAsset(Graphics2D g, String path, int x, int y) {
        BufferedImage img = readAssetImage(path);
        if (img != null && img.getWidth() > 1) {
            g.drawImage(img, PREVIEW_MARGIN + x, PREVIEW_MARGIN + y, null);
        }
    }

    private BufferedImage readAssetImage(String relPath) {
        try {
            Path targetPath = assetRoot.resolve(relPath);
            if (!Files.exists(targetPath)) {
                Path parentDir = targetPath.getParent();
                if (parentDir != null && Files.isDirectory(parentDir)) {
                    try (Stream<Path> s = Files.list(parentDir)) {
                        Path matched = s.filter(p -> p.getFileName().toString().equalsIgnoreCase(targetPath.getFileName().toString()))
                                        .findFirst().orElse(null);
                        if (matched != null) return ImageIO.read(matched.toFile());
                    }
                }
                return null;
            }
            return ImageIO.read(targetPath.toFile());
        } catch (IOException e) {
            return null;
        }
    }

    private void scanAllAssetLevels() {
        for (AvatarPart part : AvatarPart.values()) {
            TreeSet<Integer> lvls = new TreeSet<>();
            Path dir = assetRoot.resolve(part.folder);
            if (Files.isDirectory(dir)) {
                try (Stream<Path> stream = Files.list(dir)) {
                    stream.map(p -> p.getFileName().toString())
                          .filter(name -> name.startsWith("lv") && name.endsWith(".png"))
                          .forEach(name -> {
                              try {
                                  int score = name.indexOf('_');
                                  if (score > 2) {
                                    lvls.add(Integer.parseInt(name.substring(2, score)));
                                  }
                              } catch (Exception ignored) {}
                          });
                } catch (Exception ignored) {}
            }
            if (lvls.isEmpty()) lvls.add(1);
            assetLevelMap.put(part, lvls); 
        }
    }

    private void drawBackground(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, new Color(0x303645), 0, getHeight(), new Color(0x222733)));
        g.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
    }

    private void drawGridLines(Graphics2D g) {
        g.setColor(new Color(0x3a4050));
        for (int y = 110; y < getHeight() - 140; y += 44) {
            g.drawLine(24, y, getWidth() - 24, y);
        }
    }

    private void drawHeader(Graphics2D g) {
        g.setColor(new Color(0xf4f6fb)); g.setFont(new Font(FONT_FAMILY, Font.BOLD, 26));
        g.drawString("角色體態與特徵分析", 32, 42);
        g.setColor(new Color(0xb7c0d1)); g.setFont(new Font(FONT_FAMILY, Font.PLAIN, 13));
        g.drawString("用真實訓練打破基因限制，系統自動演進肌群與五維指標。", 34, 66);
    }

    private void drawEmbeddedRadarChart(Graphics2D g) {
        int rcX = getWidth() - 145; int rcY = getHeight() / 2 - 40; int maxR = 85;
        g.setStroke(new BasicStroke(1f)); g.setColor(new Color(0x3a4050));
        for (int ring = 1; ring <= 3; ring++) g.draw(createPentagonPath(rcX, rcY, maxR * ring / 3));

        Path2D playerPath = new Path2D.Double();
        int maxVisualLevel = 20;

        for (int i = 0; i < 5; i++) {
            double angle = Math.toRadians(i * 72 - 90);
            g.setColor(new Color(0x3a4050));
            g.drawLine(rcX, rcY, (int)(rcX + maxR * Math.cos(angle)), (int)(rcY + maxR * Math.sin(angle)));

            int lvl = player.muscleLevel(radarMuscles[i]);
            double ratio = (double) Math.min(maxVisualLevel, lvl) / maxVisualLevel;
            int rVal = (int) (maxR * ratio);
            int pX = (int) (rcX + rVal * Math.cos(angle)); int pY = (int) (rcY + rVal * Math.sin(angle));
            if (i == 0) playerPath.moveTo(pX, pY); else playerPath.lineTo(pX, pY);
        }
        playerPath.closePath();
        g.setColor(new Color(47, 155, 255, 110)); g.fill(playerPath);
        g.setColor(new Color(0x5aa9ff)); g.setStroke(new BasicStroke(2f)); g.draw(playerPath);

        g.setFont(new Font(FONT_FAMILY, Font.BOLD, 11));
        FontMetrics fm = g.getFontMetrics(); g.setColor(new Color(0xf4f6fb));
        for (int i = 0; i < 5; i++) {
            double angle = Math.toRadians(i * 72 - 90);
            String txt = radarMuscles[i].displayName() + " L" + player.muscleLevel(radarMuscles[i]);
            int tX = (int) (rcX + (maxR + 18) * Math.cos(angle)) - fm.stringWidth(txt) / 2;
            int tY = (int) (rcY + (maxR + 18) * Math.sin(angle)) + fm.getAscent() / 2 - 2;
            g.drawString(txt, tX, tY);
        }
    }

    private Path2D createPentagonPath(int cx, int cy, int r) {
        Path2D path = new Path2D.Double();
        for (int i = 0; i < 5; i++) {
            double angle = Math.toRadians(i * 72 - 90);
            if (i == 0) path.moveTo(cx + r * Math.cos(angle), cy + r * Math.sin(angle));
            else path.lineTo(cx + r * Math.cos(angle), cy + r * Math.sin(angle));
        }
        path.closePath(); return path;
    }

    private void drawEmbeddedPlayerStats(Graphics2D g) {
        int x = 34; int y = getHeight() - 110; int cardWidth = getWidth() - 68; int cardHeight = 84;
        g.setColor(new Color(0x1a1d24)); g.fill(new RoundRectangle2D.Double(x, y, cardWidth, cardHeight, 14, 14));
        g.setColor(new Color(0x3a4050)); g.setStroke(new BasicStroke(1.5f)); g.draw(new RoundRectangle2D.Double(x, y, cardWidth, cardHeight, 14, 14));

        g.setFont(new Font(FONT_FAMILY, Font.BOLD, 16)); g.setColor(new Color(0xf4f6fb));
        g.drawString(player.getAvatar().getName() + " 【" + player.getAvatar().getCurrentTitle() + "】", x + 18, y + 30);
        String lvText = "等級: Lv." + player.level();
        g.setColor(new Color(0x5aa9ff)); g.drawString(lvText, x + cardWidth - g.getFontMetrics().stringWidth(lvText) - 18, y + 30);

        int barX = x + 18; int barY = y + 46; int barWidth = cardWidth - 36; int barHeight = 20;
        g.setColor(new Color(0x20242d)); g.fill(new RoundRectangle2D.Double(barX, barY, barWidth, barHeight, 8, 8));
        int progressWidth = (int) (barWidth * Math.min(1.0, (double) player.xp() / player.xpNeededForNextLevel()));
        if (progressWidth > 0) {
            g.setColor(new Color(0x2f9bff)); g.fill(new RoundRectangle2D.Double(barX, barY, progressWidth, barHeight, 8, 8));
        }
        g.setFont(new Font(FONT_FAMILY, Font.BOLD, 11)); g.setColor(Color.WHITE);
        String pStr = "XP: " + player.xp() + " / " + player.xpNeededForNextLevel();
        g.drawString(pStr, barX + (barWidth - g.getFontMetrics().stringWidth(pStr)) / 2, barY + 14);
    }

    // 🛠️【修復點】補上漏掉的極簡火柴人降級回調繪製核心，斬殺 1 error 編譯警報
    private void drawFallbackStickman(Graphics2D g, int x, int y) {
        g.setColor(new Color(0xdde4f0)); 
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        
        // 畫骨骼線條
        g.draw(new Ellipse2D.Double(x - 34, y, 68, 68)); // 頭部
        g.draw(new Line2D.Double(x, y + 68, x, y + 190)); // 軀幹
        g.draw(new Line2D.Double(x - 60, y + 110, x + 60, y + 110)); // 雙臂
        g.draw(new Line2D.Double(x, y + 190, x - 40, y + 320)); // 左腿
        g.draw(new Line2D.Double(x, y + 190, x + 40, y + 320)); // 右腿
    }
}