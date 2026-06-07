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

        JFrame frame = new JFrame("2D 人偶狀態顯示系統");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(16, 16));
        frame.getContentPane().setBackground(new Color(245, 246, 248));
        frame.add(avatarPanel, BorderLayout.CENTER);
        frame.add(new ControlPanel(assets, state, avatarPanel), BorderLayout.EAST);
        frame.setMinimumSize(new Dimension(1100, 560));
        frame.setLocationByPlatform(true);
        frame.setVisible(true);
    }

    private enum AvatarPart {
        BODY("body", "body", "身體"),
        CHEST("chest", "chest", "胸部"),
        ABDOMEN("abdomen", "abdomen", "腹肌"),
        HAND("hand", "Hand", "手臂", true),
        LEG("leg", "Leg", "腿部", true),
        BACK("back", "back", "背肌");

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
        // ==============================
        private static final int FRONT_BODY_X = 0;
        private static final int FRONT_BODY_Y = 0;
        private static final int FRONT_CHEST_X = 0;
        private static final int FRONT_CHEST_Y = 0;
        private static final int FRONT_ABDOMEN_X = 0;
        private static final int FRONT_ABDOMEN_Y = 0;
        private static final int FRONT_RIGHT_HAND_BASE_X = 0;
        private static final int FRONT_LEFT_HAND_BASE_X = 0;
        private static final int FRONT_HAND_BASE_Y = -10;
        private static final int FRONT_RIGHT_LEG_BASE_X = 0;
        private static final int FRONT_LEFT_LEG_BASE_X = 0;
        private static final int FRONT_LEG_BASE_Y = 0;
        private static final int FRONT_BACK_HEAD_X = 0;
        private static final int FRONT_BACK_HEAD_Y = 0;
        private static final int FRONT_HEAD_X = 0;
        private static final int FRONT_HEAD_Y = 0;
        private static final int FRONT_EMOJI_X = 0;
        private static final int FRONT_EMOJI_Y = 0;

        // ==============================
        // 背面人偶各部位座標設定區
        // ==============================
        private static final int BACK_BODY_X = 0;
        private static final int BACK_BODY_Y = 0;
        private static final int BACK_MUSCLE_X = 0;
        private static final int BACK_MUSCLE_Y = 0;
        private static final int BACK_RIGHT_HAND_BASE_X = 0;
        private static final int BACK_LEFT_HAND_BASE_X = 0;
        private static final int BACK_HAND_BASE_Y = -10;
        private static final int BACK_RIGHT_LEG_BASE_X = 0;
        private static final int BACK_LEFT_LEG_BASE_X = 0;
        private static final int BACK_LEG_BASE_Y = 0;
        private static final int BACK_HEAD_X = 0;
        private static final int BACK_HEAD_Y = 0;

        // ==============================
        // 等級造成的部位位移設定區
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

        BufferedImage composeFront(AvatarState state) {
            BufferedImage result = new BufferedImage(COMPOSE_SIZE, COMPOSE_SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = result.createGraphics();
            try {
                prepareGraphics(g);
                drawPart(g, AvatarPart.BODY, state.getLevel(AvatarPart.BODY), null, FRONT_BODY_X, FRONT_BODY_Y);

                int legLevelOffset = Math.max(0, state.getLevel(AvatarPart.LEG) - 1);
                int legOffsetX = legLevelOffset * LEG_LEVEL_OUT_STEP;
                int legOffsetY = legLevelOffset * LEG_LEVEL_DOWN_STEP;
                drawPart(g, AvatarPart.LEG, state.getLevel(AvatarPart.LEG), "right",
                        FRONT_RIGHT_LEG_BASE_X - legOffsetX, FRONT_LEG_BASE_Y + legOffsetY);
                drawPart(g, AvatarPart.LEG, state.getLevel(AvatarPart.LEG), "left",
                        FRONT_LEFT_LEG_BASE_X + legOffsetX, FRONT_LEG_BASE_Y + legOffsetY);

                int handOffsetX = frontHandOffset(state);
                drawPart(g, AvatarPart.HAND, state.getLevel(AvatarPart.HAND), "right",
                        FRONT_RIGHT_HAND_BASE_X - handOffsetX, FRONT_HAND_BASE_Y);
                drawPart(g, AvatarPart.HAND, state.getLevel(AvatarPart.HAND), "left",
                        FRONT_LEFT_HAND_BASE_X + handOffsetX, FRONT_HAND_BASE_Y);

                drawPart(g, AvatarPart.CHEST, state.getLevel(AvatarPart.CHEST), null,
                        FRONT_CHEST_X, FRONT_CHEST_Y);
                drawPart(g, AvatarPart.ABDOMEN, state.getLevel(AvatarPart.ABDOMEN), null,
                        FRONT_ABDOMEN_X, FRONT_ABDOMEN_Y);
                drawFixed(g, "head/back_head.png", FRONT_BACK_HEAD_X, FRONT_BACK_HEAD_Y);
                drawFixed(g, "head/head.png", FRONT_HEAD_X, FRONT_HEAD_Y);
                drawFixed(g, "emoji/emoji.png", FRONT_EMOJI_X, FRONT_EMOJI_Y);
            } finally {
                g.dispose();
            }
            return result;
        }

        BufferedImage composeBack(AvatarState state) {
            BufferedImage result = new BufferedImage(COMPOSE_SIZE, COMPOSE_SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = result.createGraphics();
            try {
                prepareGraphics(g);
                drawBackPart(g, AvatarPart.BODY, state.getLevel(AvatarPart.BODY), null,
                        BACK_BODY_X, BACK_BODY_Y);

                int legLevelOffset = Math.max(0, state.getLevel(AvatarPart.LEG) - 1);
                int legOffsetX = legLevelOffset * LEG_LEVEL_OUT_STEP;
                int legOffsetY = legLevelOffset * LEG_LEVEL_DOWN_STEP;
                drawBackPart(g, AvatarPart.LEG, state.getLevel(AvatarPart.LEG), "right",
                        BACK_RIGHT_LEG_BASE_X - legOffsetX, BACK_LEG_BASE_Y + legOffsetY);
                drawBackPart(g, AvatarPart.LEG, state.getLevel(AvatarPart.LEG), "left",
                        BACK_LEFT_LEG_BASE_X + legOffsetX, BACK_LEG_BASE_Y + legOffsetY);

                int handOffsetX = backHandOffset(state);
                drawBackPart(g, AvatarPart.HAND, state.getLevel(AvatarPart.HAND), "right",
                        BACK_RIGHT_HAND_BASE_X - handOffsetX, BACK_HAND_BASE_Y);
                drawBackPart(g, AvatarPart.HAND, state.getLevel(AvatarPart.HAND), "left",
                        BACK_LEFT_HAND_BASE_X + handOffsetX, BACK_HAND_BASE_Y);

                drawPart(g, AvatarPart.BACK, state.getLevel(AvatarPart.BACK), null,
                        BACK_MUSCLE_X, BACK_MUSCLE_Y);
                drawFixed(g, "head/back_head.png", BACK_HEAD_X, BACK_HEAD_Y);
            } finally {
                g.dispose();
            }
            return result;
        }

        private void prepareGraphics(Graphics2D g) {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        }

        private int frontHandOffset(AvatarState state) {
            return (Math.max(0, state.getLevel(AvatarPart.HAND) - 1) * HAND_LEVEL_OUT_STEP)
                    + (Math.max(0, state.getLevel(AvatarPart.CHEST) - 1) * CHEST_LEVEL_HAND_OUT_STEP);
        }

        private int backHandOffset(AvatarState state) {
            return Math.max(0, state.getLevel(AvatarPart.HAND) - 1) * HAND_LEVEL_OUT_STEP;
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
                throw new IOException(part.label + " 沒有可用的 lv 素材");
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
            String fileName = frontFileName(part, level, side);
            drawImage(g, part.folder + "/" + fileName, xOffset, yOffset);
        }

        private void drawBackPart(Graphics2D g, AvatarPart part, int level, String side, int xOffset, int yOffset) {
            String backFileName = backFileName(part, level, side);
            Path backPath = root.resolve(part.folder).resolve(backFileName);
            String fileName = Files.isRegularFile(backPath) && !hasDuplicateBackSides(part, level)
                    ? backFileName
                    : frontFileName(part, level, side);
            drawImage(g, part.folder + "/" + fileName, xOffset, yOffset);
        }

        private boolean hasDuplicateBackSides(AvatarPart part, int level) {
            if (!part.hasLeftRightImages) {
                return false;
            }

            Path leftPath = root.resolve(part.folder).resolve(backFileName(part, level, "left"));
            Path rightPath = root.resolve(part.folder).resolve(backFileName(part, level, "right"));
            if (!Files.isRegularFile(leftPath) || !Files.isRegularFile(rightPath)) {
                return false;
            }

            try {
                return java.util.Arrays.equals(Files.readAllBytes(leftPath), Files.readAllBytes(rightPath));
            } catch (IOException ex) {
                return false;
            }
        }

        private void drawImage(Graphics2D g, String relativePath, int x, int y) {
            g.drawImage(readImage(relativePath), PREVIEW_MARGIN + x, PREVIEW_MARGIN + y, null);
        }

        private String frontFileName(AvatarPart part, int level, String side) {
            if (part.hasLeftRightImages) {
                return "lv" + level + "_" + side + part.fileName + ".png";
            }
            return "lv" + level + "_" + part.fileName + ".png";
        }

        private String backFileName(AvatarPart part, int level, String side) {
            if (part.hasLeftRightImages) {
                return "lv" + level + "_back_" + side + part.fileName + ".png";
            }
            return "lv" + level + "_back_" + part.fileName + ".png";
        }

        private BufferedImage readImage(String relativePath) {
            try {
                return ImageIO.read(root.resolve(relativePath).toFile());
            } catch (IOException ex) {
                throw new IllegalStateException("無法讀取圖片：" + relativePath, ex);
            }
        }

        private void requireFixedImage(String relativePath) throws IOException {
            if (!Files.isRegularFile(root.resolve(relativePath))) {
                throw new IOException("找不到固定圖片：" + relativePath);
            }
        }
    }

    private static class AvatarPanel extends JPanel {
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
                BufferedImage frontAvatar = assets.composeFront(state);
                BufferedImage backAvatar = assets.composeBack(state);
                int availableWidth = Math.max(1, getWidth() - PANEL_PADDING * 2);
                int availableHeight = Math.max(1, getHeight() - PANEL_PADDING * 2);
                int combinedWidth = frontAvatar.getWidth() + backAvatar.getWidth();
                double scale = Math.min(
                        availableWidth / (double) combinedWidth,
                        availableHeight / (double) frontAvatar.getHeight());
                int drawWidth = Math.max(1, (int) Math.round(frontAvatar.getWidth() * scale));
                int drawHeight = Math.max(1, (int) Math.round(frontAvatar.getHeight() * scale));
                int x = (getWidth() - drawWidth * 2) / 2;
                int y = (getHeight() - drawHeight) / 2;
                g.drawImage(frontAvatar, x, y, drawWidth, drawHeight, null);
                g.drawImage(backAvatar, x + drawWidth, y, drawWidth, drawHeight, null);
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

            setPreferredSize(new Dimension(290, 0));
            setBackground(new Color(245, 246, 248));
            setBorder(BorderFactory.createEmptyBorder(24, 0, 24, 24));
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            JLabel title = new JLabel("部位等級測試");
            title.setFont(new Font("Microsoft JhengHei", Font.BOLD, 20));
            title.setAlignmentX(LEFT_ALIGNMENT);
            add(title);
            add(Box.createVerticalStrut(16));

            for (AvatarPart part : AvatarPart.values()) {
                add(createPartRow(part));
                add(Box.createVerticalStrut(10));
            }
            add(Box.createVerticalStrut(8));
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
