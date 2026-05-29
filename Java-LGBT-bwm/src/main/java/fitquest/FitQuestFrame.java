package fitquest;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

public class FitQuestFrame extends JFrame {
    private static final Color APP_BG = new Color(0x1e222b);
    private static final Color PANEL_BG = new Color(0x282c37);
    private static final Color TEXT = new Color(0xf4f6fb);
    private static final Color MUTED = new Color(0xb7c0d1);
    private static final Color ACCENT = new Color(0x5aa9ff);
    private static final Color CTA_BLUE = new Color(59, 130, 246);
    private static final Color NAV_BG = new Color(0x181b22);
    private static final Color NAV_SELECTED = new Color(0x3a4354);
    private static final Color BUTTON_BG = new Color(40, 44, 55);
    private static final Color BORDER = new Color(0x3a4050);

    private final Storage storage = new Storage(Path.of("data"));
    private final PlayerState player = storage.loadPlayer();
    private final List<WorkoutEntry> workouts = new ArrayList<>(storage.loadWorkouts());
    private final AvatarPanel avatarPanel = new AvatarPanel(player);
    private final CardLayout pageLayout = new CardLayout();
    private final JPanel pageContainer = new JPanel(pageLayout);
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();

    private final JLabel levelValue = statValueLabel();
    private final JLabel xpValue = statValueLabel();
    private final JLabel pointsValue = statValueLabel();
    private final JLabel todayValue = statValueLabel();
    private final JLabel feedbackLabel = new JLabel("選擇訓練後會直接記錄，不會再跳出 OK 視窗。");
    private final JProgressBar xpProgress = new JProgressBar();
    private final JSpinner amountSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 999, 1));
    private final DefaultTableModel historyModel = new DefaultTableModel(
            new String[] {"時間", "訓練", "數量", "獲得 XP"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    public FitQuestFrame() {
        super("FitQuest - 遊戲化健身紀錄");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1024, 768);
        setMinimumSize(new Dimension(1024, 768));
        setLayout(new BorderLayout());
        getContentPane().setBackground(APP_BG);

        pageContainer.setBackground(APP_BG);
        pageContainer.add(createHomePage(), "home");
        pageContainer.add(new CalendarPage(this, workouts), "calendar");
        pageContainer.add(createPlaceholderPage("分析", "Analysis Page"), "analytics");
        pageContainer.add(createPlaceholderPage("個人資料", "Profile Page"), "profile");

        add(pageContainer, BorderLayout.CENTER);
        add(createBottomNavigation(), BorderLayout.SOUTH);

        refreshAll();
        showPage("home");
        setLocationRelativeTo(null);
    }

    private JPanel createHomePage() {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(APP_BG);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(APP_BG);
        content.setBorder(BorderFactory.createEmptyBorder(18, 18, 24, 18));

        JButton startButton = new JButton("開始運動");
        startButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        startButton.setFont(new Font("Dialog", Font.BOLD, 24));
        applySolidButtonStyle(startButton, CTA_BLUE, Color.WHITE);
        startButton.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));
        startButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));
        startButton.addActionListener(event -> new WorkoutSessionDialog(this).setVisible(true));

        content.add(startButton);
        content.add(gap(18));
        content.add(createWorkoutChartSection());
        content.add(gap(18));
        content.add(createPlanSection());

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(APP_BG);
        scrollPane.getViewport().setBackground(APP_BG);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        page.add(scrollPane, BorderLayout.CENTER);
        return page;
    }

    private JPanel createWorkoutChartSection() {
        JPanel panel = wideSectionPanel();
        panel.setLayout(new BorderLayout(0, 18));
        panel.setPreferredSize(new Dimension(900, 360));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 360));

        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        tabs.setOpaque(false);
        tabs.add(tabButton("時間", true));
        tabs.add(tabButton("體積", false));
        tabs.add(tabButton("密度", false));

        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);

        GridBagConstraints summaryConstraints = new GridBagConstraints();
        summaryConstraints.gridx = 0;
        summaryConstraints.gridy = 0;
        summaryConstraints.weightx = 0.26;
        summaryConstraints.fill = GridBagConstraints.BOTH;
        summaryConstraints.insets = new Insets(0, 0, 0, 18);
        body.add(createWeeklySummary(), summaryConstraints);

        GridBagConstraints chartConstraints = new GridBagConstraints();
        chartConstraints.gridx = 1;
        chartConstraints.gridy = 0;
        chartConstraints.weightx = 0.74;
        chartConstraints.weighty = 1.0;
        chartConstraints.fill = GridBagConstraints.BOTH;
        body.add(new WorkoutBarChartPanel(), chartConstraints);

        panel.add(tabs, BorderLayout.NORTH);
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createWeeklySummary() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setPreferredSize(new Dimension(235, 260));

        JLabel title = sectionTitle("本週平均");
        JLabel average = new JLabel("<html>本週平均鍛鍊時間為<br><span style='font-size:30px;'>52 分鐘</span></html>");
        average.setForeground(TEXT);
        average.setFont(new Font("Dialog", Font.BOLD, 18));

        JLabel comparison = new JLabel("與上周相比平均增加 2%");
        comparison.setForeground(new Color(0x70d69a));
        comparison.setFont(new Font("Dialog", Font.BOLD, 16));

        panel.add(title);
        panel.add(gap(22));
        panel.add(average);
        panel.add(gap(14));
        panel.add(comparison);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel createPlanSection() {
        JPanel panel = wideSectionPanel();
        panel.setLayout(new BorderLayout(0, 16));
        panel.setPreferredSize(new Dimension(900, 230));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 230));
        panel.add(sectionTitle("計畫 (Workout Plans)"), BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(1, 3, 14, 0));
        cards.setOpaque(false);
        cards.add(planCard("練胸日", "槓鈴臥推 60kg 12下 3組", "啞鈴飛鳥 / 伏地挺身", "點擊查看菜單"));
        cards.add(planCard("練腿日", "深蹲 80kg 8下 4組", "腿推 / 弓箭步", "點擊查看菜單"));
        cards.add(planCard("背部日", "槓鈴划船 50kg 10下 3組", "引體向上 / 滑輪下拉", "點擊查看菜單"));

        panel.add(cards, BorderLayout.CENTER);
        return panel;
    }

    private JPanel planCard(String title, String subtitle, String meta, String level) {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(new Color(0x20242d));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)
        ));
        card.setPreferredSize(new Dimension(280, 150));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(TEXT);
        titleLabel.setFont(new Font("Dialog", Font.BOLD, 20));

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setForeground(MUTED);
        subtitleLabel.setFont(new Font("Dialog", Font.PLAIN, 14));

        JLabel metaLabel = new JLabel(meta + " · " + level);
        metaLabel.setForeground(new Color(0xcfe5ff));
        metaLabel.setFont(new Font("Dialog", Font.BOLD, 14));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(titleLabel);
        text.add(gap(6));
        text.add(subtitleLabel);

        card.add(text, BorderLayout.CENTER);
        card.add(metaLabel, BorderLayout.SOUTH);
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                JOptionPane.showMessageDialog(
                        FitQuestFrame.this,
                        title + "\n\n" + subtitle + "\n" + meta + "\n建議休息：每組 90 秒",
                        "計畫詳細菜單",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
        });
        return card;
    }

    private JButton tabButton(String text, boolean selected) {
        JButton button = new JButton(text);
        button.setFont(new Font("Dialog", Font.BOLD, 15));
        applySolidButtonStyle(button, selected ? BUTTON_BG : new Color(0x20242d), Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(10, 24, 10, 24));
        return button;
    }

    private JPanel createBottomNavigation() {
        JPanel nav = new JPanel(new GridLayout(1, 4, 12, 0));
        nav.setBackground(NAV_BG);
        nav.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
        nav.setPreferredSize(new Dimension(1024, 82));

        addNavButton(nav, "home", "主頁");
        addNavButton(nav, "calendar", "日曆");
        addNavButton(nav, "analytics", "分析");
        addNavButton(nav, "profile", "個人資料");
        return nav;
    }

    private void addNavButton(JPanel nav, String pageName, String label) {
        JButton button = new JButton(label);
        button.setFont(new Font("Dialog", Font.BOLD, 18));
        applySolidButtonStyle(button, BUTTON_BG, TEXT);
        button.setMargin(new Insets(12, 20, 12, 20));
        button.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        button.addActionListener(event -> showPage(pageName));

        navButtons.put(pageName, button);
        nav.add(button);
    }

    private void showPage(String pageName) {
        pageLayout.show(pageContainer, pageName);
        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            boolean selected = entry.getKey().equals(pageName);
            JButton button = entry.getValue();
            button.setBackground(selected ? NAV_SELECTED : BUTTON_BG);
            button.setForeground(selected ? ACCENT : MUTED);
        }
    }

    private JPanel createPlaceholderPage(String title, String body) {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(APP_BG);
        page.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JPanel panel = sectionPanel();
        panel.setLayout(new BorderLayout(0, 12));

        JLabel titleLabel = sectionTitle(title);
        JLabel bodyLabel = new JLabel(body, SwingConstants.CENTER);
        bodyLabel.setFont(new Font("Dialog", Font.PLAIN, 22));
        bodyLabel.setForeground(MUTED);

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(bodyLabel, BorderLayout.CENTER);
        page.add(panel, BorderLayout.CENTER);
        return page;
    }

    private JScrollPane createDashboard() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(APP_BG);
        content.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        content.setPreferredSize(new Dimension(430, 900));

        content.add(createHeader());
        content.add(gap(12));
        content.add(createStatsPanel());
        content.add(gap(12));
        content.add(createWorkoutPanel());
        content.add(gap(12));
        content.add(createUpgradePanel());
        content.add(gap(12));
        content.add(createHistoryPanel());

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(APP_BG);
        scrollPane.getViewport().setBackground(APP_BG);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        return scrollPane;
    }

    private JPanel createHeader() {
        JPanel panel = sectionPanel();
        panel.setLayout(new BorderLayout(8, 8));

        JLabel title = new JLabel("FitQuest");
        title.setFont(new Font("Dialog", Font.BOLD, 30));
        title.setForeground(TEXT);

        JLabel subtitle = new JLabel("訓練、升級、把角色練壯");
        subtitle.setFont(new Font("Dialog", Font.PLAIN, 14));
        subtitle.setForeground(MUTED);

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.add(title);
        textPanel.add(subtitle);

        feedbackLabel.setOpaque(true);
        feedbackLabel.setBackground(new Color(0x22344a));
        feedbackLabel.setForeground(new Color(0xcfe5ff));
        feedbackLabel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        feedbackLabel.setFont(new Font("Dialog", Font.PLAIN, 13));

        panel.add(textPanel, BorderLayout.NORTH);
        panel.add(feedbackLabel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createStatsPanel() {
        JPanel panel = sectionPanel();
        panel.setLayout(new BorderLayout(0, 12));

        JPanel stats = new JPanel(new GridLayout(1, 4, 8, 8));
        stats.setOpaque(false);
        stats.add(statBlock("等級", levelValue));
        stats.add(statBlock("XP", xpValue));
        stats.add(statBlock("點數", pointsValue));
        stats.add(statBlock("今日", todayValue));

        xpProgress.setMinimum(0);
        xpProgress.setStringPainted(true);
        xpProgress.setForeground(ACCENT);
        xpProgress.setBackground(new Color(0x1c2029));
        xpProgress.setPreferredSize(new Dimension(100, 26));

        panel.add(stats, BorderLayout.CENTER);
        panel.add(xpProgress, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createWorkoutPanel() {
        JPanel panel = sectionPanel();
        panel.setLayout(new BorderLayout(0, 12));
        panel.add(sectionTitle("快速記錄訓練"), BorderLayout.NORTH);

        JPanel amountRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        amountRow.setOpaque(false);
        amountRow.add(formLabel("每次記錄數量"));
        amountSpinner.setPreferredSize(new Dimension(84, 30));
        amountRow.add(amountSpinner);
        amountRow.add(formLabel("次 / 分鐘"));

        JPanel buttons = new JPanel(new GridLayout(3, 2, 8, 8));
        buttons.setOpaque(false);
        for (WorkoutType type : WorkoutType.values()) {
            buttons.add(workoutButton(type));
        }

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(amountRow);
        body.add(gap(10));
        body.add(buttons);

        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createUpgradePanel() {
        JPanel panel = sectionPanel();
        panel.setLayout(new BorderLayout(0, 12));
        panel.add(sectionTitle("肌群升級"), BorderLayout.NORTH);

        JPanel rows = new JPanel(new GridLayout(MuscleGroup.values().length, 1, 8, 8));
        rows.setOpaque(false);
        for (MuscleGroup muscle : MuscleGroup.values()) {
            rows.add(muscleUpgradeRow(muscle));
        }

        panel.add(rows, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createHistoryPanel() {
        JPanel panel = sectionPanel();
        panel.setLayout(new BorderLayout(0, 10));
        panel.add(sectionTitle("最近訓練紀錄"), BorderLayout.NORTH);

        JTable table = new JTable(historyModel);
        table.setRowHeight(28);
        table.setBackground(new Color(0x20242d));
        table.setForeground(TEXT);
        table.setGridColor(BORDER);
        table.setSelectionBackground(NAV_SELECTED);
        table.setSelectionForeground(Color.WHITE);
        table.getTableHeader().setBackground(PANEL_BG);
        table.getTableHeader().setForeground(TEXT);
        table.getTableHeader().setReorderingAllowed(false);
        table.setFillsViewportHeight(true);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(380, 210));
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER));
        scrollPane.getViewport().setBackground(new Color(0x20242d));
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JButton workoutButton(WorkoutType type) {
        JButton button = new JButton("<html><b>" + type.displayName() + "</b><br><span style='font-size:10px;'>"
                + type.mainMuscle().displayName() + " / 每單位 " + type.calculateXp(1) + " XP</span></html>");
        button.setHorizontalAlignment(SwingConstants.LEFT);
        applySolidButtonStyle(button, BUTTON_BG, Color.WHITE);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(tint(type.mainMuscle().color())),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        button.addActionListener(event -> addWorkout(type));
        return button;
    }

    private JPanel muscleUpgradeRow(MuscleGroup muscle) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);

        JLabel label = new JLabel(muscle.displayName());
        label.setForeground(TEXT);
        label.setFont(new Font("Dialog", Font.BOLD, 14));

        JLabel level = new JLabel("Lv." + player.muscleLevel(muscle));
        level.setForeground(MUTED);

        JButton button = new JButton("升級");
        applySolidButtonStyle(button, BUTTON_BG, TEXT);
        button.setBorder(BorderFactory.createLineBorder(tint(muscle.color())));
        button.addActionListener(event -> upgradeMuscle(muscle));

        JPanel text = new JPanel(new GridLayout(1, 2));
        text.setOpaque(false);
        text.add(label);
        text.add(level);

        row.add(text, BorderLayout.CENTER);
        row.add(button, BorderLayout.EAST);
        return row;
    }

    private void addWorkout(WorkoutType type) {
        int amount = (Integer) amountSpinner.getValue();
        int xpBefore = player.xp();
        int levelBefore = player.level();
        int pointsBefore = player.upgradePoints();

        int xp = player.addWorkout(type, amount);
        workouts.add(new WorkoutEntry(LocalDateTime.now(), type, amount, xp));
        saveAndRefresh();

        if (player.level() > levelBefore) {
            int gainedPoints = player.upgradePoints() - pointsBefore;
            feedbackLabel.setText("升級！Lv." + levelBefore + " -> Lv." + player.level()
                    + "，獲得 " + gainedPoints + " 點升級點。");
        } else {
            feedbackLabel.setText("已記錄 " + type.displayName() + " " + amount
                    + "，獲得 " + xp + " XP。距離升級還差 "
                    + (player.xpNeededForNextLevel() - player.xp()) + " XP。");
        }

        if (player.xp() < xpBefore && player.level() == levelBefore) {
            feedbackLabel.setText("已記錄 " + type.displayName() + "，獲得 " + xp + " XP。");
        }
    }

    private void upgradeMuscle(MuscleGroup muscle) {
        if (!player.upgrade(muscle)) {
            feedbackLabel.setText("升級點不足。先完成幾次訓練，升級後就能強化 " + muscle.displayName() + "。");
            return;
        }
        saveAndRefresh();
        feedbackLabel.setText(muscle.displayName() + " 強化成功，現在是 Lv." + player.muscleLevel(muscle) + "。");
    }

    private void saveAndRefresh() {
        storage.savePlayer(player);
        storage.saveWorkouts(workouts);
        refreshAll();
    }

    private void refreshAll() {
        levelValue.setText("Lv." + player.level());
        xpValue.setText(player.xp() + "/" + player.xpNeededForNextLevel());
        pointsValue.setText(String.valueOf(player.upgradePoints()));
        todayValue.setText(String.valueOf(todayWorkoutCount()));

        xpProgress.setMaximum(player.xpNeededForNextLevel());
        xpProgress.setValue(player.xp());
        xpProgress.setString("下一級進度 " + player.xp() + " / " + player.xpNeededForNextLevel());

        avatarPanel.setPlayer(player);
        refreshHistory();
    }

    private int todayWorkoutCount() {
        LocalDate today = LocalDate.now();
        int count = 0;
        for (WorkoutEntry entry : workouts) {
            if (entry.time().toLocalDate().equals(today)) {
                count++;
            }
        }
        return count;
    }

    private void refreshHistory() {
        historyModel.setRowCount(0);
        workouts.stream()
                .sorted(Comparator.comparing(WorkoutEntry::time).reversed())
                .limit(12)
                .forEach(entry -> historyModel.addRow(new Object[] {
                        entry.displayTime(),
                        entry.type().displayName(),
                        entry.amount(),
                        entry.xp()
                }));
    }

    private JPanel sectionPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(PANEL_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(430, Integer.MAX_VALUE));
        return panel;
    }

    private JPanel wideSectionPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(PANEL_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)
        ));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return panel;
    }

    private JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Dialog", Font.BOLD, 18));
        label.setForeground(TEXT);
        return label;
    }

    private JPanel statBlock(String title, JLabel valueLabel) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBackground(new Color(0x20242d));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(MUTED);
        titleLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
        valueLabel.setHorizontalAlignment(SwingConstants.LEFT);

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(valueLabel, BorderLayout.CENTER);
        return panel;
    }

    private JLabel statValueLabel() {
        JLabel label = new JLabel();
        label.setFont(new Font("Dialog", Font.BOLD, 18));
        label.setForeground(TEXT);
        return label;
    }

    private JLabel formLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(MUTED);
        return label;
    }

    private void applySolidButtonStyle(JButton button, Color background, Color foreground) {
        button.setForeground(foreground);
        button.setBackground(background);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
    }

    private Component gap(int height) {
        return Box.createRigidArea(new Dimension(1, height));
    }

    private Color tint(Color color) {
        int red = (color.getRed() + 255) / 2;
        int green = (color.getGreen() + 255) / 2;
        int blue = (color.getBlue() + 255) / 2;
        return new Color(red, green, blue);
    }
}
