import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Stream;

public class AvatarDemo {
    public static void main(String[] args) {
        Path assetRoot = args.length > 0
                ? Paths.get(args[0]).toAbsolutePath()
                : Paths.get("").toAbsolutePath();

        try {
            AvatarAssets assets = new AvatarAssets(assetRoot);
            AvatarState state = new AvatarState(assets);

            SwingUtilities.invokeLater(() -> showWindow(assets, state));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static void showWindow(AvatarAssets assets, AvatarState state) {
        AvatarPanel avatarPanel = new AvatarPanel(assets, state);

        JFrame frame = new JFrame("正面 2D 人偶狀態顯示系統");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(16, 16));
        frame.getContentPane().setBackground(new Color(245, 246, 248));
        frame.add(avatarPanel, BorderLayout.CENTER);
        frame.add(new ControlPanel(assets, state, avatarPanel), BorderLayout.EAST);
        frame.setMinimumSize(new Dimension(780, 560));
        frame.setLocationByPlatform(true);
        frame.setVisible(true);
    }

    private enum AvatarPart {
        BODY("body", "body", "身體"),
        CHEST("chest", "chest", "胸部"),
        ABDOMEN("abdomen", "abdomen", "腹肌"),
        HAND("hand", "Hand", "手臂", true),
        LEG("leg", "Leg", "腿部", true);

        final String folder;
        final String fileName;
        final String label;
        final boolean hasLeftRightImages;

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

    private static class AvatarState {
        private final AvatarAssets assets;
        private final Map<AvatarPart, Integer> levels = new EnumMap<>(AvatarPart.class);

        AvatarState(AvatarAssets assets) {
            this.assets = assets;
            reset();
        }

        int getLevel(AvatarPart part) {
            return levels.get(part);
        }

        void setLevel(AvatarPart part, int level) {
            int nextLevel = Math.max(1, Math.min(level, assets.maxLevel(part)));
            levels.put(part, nextLevel);
        }

        void levelUp(AvatarPart part) {
            setLevel(part, getLevel(part) + 1);
        }

        void reset() {
            for (AvatarPart part : AvatarPart.values()) {
                levels.put(part, 1);
            }
        }

        void setAllMax() {
            for (AvatarPart part : AvatarPart.values()) {
                levels.put(part, assets.maxLevel(part));
            }
        }
    }

    private static class AvatarAssets {
        private static final int CANVAS_SIZE = 256;
        private static final int PREVIEW_MARGIN = 28;
        private static final int COMPOSE_SIZE = CANVAS_SIZE + PREVIEW_MARGIN * 2;

        // ==============================
        // 正面人偶各部位座標設定區
        // 之後如果要微調人偶外觀，主要改這裡
        // 座標是以原本 256x256 素材畫布為基準，程式會自動加上 PREVIEW_MARGIN 防止裁切
        // ==============================
        private static final int BODY_X = 0;
        private static final int BODY_Y = 0;
        private static final int CHEST_X = 0;
        private static final int CHEST_Y = 0;
        private static final int ABDOMEN_X = 0;
        private static final int ABDOMEN_Y = 0;
        private static final int RIGHT_HAND_BASE_X = 0;
        private static final int LEFT_HAND_BASE_X = 0;
        private static final int HAND_BASE_Y = -10;
        private static final int RIGHT_LEG_BASE_X = 0;
        private static final int LEFT_LEG_BASE_X = 0;
        private static final int LEG_BASE_Y = 0;
        private static final int BACK_HEAD_X = 0;
        private static final int BACK_HEAD_Y = 0;
        private static final int HEAD_X = 0;
        private static final int HEAD_Y = 0;
        private static final int EMOJI_X = 0;
        private static final int EMOJI_Y = 0;

        // ==============================
        // 等級間距調整區
        // 等級越高時，手臂或腿部可以稍微外移，避免和其他部位重疊
        // ==============================
        private static final int HAND_LEVEL_OUT_STEP = 2;
        private static final int CHEST_LEVEL_HAND_OUT_STEP = 2;
        private static final int LEG_LEVEL_OUT_STEP = 2;
        private static final int LEG_LEVEL_DOWN_STEP = 1;

        private final Path root;
        private final Map<AvatarPart, TreeSet<Integer>> levelMap = new EnumMap<>(AvatarPart.class);

        AvatarAssets(Path root) throws IOException {
            this.root = root;
            for (AvatarPart part : AvatarPart.values()) {
                levelMap.put(part, scanLevels(part));
            }
            requireFixedImage("head/back_head.png");
            requireFixedImage("head/head.png");
            requireFixedImage("emoji/emoji.png");
        }

        int maxLevel(AvatarPart part) {
            return levelMap.get(part).last();
        }

        BufferedImage compose(AvatarState state) {
            BufferedImage result = new BufferedImage(COMPOSE_SIZE, COMPOSE_SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = result.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

                // 畫身體：身體中心保持穩定，不受其他部位等級影響。
                drawPart(g, AvatarPart.BODY, state.getLevel(AvatarPart.BODY), null, BODY_X, BODY_Y);

                // 畫腿部：腿部等級越高，左右腿稍微外移並微微下移。
                int legLevelOffset = Math.max(0, state.getLevel(AvatarPart.LEG) - 1);
                int legOffsetX = legLevelOffset * LEG_LEVEL_OUT_STEP;
                int legOffsetY = legLevelOffset * LEG_LEVEL_DOWN_STEP;
                drawPart(g, AvatarPart.LEG, state.getLevel(AvatarPart.LEG), "right",
                        RIGHT_LEG_BASE_X - legOffsetX, LEG_BASE_Y + legOffsetY);
                drawPart(g, AvatarPart.LEG, state.getLevel(AvatarPart.LEG), "left",
                        LEFT_LEG_BASE_X + legOffsetX, LEG_BASE_Y + legOffsetY);

                // 畫手臂：手臂等級和胸部等級越高，手臂稍微往外側讓開。
                int handOffsetX = (Math.max(0, state.getLevel(AvatarPart.HAND) - 1) * HAND_LEVEL_OUT_STEP)
                        + (Math.max(0, state.getLevel(AvatarPart.CHEST) - 1) * CHEST_LEVEL_HAND_OUT_STEP);
                drawPart(g, AvatarPart.HAND, state.getLevel(AvatarPart.HAND), "right",
                        RIGHT_HAND_BASE_X - handOffsetX, HAND_BASE_Y);
                drawPart(g, AvatarPart.HAND, state.getLevel(AvatarPart.HAND), "left",
                        LEFT_HAND_BASE_X + handOffsetX, HAND_BASE_Y);

                // 畫胸部：胸部圖片維持在身體中央，只有胸部等級會改變圖片。
                drawPart(g, AvatarPart.CHEST, state.getLevel(AvatarPart.CHEST), null, CHEST_X, CHEST_Y);
                // 畫腹肌：腹肌圖片維持在身體中央，只有腹肌等級會改變圖片。
                drawPart(g, AvatarPart.ABDOMEN, state.getLevel(AvatarPart.ABDOMEN), null, ABDOMEN_X, ABDOMEN_Y);
                // 畫頭部：固定外觀，不受等級影響。
                drawFixed(g, "head/back_head.png", BACK_HEAD_X, BACK_HEAD_Y);
                drawFixed(g, "head/head.png", HEAD_X, HEAD_Y);
                // 畫臉部表情：固定外觀，不受等級影響。
                drawFixed(g, "emoji/emoji.png", EMOJI_X, EMOJI_Y);
            } finally {
                g.dispose();
            }
            return result;
        }

        private TreeSet<Integer> scanLevels(AvatarPart part) throws IOException {
            TreeSet<Integer> levels = new TreeSet<>();
            Path folder = root.resolve(part.folder);
            if (!Files.isDirectory(folder)) {
                throw new IOException("找不到素材資料夾：" + folder);
            }

            try (Stream<Path> files = Files.list(folder)) {
                files.filter(path -> path.getFileName().toString().endsWith(".png"))
                        .map(path -> path.getFileName().toString())
                        .forEach(fileName -> {
                            Integer level = readLevel(fileName);
                            if (level != null && isPartFile(part, fileName, level)) {
                                levels.add(level);
                            }
                        });
            }

            if (levels.isEmpty()) {
                throw new IOException(part.label + " 沒有可用的 lv 圖片");
            }
            return levels;
        }

        private boolean isPartFile(AvatarPart part, String fileName, int level) {
            if (part.hasLeftRightImages) {
                String left = "lv" + level + "_left" + part.fileName + ".png";
                String right = "lv" + level + "_right" + part.fileName + ".png";
                return fileName.equals(left) || fileName.equals(right);
            }
            return fileName.equals("lv" + level + "_" + part.fileName + ".png");
        }

        private Integer readLevel(String fileName) {
            if (!fileName.startsWith("lv")) {
                return null;
            }
            int underscore = fileName.indexOf('_');
            if (underscore < 2) {
                return null;
            }
            try {
                return Integer.parseInt(fileName.substring(2, underscore));
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        private void drawFixed(Graphics2D g, String relativePath, int x, int y) {
            g.drawImage(readImage(relativePath), PREVIEW_MARGIN + x, PREVIEW_MARGIN + y, null);
        }

        private void drawPart(Graphics2D g, AvatarPart part, int level, String side, int xOffset, int yOffset) {
            String fileName;
            if (part.hasLeftRightImages) {
                fileName = "lv" + level + "_" + side + part.fileName + ".png";
            } else {
                fileName = "lv" + level + "_" + part.fileName + ".png";
            }
            g.drawImage(readImage(part.folder + "/" + fileName),
                    PREVIEW_MARGIN + xOffset,
                    PREVIEW_MARGIN + yOffset,
                    null);
        }

        private BufferedImage readImage(String relativePath) {
            try {
                return ImageIO.read(root.resolve(relativePath).toFile());
            } catch (IOException ex) {
                throw new IllegalStateException("讀取圖片失敗：" + relativePath, ex);
            }
        }

        private void requireFixedImage(String relativePath) throws IOException {
            if (!Files.isRegularFile(root.resolve(relativePath))) {
                throw new IOException("找不到固定素材：" + relativePath);
            }
        }
    }

    private static class AvatarPanel extends JPanel {
        // 調整整個人偶在左側預覽區的大小：數字越大，人偶越小；數字越小，人偶越大。
        private static final int PANEL_PADDING = 32;

        private final AvatarAssets assets;
        private final AvatarState state;

        AvatarPanel(AvatarAssets assets, AvatarState state) {
            this.assets = assets;
            this.state = state;
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createLineBorder(new Color(210, 214, 220), 1));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                BufferedImage avatar = assets.compose(state);
                int availableWidth = Math.max(1, getWidth() - PANEL_PADDING * 2);
                int availableHeight = Math.max(1, getHeight() - PANEL_PADDING * 2);
                double scale = Math.min(
                        availableWidth / (double) avatar.getWidth(),
                        availableHeight / (double) avatar.getHeight());
                int drawWidth = Math.max(1, (int) Math.round(avatar.getWidth() * scale));
                int drawHeight = Math.max(1, (int) Math.round(avatar.getHeight() * scale));
                int x = (getWidth() - drawWidth) / 2;
                int y = (getHeight() - drawHeight) / 2;
                g.drawImage(avatar, x, y, drawWidth, drawHeight, null);
            } finally {
                g.dispose();
            }
        }
    }

    private static class ControlPanel extends JPanel {
        private final AvatarAssets assets;
        private final AvatarState state;
        private final AvatarPanel avatarPanel;
        private final Map<AvatarPart, JComboBox<Integer>> boxes = new EnumMap<>(AvatarPart.class);

        ControlPanel(AvatarAssets assets, AvatarState state, AvatarPanel avatarPanel) {
            this.assets = assets;
            this.state = state;
            this.avatarPanel = avatarPanel;

            setPreferredSize(new Dimension(270, 0));
            setBackground(new Color(245, 246, 248));
            setBorder(BorderFactory.createEmptyBorder(24, 0, 24, 24));
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            JLabel title = new JLabel("部位等級測試");
            title.setFont(new Font("Microsoft JhengHei", Font.BOLD, 20));
            title.setAlignmentX(LEFT_ALIGNMENT);
            add(title);
            add(Box.createVerticalStrut(16));

            add(createPartRow(AvatarPart.BODY));
            add(Box.createVerticalStrut(10));
            add(createPartRow(AvatarPart.CHEST));
            add(Box.createVerticalStrut(10));
            add(createPartRow(AvatarPart.ABDOMEN));
            add(Box.createVerticalStrut(10));
            add(createPartRow(AvatarPart.HAND));
            add(Box.createVerticalStrut(10));
            add(createPartRow(AvatarPart.LEG));
            add(Box.createVerticalStrut(18));
            add(actionButton("全部重設", () -> {
                state.reset();
                syncControls();
                avatarPanel.repaint();
            }));
            add(Box.createVerticalStrut(10));
            add(actionButton("全部最高", () -> {
                state.setAllMax();
                syncControls();
                avatarPanel.repaint();
            }));
            add(Box.createVerticalGlue());
        }

        private JPanel createPartRow(AvatarPart part) {
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setBackground(new Color(245, 246, 248));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

            JLabel label = new JLabel(part.label);
            label.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 15));

            JComboBox<Integer> box = new JComboBox<>();
            for (int level = 1; level <= assets.maxLevel(part); level++) {
                box.addItem(level);
            }
            box.setSelectedItem(state.getLevel(part));
            box.addActionListener(event -> {
                Integer selected = (Integer) box.getSelectedItem();
                if (selected != null) {
                    state.setLevel(part, selected);
                    avatarPanel.repaint();
                }
            });
            boxes.put(part, box);

            JButton upButton = new JButton("升級");
            upButton.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 14));
            upButton.addActionListener(event -> {
                state.levelUp(part);
                box.setSelectedItem(state.getLevel(part));
                avatarPanel.repaint();
            });

            row.add(label, BorderLayout.WEST);
            row.add(box, BorderLayout.CENTER);
            row.add(upButton, BorderLayout.EAST);
            return row;
        }

        private JButton actionButton(String text, Runnable action) {
            JButton button = new JButton(text);
            button.setAlignmentX(LEFT_ALIGNMENT);
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
            button.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 15));
            button.addActionListener(event -> action.run());
            return button;
        }

        private void syncControls() {
            for (AvatarPart part : AvatarPart.values()) {
                boxes.get(part).setSelectedItem(state.getLevel(part));
            }
        }
    }
}
