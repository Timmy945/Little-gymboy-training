package fithero.ui;

import fithero.logic.manager.PlayerState;
import fithero.model.achievement.Achievement;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.*;

public class AchievementWallPage extends JPanel {
    private static final Color APP_BG = new Color(0x1e222b);
    private static final Color PANEL_BG = new Color(0x282c37);
    private static final Color CELL_BG = new Color(0x20242d);
    private static final Color TEXT = new Color(0xf4f6fb);
    private static final Color MUTED = new Color(0xb7c0d1);
    private static final Color ACCENT = new Color(0x5aa9ff);
    private static final Color BORDER = new Color(0x3a4050);
    private static final Color NAV_SELECTED = new Color(0x3a4354);
    
    // 黃金榮譽漸層
    private static final Color GOLD_GRAD_TOP = new Color(45, 38, 25);
    private static final Color GOLD_GRAD_BOTTOM = new Color(32, 28, 20);
    private static final Color MEDAL_GOLD = new Color(0xd4af37); // 黃金勳章色

    private static final String FONT_FAMILY = "Microsoft JhengHei";

    private final PlayerState playerState;
    private final JPanel cardsGridContainer;
    private final JButton tabUnlockedBtn;
    private final JButton tabLockedBtn;
    
    private boolean showUnlockedFilter = true; 

    public AchievementWallPage(PlayerState player) {
        this.playerState = player;
        setBackground(APP_BG);
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        setLayout(new BorderLayout(0, 16));

        JPanel topWrapper = new JPanel(new BorderLayout());
        topWrapper.setOpaque(false);

        JPanel infoHeader = new JPanel();
        infoHeader.setLayout(new BoxLayout(infoHeader, BoxLayout.Y_AXIS));
        infoHeader.setOpaque(false);

        JLabel titleLabel = new JLabel("榮譽成就牆中心");
        titleLabel.setFont(new Font(FONT_FAMILY, Font.BOLD, 22)); titleLabel.setForeground(TEXT);
        JLabel descLabel = new JLabel("點擊任意【已解鎖】榮譽勳章，即可更換人偶稱號。");
        descLabel.setFont(new Font(FONT_FAMILY, Font.PLAIN, 13)); descLabel.setForeground(MUTED);

        infoHeader.add(titleLabel); infoHeader.add(Box.createRigidArea(new Dimension(1, 6))); infoHeader.add(descLabel);
        topWrapper.add(infoHeader, BorderLayout.WEST);

        // 頁籤更換為萬國碼安全符號
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filterRow.setOpaque(false);

        tabUnlockedBtn = createFilterTabButton("◆ 已解鎖榮譽", true);
        tabLockedBtn = createFilterTabButton("◇ 未解鎖挑戰", false);
        filterRow.add(tabUnlockedBtn); filterRow.add(tabLockedBtn);
        topWrapper.add(filterRow, BorderLayout.EAST);

        add(topWrapper, BorderLayout.NORTH);

        cardsGridContainer = new JPanel(new GridLayout(0, 3, 14, 14));
        cardsGridContainer.setOpaque(false);

        JScrollPane scroll = new JScrollPane(cardsGridContainer);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setBackground(APP_BG); scroll.getViewport().setBackground(APP_BG);
        scroll.getVerticalScrollBar().setUnitIncrement(20);

        add(scroll, BorderLayout.CENTER);
        refreshFilteredGrid();
    }

    private JButton createFilterTabButton(String text, boolean targetFilter) {
        JButton btn = new JButton(text);
        btn.setFont(new Font(FONT_FAMILY, Font.BOLD, 13)); btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        btn.addActionListener(e -> {
            this.showUnlockedFilter = targetFilter;
            refreshFilteredGrid();
        });
        return btn;
    }

