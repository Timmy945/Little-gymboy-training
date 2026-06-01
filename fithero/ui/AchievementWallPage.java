package fithero.ui;

import fithero.logic.manager.PlayerState;
import fithero.model.achievement.Achievement;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * 榮譽成就牆分頁：具備 100 項成就動態高亮、難度色塊分級與未解鎖隱藏馬賽克機制
 */
public class AchievementWallPage extends JPanel {
    private static final Color APP_BG = new Color(0x1e222b);
    private static final Color PANEL_BG = new Color(0x282c37);
    private static final Color TEXT = new Color(0xf4f6fb);
    private static final Color MUTED = new Color(0xb7c0d1);
    private static final Color ACCENT = new Color(0x5aa9ff);
    private static final Color BORDER = new Color(0x3a4050);
    
    private static final String FONT_FAMILY = "Microsoft JhengHei";

    public AchievementWallPage(PlayerState player) {
        setBackground(APP_BG);
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        setLayout(new BorderLayout(0, 16));

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);

        JLabel titleLabel = new JLabel("榮譽成就牆");
        titleLabel.setFont(new Font(FONT_FAMILY, Font.BOLD, 20));
        titleLabel.setForeground(TEXT);

        JLabel descLabel = new JLabel("打破基因臨界值，總計 100 項鋼鐵意志里程碑，包含高難度與隱藏挑戰。");
        descLabel.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 13));
        descLabel.setForeground(MUTED);

        header.add(titleLabel);
        header.add(Box.createRigidArea(new Dimension(1, 6)));
        header.add(descLabel);
        add(header, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(0, 3, 14, 14));
        grid.setOpaque(false);

        var achievementManager = player.getAchievementManager(); 
        List<Achievement> allAchievements = achievementManager.getAchievementList();

        for (Achievement ach : allAchievements) {
            grid.add(createAchievementCard(ach, ach.isUnlocked()));
        }

        JScrollPane scroll = new JScrollPane(grid);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setBackground(APP_BG);
        scroll.getViewport().setBackground(APP_BG);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        add(scroll, BorderLayout.CENTER);
    }

    private JPanel createAchievementCard(Achievement ach, boolean isUnlocked) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(PANEL_BG);
        
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isUnlocked ? ACCENT : BORDER, isUnlocked ? 2 : 1),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)
        ));

        Color diffColor = switch (ach.getDifficulty()) {
            case "極困難" -> new Color(0xef4444); 
            case "困難" -> new Color(0xf97316);   
            case "普通" -> new Color(0xeab308);   
            default -> MUTED;
        };

        JLabel statusLabel = new JLabel(isUnlocked ? "🏆 已解鎖" : "🔒 [" + ach.getDifficulty() + "]");
        statusLabel.setFont(new Font(FONT_FAMILY, Font.BOLD, 12));
        statusLabel.setForeground(isUnlocked ? ACCENT : diffColor);

        String titleText = (ach.isHidden() && !isUnlocked) ? "??? (隱藏成就)" : ach.getTitle();
        String descText = (ach.isHidden() && !isUnlocked) ? "未知的隱藏挑戰。繼續訓練以探索未知领域。" : ach.getDescription();

        JLabel nameLabel = new JLabel(titleText);
        nameLabel.setFont(new Font(FONT_FAMILY, Font.BOLD, 15));
        nameLabel.setForeground(isUnlocked ? TEXT : MUTED);

        JLabel condLabel = new JLabel("<html>" + descText + "</html>");
        condLabel.setFont(new Font(FONT_FAMILY, Font.PLAIN, 12));
        condLabel.setForeground(MUTED);

        card.add(statusLabel);
        card.add(Box.createRigidArea(new Dimension(1, 8)));
        card.add(nameLabel);
        card.add(Box.createRigidArea(new Dimension(1, 6)));
        card.add(condLabel);

        return card;
    }
}