    public void refreshFilteredGrid() {
        tabUnlockedBtn.setBackground(showUnlockedFilter ? NAV_SELECTED : PANEL_BG);
        tabUnlockedBtn.setForeground(showUnlockedFilter ? ACCENT : MUTED);
        tabLockedBtn.setBackground(!showUnlockedFilter ? NAV_SELECTED : PANEL_BG);
        tabLockedBtn.setForeground(!showUnlockedFilter ? ACCENT : MUTED);

        cardsGridContainer.removeAll();

        List<Achievement> filteredList = playerState.getAchievementManager().getAchievementList().stream()
                .filter(ach -> ach.isUnlocked() == showUnlockedFilter)
                .collect(Collectors.toList());

        if (filteredList.isEmpty()) {
            JPanel emptyBox = new JPanel(new BorderLayout()); emptyBox.setOpaque(false);
            JLabel msg = new JLabel(showUnlockedFilter ? "當前尚無已解鎖榮譽，快去鍛鍊吧！" : "◆ 奇蹟！您已完美制霸全數 100 項榮譽天梯！", SwingConstants.CENTER);
            msg.setFont(new Font(FONT_FAMILY, Font.PLAIN, 15)); msg.setForeground(MUTED);
            emptyBox.add(msg, BorderLayout.CENTER);
            cardsGridContainer.setLayout(new BorderLayout());
            cardsGridContainer.add(emptyBox);
        } else {
            // 【GridBagLayout 黃金美學重構】徹底斬殺 GridLayout 卡片巨大化的惡夢
            cardsGridContainer.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(7, 7, 7, 7); // 卡片外邊距
            gbc.fill = GridBagConstraints.BOTH;  // 允許在格子內填滿
            gbc.weightx = 1.0;                   // 水平權重均分
            gbc.weighty = 0.0;                   // 【關鍵點】垂直絕對不向下衍生拉伸！

            int columns = 3; // 固定每行 3 列
            for (int i = 0; i < filteredList.size(); i++) {
                gbc.gridx = i % columns;
                gbc.gridy = i / columns;
                
                JPanel card = createFancyAchievementCard(filteredList.get(i));
                // 強制鎖定卡片的黃金幾何尺寸，防止極端數量時變形
                card.setPreferredSize(new Dimension(220, 130));
                card.setMinimumSize(new Dimension(200, 120));
                
                cardsGridContainer.add(card, gbc);
            }

            // 如果最後一行的卡片沒有填滿 3 列（例如只有 1 筆或 2 筆），用透明的假格子把剩餘格子撐住！
            int rem = filteredList.size() % columns;
            if (rem > 0) {
                gbc.weighty = 0.0;
                for (int k = rem; k < columns; k++) {
                    gbc.gridx = k;
                    gbc.gridy = (filteredList.size() / columns);
                    JPanel ghostPanel = new JPanel();
                    ghostPanel.setOpaque(false); // 完全透明隱形
                    ghostPanel.setPreferredSize(new Dimension(220, 130));
                    cardsGridContainer.add(ghostPanel, gbc);
                }
            }

            // 在最底下塞一個強力的垂直彈簧，把上方不論幾排的成就卡片全部「往上推聚攏」，防止垂直方向被拉長變肥！
            GridBagConstraints pusherGbc = new GridBagConstraints();
            pusherGbc.gridx = 0;
            pusherGbc.gridy = (filteredList.size() / columns) + 1;
            pusherGbc.gridwidth = columns;
            pusherGbc.weighty = 1.0; // 拿走所有垂直剩餘空間
            pusherGbc.fill = GridBagConstraints.VERTICAL;
            JPanel verticalPusher = new JPanel();
            verticalPusher.setOpaque(false);
            cardsGridContainer.add(verticalPusher, pusherGbc);
        }
        
        cardsGridContainer.revalidate(); 
        cardsGridContainer.repaint();
    }

    private JPanel createFancyAchievementCard(Achievement ach) {
        boolean isUnlocked = ach.isUnlocked();
        
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (isUnlocked) {
                    GradientPaint gp = new GradientPaint(0, 0, GOLD_GRAD_TOP, 0, getHeight(), GOLD_GRAD_BOTTOM);
                    g2.setPaint(gp);
                } else {
                    g2.setColor(PANEL_BG); 
                }
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isUnlocked ? MEDAL_GOLD : BORDER, isUnlocked ? 2 : 1), 
                BorderFactory.createEmptyBorder(14, 16, 14, 16)
        ));

        Color diffColor = switch (ach.getDifficulty()) {
            case "極困難" -> new Color(0xef4444); case "困難" -> new Color(0xf97316);   
            case "普通" -> new Color(0xeab308);   default -> MUTED;
        };

        // 🛠️【修復點 1】剔除高位元 Emoji，換裝為 Java 2D 完好支援之星芒符格
        JLabel statusLabel = new JLabel(isUnlocked ? "◆ 榮譽已達成" : "◇ [" + ach.getDifficulty() + "]");
        statusLabel.setFont(new Font(FONT_FAMILY, Font.BOLD, 12));
        statusLabel.setForeground(isUnlocked ? MEDAL_GOLD : diffColor);

        String titleText = (ach.isHidden() && !isUnlocked) ? "??? (隱藏成就)" : ach.getTitle();
        String descText = (ach.isHidden() && !isUnlocked) ? "未知的隱藏挑戰。繼續訓練以探索未知領域。" : ach.getDescription();

        JLabel nameLabel = new JLabel(titleText);
        nameLabel.setFont(new Font(FONT_FAMILY, Font.BOLD, 15));
        nameLabel.setForeground(isUnlocked ? Color.WHITE : MUTED);

        JLabel condLabel = new JLabel("<html>" + descText + "</html>");
        condLabel.setFont(new Font(FONT_FAMILY, Font.PLAIN, 12)); condLabel.setForeground(isUnlocked ? TEXT : MUTED);

        card.add(statusLabel); card.add(Box.createRigidArea(new Dimension(1, 8)));
        card.add(nameLabel); card.add(Box.createRigidArea(new Dimension(1, 6)));
        card.add(condLabel);

        if (isUnlocked) {
            card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            card.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    // 🛠️【修復點 2】呼叫專屬設計的豪華暗黑科幻風對話框，斬殺原生醜視窗
                    showLuxuryTitleConfirmDialog(ach.getTitle());
                }
                @Override
                public void mouseEntered(MouseEvent e) {
                    card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.WHITE, 2), BorderFactory.createEmptyBorder(14, 16, 14, 16)
                    ));
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(MEDAL_GOLD, 2), BorderFactory.createEmptyBorder(14, 16, 14, 16)
                    ));
                }
            });
        }
        return card;
    }

    /**
     * 【全新實作】全自訂高階暗黑風稱號確認工作台，完美融合遊戲主題
     */
    private void showLuxuryTitleConfirmDialog(String targetTitle) {
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parentWindow, " ⚔  冒險者稱號", JDialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true); // 拔除原生白邊老舊視窗外框
        dialog.setSize(400, 180);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(PANEL_BG);
        panel.setBorder(BorderFactory.createLineBorder(ACCENT, 2)); // 賽博藍科技框線

        // 頂部大標
        JLabel head = new JLabel(" 稱號變更確認", SwingConstants.LEFT);
        head.setFont(new Font(FONT_FAMILY, Font.BOLD, 15)); head.setForeground(ACCENT);
        head.setPreferredSize(new Dimension(400, 36));
        head.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
        panel.add(head, BorderLayout.NORTH);

        // 中部提示內文
        JLabel body = new JLabel("<html>確定要將您當前的稱號，變更為：<br><span style='color:#5aa9ff; font-size:14px; font-weight:bold;'>【" + targetTitle + "】</span> 嗎？</html>", SwingConstants.CENTER);
        body.setFont(new Font(FONT_FAMILY, Font.PLAIN, 13)); body.setForeground(TEXT);
        body.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        panel.add(body, BorderLayout.CENTER);

        // 底部自訂流線雙色按鈕列
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 10));
        btnRow.setOpaque(false);

        JButton cancelBtn = new JButton("取消");
        cancelBtn.setFont(new Font(FONT_FAMILY, Font.PLAIN, 13));
        cancelBtn.setBackground(CELL_BG); cancelBtn.setForeground(MUTED);
        cancelBtn.setFocusPainted(false); cancelBtn.setBorder(BorderFactory.createEmptyBorder(6, 18, 6, 18));
        cancelBtn.addActionListener(e -> dialog.dispose());

        JButton confirmBtn = new JButton("確認配戴");
        confirmBtn.setFont(new Font(FONT_FAMILY, Font.BOLD, 13));
        confirmBtn.setBackground(new Color(16, 185, 129)); // 翡翠綠
        confirmBtn.setForeground(Color.WHITE);
        confirmBtn.setBorderPainted(false); confirmBtn.setFocusPainted(false);
        confirmBtn.setBorder(BorderFactory.createEmptyBorder(6, 20, 6, 20));
        
        confirmBtn.addActionListener(e -> {
            playerState.getAvatar().setCurrentTitle(targetTitle);
            FitQuestFrame topFrame = (FitQuestFrame) SwingUtilities.getWindowAncestor(this);
            if (topFrame != null) {
                topFrame.saveAndRefresh();
            }
            dialog.dispose();
            
            // 跳出高質感客製化單確認成功提示
            showLuxuryMessageDialog(" 變更成功！已配戴全新稱號。");
        });

        btnRow.add(cancelBtn); btnRow.add(confirmBtn);
        panel.add(btnRow, BorderLayout.SOUTH);

        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    /**
     * 自訂高質感單確認成功彈窗
     */
    private void showLuxuryMessageDialog(String message) {
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        JDialog msgDialog = new JDialog(parentWindow, "通知", JDialog.ModalityType.APPLICATION_MODAL);
        msgDialog.setUndecorated(true); msgDialog.setSize(380, 120); msgDialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(PANEL_BG); panel.setBorder(BorderFactory.createLineBorder(MEDAL_GOLD, 2)); // 黃金細框

        JLabel textLabel = new JLabel("<html>" + message + "</html>", SwingConstants.CENTER);
        textLabel.setFont(new Font(FONT_FAMILY, Font.PLAIN, 13)); textLabel.setForeground(TEXT);
        textLabel.setBorder(BorderFactory.createEmptyBorder(14, 14, 0, 14));
        panel.add(textLabel, BorderLayout.CENTER);

        JButton okBtn = new JButton("確定");
        okBtn.setFont(new Font(FONT_FAMILY, Font.BOLD, 12));
        okBtn.setBackground(CELL_BG); okBtn.setForeground(ACCENT); okBtn.setFocusPainted(false);
        okBtn.setBorder(BorderFactory.createEmptyBorder(5, 16, 5, 16));
        okBtn.addActionListener(e -> msgDialog.dispose());

        JPanel btnWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 8)); btnWrapper.setOpaque(false);
        btnWrapper.add(okBtn);
        panel.add(btnWrapper, BorderLayout.SOUTH);

        msgDialog.setContentPane(panel); msgDialog.setVisible(true);
    }
}