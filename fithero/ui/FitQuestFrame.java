package fithero.ui;

import fithero.infra.Storage;
import fithero.logic.manager.PlayerState;
import fithero.logic.manager.FitnessGoal;
import fithero.model.player.Gender;
import fithero.model.workout.WorkoutEntry;
import fithero.model.exercise.ExerciseRegistry;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.MouseInfo; 
import java.awt.Point;     
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities; 
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

/**
 * 應用程式主視窗框架：已完成跨套件低耦合重構，實作全域分頁指針與重置管線。
 */
public class FitQuestFrame extends JFrame {
    private static final Color APP_BG = new Color(0x1e222b);
    private static final Color PANEL_BG = new Color(0x282c37);
    private static final Color CELL_BG = new Color(0x20242d);
    private static final Color TEXT = new Color(0xf4f6fb);
    private static final Color MUTED = new Color(0xb7c0d1);
    private static final Color ACCENT = new Color(0x5aa9ff);
    private static final Color CTA_BLUE = new Color(59, 130, 246);
    private static final Color RESET_RED = new Color(239, 68, 68); 
    private static final Color NAV_SELECTED = new Color(0x3a4354);
    private static final Color BUTTON_BG = new Color(40, 44, 55);
    private static final Color BORDER = new Color(0x3a4050);

    private static final String FONT_FAMILY = "Microsoft JhengHei";

    private final Storage storage = new Storage(Path.of("data"));
    private PlayerState player; 
    private List<WorkoutEntry> workouts = new ArrayList<>(); 
    
    private final AvatarPanel avatarPanel;
    private final WorkoutBarChartPanel chartPanel = new WorkoutBarChartPanel();
    
    private final CardLayout pageLayout = new CardLayout();
    private final JPanel pageContainer = new JPanel(pageLayout);
    private final Map<String, JButton> scaleButtons = new LinkedHashMap<>();

    private CalendarPage calendarPage;
    private AchievementWallPage achievementWallPage;

    private JLabel weightProgressLabel;

    private JTextField nameField;
    private JTextField ageField; 
    private JTextField heightField;
    private JTextField weightField;
    private JTextField targetWeightField; 
    private JComboBox<Gender> genderBox;
    private JComboBox<FitnessGoal> goalBox; 

    private JLabel bmiLiveLabel;
    private JLabel bmrLiveLabel;
    private JLabel tdeeLiveLabel;
    private JLabel recommendCalLabel; 

    private final DefaultTableModel historyModel = new DefaultTableModel(
            new String[] {"時間", "訓練項目", "數量/時間", "訓練組數"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) { return false; } 
    };

    public PlayerState getPlayerState() { return this.player; }
    public List<WorkoutEntry> getWorkoutsList() { return this.workouts; }
    public CalendarPage getCalendarPage() { return this.calendarPage; }

    public FitQuestFrame() {
        super("FitQuest - 遊戲化健身養成系統");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180, 840); 
        setMinimumSize(new Dimension(1180, 840));
        setLayout(new BorderLayout());
        getContentPane().setBackground(APP_BG);

        this.player = storage.loadPlayer();
        this.workouts.addAll(storage.loadWorkouts());
        
        this.avatarPanel = new AvatarPanel(player);
        this.calendarPage = new CalendarPage(this, workouts);
        this.achievementWallPage = new AchievementWallPage(player);

        pageContainer.setBackground(APP_BG);
        pageContainer.add(createHomePage(), "home");
        pageContainer.add(calendarPage, "calendar"); 
        pageContainer.add(achievementWallPage, "analytics"); 
        pageContainer.add(createProfilePage(), "profile"); 

        JPanel globalTopBar = new JPanel(new BorderLayout());
        globalTopBar.setBackground(APP_BG);
        globalTopBar.setBorder(BorderFactory.createEmptyBorder(12, 18, 0, 18));
        globalTopBar.add(createHoverNavigationGear(), BorderLayout.WEST); 
        
        add(globalTopBar, BorderLayout.NORTH); 
        add(pageContainer, BorderLayout.CENTER); 

        // 開機自動掃描一次昨日未完訓狀態
        this.calendarPage.runBootUpStreakCheck(player);

        refreshAll(); 
        showPage("home");
        setLocationRelativeTo(null); 
    }

    private JButton createHoverNavigationGear() {
        JPopupMenu navMenu = new JPopupMenu();
        navMenu.setBackground(PANEL_BG);
        navMenu.setBorder(BorderFactory.createLineBorder(BORDER));

        String[] menuLabels = {"首頁", "運動行事曆", "榮譽成就牆", "個人資料"};
        String[] menuKeys = {"home", "calendar", "analytics", "profile"};

        for (int i = 0; i < menuLabels.length; i++) {
            final String targetPage = menuKeys[i];
            JMenuItem item = new JMenuItem(menuLabels[i]);
            item.setFont(new Font(FONT_FAMILY, Font.BOLD, 14));
            item.setForeground(TEXT); item.setBackground(PANEL_BG); item.setOpaque(true);
            item.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
            
            item.addActionListener(e -> {
                showPage(targetPage);
                navMenu.setVisible(false); 
            });

            item.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    item.setBackground(NAV_SELECTED); item.setForeground(ACCENT);
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    item.setBackground(PANEL_BG); item.setForeground(TEXT);
                }
            });
            navMenu.add(item);
        }

        JButton gearButton = new JButton("⚙");
        gearButton.setFont(new Font(FONT_FAMILY, Font.BOLD, 24));
        gearButton.setForeground(TEXT); gearButton.setBackground(PANEL_BG);
        gearButton.setOpaque(true); gearButton.setContentAreaFilled(true);
        gearButton.setBorderPainted(false); gearButton.setFocusPainted(false);
        gearButton.setPreferredSize(new Dimension(52, 44));
        gearButton.setBorder(BorderFactory.createLineBorder(BORDER));

        MouseAdapter hoverRadar = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) { navMenu.show(gearButton, 0, gearButton.getHeight()); }
            @Override
            public void mouseExited(MouseEvent e) {
                Point mousePos = MouseInfo.getPointerInfo().getLocation();
                SwingUtilities.convertPointFromScreen(mousePos, navMenu);
                if (!navMenu.contains(mousePos)) navMenu.setVisible(false); 
            }
        };
        gearButton.addMouseListener(hoverRadar);
        return gearButton;
    }

    private JPanel createProfilePage() {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(APP_BG);
        page.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JPanel wrapperPanel = sectionPanel();
        wrapperPanel.setLayout(new BorderLayout(0, 12));

        JLabel titleLabel = sectionTitle("個人生物特徵與目標體重設定");
        wrapperPanel.add(titleLabel, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 15, 6, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        Font labelFont = new Font(FONT_FAMILY, Font.BOLD, 15);
        Font inputFont = new Font(FONT_FAMILY, Font.PLAIN, 15);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.15;
        JLabel nameLabel = new JLabel("使用者暱稱：");
        nameLabel.setFont(labelFont); nameLabel.setForeground(TEXT);
        formPanel.add(nameLabel, gbc);

        gbc.gridx = 1; gbc.weightx = 0.85;
        nameField = new JTextField(); setupInputField(nameField, inputFont);
        formPanel.add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.15;
        JLabel ageLabel = new JLabel("當前年齡：");
        ageLabel.setFont(labelFont); ageLabel.setForeground(TEXT);
        formPanel.add(ageLabel, gbc);

        gbc.gridx = 1; gbc.weightx = 0.85;
        ageField = new JTextField(); setupInputField(ageField, inputFont);
        formPanel.add(ageField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.15;
        JLabel heightLabel = new JLabel("現時身高 (cm)：");
        heightLabel.setFont(labelFont); heightLabel.setForeground(TEXT);
        formPanel.add(heightLabel, gbc);

        gbc.gridx = 1; gbc.weightx = 0.85;
        heightField = new JTextField(); setupInputField(heightField, inputFont);
        formPanel.add(heightField, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.15;
        JLabel weightLabel = new JLabel("現時體重 (kg)：");
        weightLabel.setFont(labelFont); weightLabel.setForeground(TEXT);
        formPanel.add(weightLabel, gbc);

        gbc.gridx = 1; gbc.weightx = 0.85;
        weightField = new JTextField(); setupInputField(weightField, inputFont);
        formPanel.add(weightField, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.15;
        JLabel targetWeightLabel = new JLabel("目標體重 (kg)：");
        targetWeightLabel.setFont(labelFont); targetWeightLabel.setForeground(ACCENT);
        formPanel.add(targetWeightLabel, gbc);

        gbc.gridx = 1; gbc.weightx = 0.85;
        targetWeightField = new JTextField(); setupInputField(targetWeightField, inputFont);
        formPanel.add(targetWeightField, gbc);

        gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 0.15;
        JLabel genderLabel = new JLabel("生物學性別：");
        genderLabel.setFont(labelFont); genderLabel.setForeground(TEXT);
        formPanel.add(genderLabel, gbc);

        gbc.gridx = 1; gbc.weightx = 0.85;
        genderBox = new JComboBox<>(Gender.values());
        genderBox.setFont(inputFont); genderBox.setBackground(CELL_BG); genderBox.setForeground(TEXT);
        genderBox.setBorder(BorderFactory.createLineBorder(BORDER));
        genderBox.addActionListener(e -> triggerLiveScientificCalcs());
        setupComboBoxTheme(genderBox);
        formPanel.add(genderBox, gbc);

        gbc.gridx = 0; gbc.gridy = 6; gbc.weightx = 0.15;
        JLabel goalLabel = new JLabel("體態核心目標：");
        goalLabel.setFont(labelFont); goalLabel.setForeground(TEXT);
        formPanel.add(goalLabel, gbc);

        gbc.gridx = 1; gbc.weightx = 0.85;
        goalBox = new JComboBox<>(FitnessGoal.values());
        goalBox.setFont(inputFont); goalBox.setBackground(CELL_BG); goalBox.setForeground(TEXT);
        goalBox.setBorder(BorderFactory.createLineBorder(BORDER));
        goalBox.addActionListener(e -> triggerLiveScientificCalcs());
        setupComboBoxTheme(goalBox);
        formPanel.add(goalBox, gbc);

        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 2; gbc.insets = new Insets(12, 15, 5, 15);
        JPanel calcReportPanel = new JPanel(new GridLayout(2, 2, 15, 10));
        calcReportPanel.setBackground(CELL_BG);
        calcReportPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1), BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));

        bmiLiveLabel = createReportBlock(calcReportPanel, "身體質量指數 (BMI)");
        bmrLiveLabel = createReportBlock(calcReportPanel, "基礎代謝率 (BMR)");
        tdeeLiveLabel = createReportBlock(calcReportPanel, "每日總熱量消耗 (TDEE)");
        recommendCalLabel = createReportBlock(calcReportPanel, "每日建議熱量攝取 / 消耗推薦");
        recommendCalLabel.setForeground(new Color(46, 204, 113)); 

        formPanel.add(calcReportPanel, gbc);

        DocumentListener liveEngine = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { triggerLiveScientificCalcs(); }
            public void removeUpdate(DocumentEvent e) { triggerLiveScientificCalcs(); }
            public void changedUpdate(DocumentEvent e) { triggerLiveScientificCalcs(); }
        };
        ageField.getDocument().addDocumentListener(liveEngine);
        heightField.getDocument().addDocumentListener(liveEngine);
        weightField.getDocument().addDocumentListener(liveEngine);

        wrapperPanel.add(formPanel, BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 5));
        btnRow.setOpaque(false);
        JButton saveBtn = new JButton("儲存修改並更新冒險者狀態");
        saveBtn.setFont(new Font(FONT_FAMILY, Font.BOLD, 16));
        applySolidButtonStyle(saveBtn, CTA_BLUE, Color.WHITE);
        saveBtn.setBorder(BorderFactory.createEmptyBorder(10, 40, 10, 40));
        saveBtn.addActionListener(e -> handleProfileSave());
        btnRow.add(saveBtn);
        
        wrapperPanel.add(btnRow, BorderLayout.SOUTH);
        page.add(wrapperPanel, BorderLayout.CENTER);
        return page;
    }

    private <T> void setupComboBoxTheme(JComboBox<T> box) {
        box.setRenderer(new ListCellRenderer<T>() {
            @Override
            public Component getListCellRendererComponent(JList<? extends T> list, T value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = new JLabel(value != null ? value.toString() : "");
                lbl.setOpaque(true); lbl.setFont(new Font(FONT_FAMILY, Font.PLAIN, 14));
                lbl.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
                if (isSelected) {
                    lbl.setBackground(NAV_SELECTED); lbl.setForeground(ACCENT);
                } else {
                    lbl.setBackground(CELL_BG); lbl.setForeground(TEXT);
                }
                return lbl;
            }
        });
    }

    private void setupInputField(JTextField field, Font font) {
        field.setFont(font); field.setBackground(CELL_BG); field.setForeground(TEXT); field.setCaretColor(TEXT);
        field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER), BorderFactory.createEmptyBorder(6, 10, 6, 10)));
    }

    private void triggerLiveScientificCalcs() {
        try {
            double w = Double.parseDouble(weightField.getText().trim());
            double h = Double.parseDouble(heightField.getText().trim());
            int age = Integer.parseInt(ageField.getText().trim());
            Gender g = (Gender) genderBox.getSelectedItem();
            FitnessGoal goal = (FitnessGoal) goalBox.getSelectedItem();

            double bmi = w / ((h / 100.0) * h / 100.0);
            bmiLiveLabel.setText(String.format("%.1f (身體質量)", bmi));

            double bmr = (g == Gender.MALE) ? (10 * w) + (6.25 * h) - (5 * age) + 5 : (10 * w) + (6.25 * h) - (5 * age) - 161;
            bmrLiveLabel.setText(String.format("%.1f 大卡", bmr));

            double tdee = bmr * 1.375;
            tdeeLiveLabel.setText(String.format("%.1f 大卡", tdee));

            double recommend = (goal == FitnessGoal.FAT_LOSS) ? (tdee - 400.0) : (tdee + 300.0);
            recommendCalLabel.setText(String.format("%.1f 大卡 / 日", recommend));
        } catch (Exception ex) {
            String waitStr = "等待合法輸入...";
            bmiLiveLabel.setText(waitStr); bmrLiveLabel.setText(waitStr); tdeeLiveLabel.setText(waitStr); recommendCalLabel.setText(waitStr);
        }
    }

    private void handleProfileSave() {
        String inputName = nameField.getText().trim();
        String rawAge = ageField.getText().trim();
        String rawHeight = heightField.getText().trim();
        String rawWeight = weightField.getText().trim();
        String rawTarget = targetWeightField.getText().trim();
        Gender selectedGender = (Gender) genderBox.getSelectedItem();
        FitnessGoal selectedGoal = (FitnessGoal) goalBox.getSelectedItem();

        if (inputName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "暱稱欄位不能留白！", "輸入錯誤", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            int parsedAge = Integer.parseInt(rawAge);
            double parsedHeight = Double.parseDouble(rawHeight);
            double parsedWeight = Double.parseDouble(rawWeight);
            double parsedTarget = Double.parseDouble(rawTarget);

            if (parsedAge <= 0 || parsedHeight <= 0 || parsedWeight <= 0 || parsedTarget <= 0) {
                JOptionPane.showMessageDialog(this, "輸入數值必須大於 0！", "數據異常", JOptionPane.ERROR_MESSAGE);
                return;
            }

            var avatar = player.getAvatar();
            avatar.setName(inputName); 
            avatar.getProfile().setHeight(parsedHeight);
            avatar.getProfile().setWeight(parsedWeight);
            avatar.getProfile().setGender(selectedGender);
            
            player.setAge(parsedAge);
            player.setTargetWeight(parsedTarget);
            player.setFitnessGoal(selectedGoal);

            saveAndRefresh();
            JOptionPane.showMessageDialog(this, "個人特徵、年齡與體態規劃目標已成功同步存檔！", "更新成功", JOptionPane.INFORMATION_MESSAGE);
            showPage("home"); 

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "請檢查輸入格式是否全為合法數字！", "格式錯誤", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel createHomePage() {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(APP_BG);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(APP_BG);
        content.setBorder(BorderFactory.createEmptyBorder(6, 18, 18, 18)); 

        JPanel progressHeaderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        progressHeaderPanel.setOpaque(false);
        weightProgressLabel = new JLabel("距離目標體重還差：計算中...");
        weightProgressLabel.setFont(new Font(FONT_FAMILY, Font.BOLD, 15));
        weightProgressLabel.setForeground(ACCENT);
        progressHeaderPanel.add(weightProgressLabel);

        JPanel leftAvatarContainer = new JPanel(new BorderLayout(0, 10));
        leftAvatarContainer.setOpaque(false);
        leftAvatarContainer.add(progressHeaderPanel, BorderLayout.NORTH); 
        leftAvatarContainer.add(avatarPanel, BorderLayout.CENTER);

        JPanel dualPanel = new JPanel(new BorderLayout(18, 0));
        dualPanel.setOpaque(false);
        dualPanel.add(leftAvatarContainer, BorderLayout.WEST); 
        dualPanel.add(createDashboard(), BorderLayout.CENTER); 

        content.add(dualPanel);
        content.add(gap(18));

        JPanel bottomBtnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        bottomBtnRow.setOpaque(false);

        JButton startButton = new JButton("紀錄運動");
        startButton.setFont(new Font(FONT_FAMILY, Font.BOLD, 20));
        applySolidButtonStyle(startButton, CTA_BLUE, Color.WHITE);
        startButton.setBorder(BorderFactory.createEmptyBorder(14, 64, 14, 64));
        startButton.addActionListener(event -> new WorkoutSessionDialog(this).setVisible(true));
        bottomBtnRow.add(startButton);

        JButton clearDataButton = new JButton("重置並清空所有資料");
        clearDataButton.setFont(new Font(FONT_FAMILY, Font.BOLD, 14));
        applySolidButtonStyle(clearDataButton, RESET_RED, Color.WHITE);
        clearDataButton.setBorder(BorderFactory.createEmptyBorder(14, 24, 14, 24));
        clearDataButton.addActionListener(event -> performDataPurgeWithConfirmation());
        bottomBtnRow.add(clearDataButton);

        content.add(bottomBtnRow);

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(APP_BG); scrollPane.getViewport().setBackground(APP_BG);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        page.add(scrollPane, BorderLayout.CENTER);
        return page;
    }

    private JPanel createDashboard() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.add(createWorkoutChartSection());
        content.add(gap(14));
        content.add(createHistoryPanel());
        return content;
    }

    private JPanel createWorkoutChartSection() {
        JPanel panel = wideSectionPanel();
        panel.setLayout(new BorderLayout(0, 12));
        panel.setPreferredSize(new Dimension(500, 310));

        JPanel topHeader = new JPanel(new BorderLayout());
        topHeader.setOpaque(false);

        JLabel titleLabel = sectionTitle("運動紀錄趨勢");
        topHeader.add(titleLabel, BorderLayout.WEST);

        JPanel scaleSelectorRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        scaleSelectorRow.setOpaque(false);
        addScaleButton(scaleSelectorRow, "WEEK", "週規模");
        addScaleButton(scaleSelectorRow, "MONTH", "月規模");
        addScaleButton(scaleSelectorRow, "YEAR", "年規模");
        topHeader.add(scaleSelectorRow, BorderLayout.EAST);

        panel.add(topHeader, BorderLayout.NORTH);
        panel.add(chartPanel, BorderLayout.CENTER); 
        return panel;
    }

    private void addScaleButton(JPanel container, String scaleKey, String label) {
        JButton btn = new JButton(label);
        btn.setFont(new Font(FONT_FAMILY, Font.BOLD, 12)); btn.setFocusPainted(false);
        applySolidButtonStyle(btn, BUTTON_BG, TEXT);
        btn.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        
        btn.addActionListener(e -> {
            chartPanel.setScaleMode(scaleKey, workouts); 
            for (Map.Entry<String, JButton> entry : scaleButtons.entrySet()) {
                boolean isTarget = entry.getKey().equals(scaleKey);
                entry.getValue().setBackground(isTarget ? NAV_SELECTED : BUTTON_BG);
                entry.getValue().setForeground(isTarget ? ACCENT : MUTED);
            }
        });
        scaleButtons.put(scaleKey, btn);
        container.add(btn);
    }

    private JPanel createHistoryPanel() {
        JPanel panel = sectionPanel();
        panel.setLayout(new BorderLayout(0, 10));
        panel.setPreferredSize(new Dimension(500, 290));
        panel.add(sectionTitle("歷史運動紀錄"), BorderLayout.NORTH);

        JTable table = new JTable(historyModel);
        table.setRowHeight(30); table.setBackground(CELL_BG); table.setForeground(TEXT); table.setGridColor(BORDER);
        table.setFont(new Font(FONT_FAMILY, Font.PLAIN, 13)); table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setDefaultRenderer((table1, value, isSelected, hasFocus, row, column) -> {
            JLabel headerLabel = new JLabel(String.valueOf(value), SwingConstants.LEFT);
            headerLabel.setOpaque(true); headerLabel.setBackground(PANEL_BG); headerLabel.setForeground(TEXT);     
            headerLabel.setFont(new Font(FONT_FAMILY, Font.BOLD, 13));
            headerLabel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 1, BORDER),
                    BorderFactory.createEmptyBorder(6, 8, 6, 8)
            ));
            return headerLabel;
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER));
        scrollPane.getViewport().setBackground(CELL_BG);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }
    
    private void showPage(String pageName) { pageLayout.show(pageContainer, pageName); }

    public void saveAndRefresh() {
        storage.savePlayer(player);
        storage.saveWorkouts(workouts);
        this.workouts = new ArrayList<>(storage.loadWorkouts()); 
        refreshAll();
    }

    private int lastKnownLevel = -1; 

    private void refreshAll() {
        if (scaleButtons.containsKey("WEEK") && scaleButtons.get("WEEK").getBackground().equals(BUTTON_BG)) {
            scaleButtons.get("WEEK").doClick();
        }
        
        int currentLevel = player.level();
        if (lastKnownLevel == -1) {
            lastKnownLevel = currentLevel; 
        } else if (currentLevel > lastKnownLevel) {
            lastKnownLevel = currentLevel;
            
            JOptionPane.showMessageDialog(this,
                    "恭喜您突破基因鎖！等級提升至 Lv." + currentLevel + " \n系統已為您全面重組肌肉上限、並演進基礎能力指標！",
                    "FITQUEST 等級突破通知",
                    JOptionPane.INFORMATION_MESSAGE);
        }

        List<fithero.model.achievement.Achievement> newlyUnlocked = player.triggerAchievementCheck();
        
        if (newlyUnlocked != null && !newlyUnlocked.isEmpty()) {
            for (fithero.model.achievement.Achievement ach : newlyUnlocked) {
                String unlockMessage = "榮譽解鎖：【" + ach.getTitle() + "】\n"
                                    + "難度級別：[" + ach.getDifficulty() + "]\n"
                                    + "達成條件：" + ach.getDescription();
                
                JOptionPane.showMessageDialog(this,
                        unlockMessage,
                        "🏆 FITQUEST 榮譽成就達成",
                        JOptionPane.INFORMATION_MESSAGE);
            }
            
            if (achievementWallPage != null) {
                pageContainer.remove(achievementWallPage);
                achievementWallPage = new AchievementWallPage(this.player);
                pageContainer.add(achievementWallPage, "analytics");
                pageContainer.revalidate();
            }
        }

        avatarPanel.setPlayer(player); 
        
        String activeScale = "WEEK";
        for (Map.Entry<String, JButton> entry : scaleButtons.entrySet()) {
            if (entry.getValue().getBackground().equals(NAV_SELECTED)) {
                activeScale = entry.getKey();
                break;
            }
        }
        chartPanel.setScaleMode(activeScale, workouts);
        refreshHistory();

        if (player != null && weightProgressLabel != null) {
            weightProgressLabel.setText(player.getWeightProgressString());
        }

        if (player != null && nameField != null) {
            var prof = player.getAvatar().getProfile();
            nameField.setText(player.getAvatar().getName());
            ageField.setText(String.valueOf(player.getAge())); 
            heightField.setText(String.valueOf(prof.getHeight()));
            weightField.setText(String.valueOf(prof.getWeight()));
            targetWeightField.setText(String.valueOf(player.getTargetWeight()));
            genderBox.setSelectedItem(prof.getGender());
            goalBox.setSelectedItem(player.getFitnessGoal()); 
            triggerLiveScientificCalcs(); 
        }
    }

    private void refreshHistory() {
        historyModel.setRowCount(0); 
        workouts.stream()
                .sorted(Comparator.comparing(WorkoutEntry::time).reversed())
                .limit(30) 
                .forEach(entry -> {
                    String name = entry.getExerciseName(); 
                    var exInfo = ExerciseRegistry.getExercise(name);
                    String amountDisplay = ""; String setsDisplay = "";

                    if (exInfo != null && exInfo.isAerobic()) {
                        amountDisplay = entry.amount() + " 分鐘"; setsDisplay = "心肺有氧";
                    } else {
                        if (entry.weight() == 0) {
                            amountDisplay = entry.amount() + " 下 (自重)";
                        } else {
                            amountDisplay = String.format("%.1f", entry.weight()) + "kg × " + entry.amount() + "下";
                        }
                        setsDisplay = entry.sets() + " 組";
                    }
                    historyModel.addRow(new Object[] { entry.displayTime(), name, amountDisplay, setsDisplay });
                });
    }

    private void performDataPurgeWithConfirmation() {
        int choice = JOptionPane.showConfirmDialog(this, 
                "確定要清空所有測試資料嗎？\n這將重置玩家等級、歸零肌肉量、抹除所有歷史運動紀錄，\n並且【全數鎖定】100 項榮譽成就牆紀錄與行事曆計畫！", 
                "資料徹底刪除警告", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        
        if (choice == JOptionPane.YES_OPTION) {
            var currentAvatar = player.getAvatar();
            String currentName = currentAvatar.getName();
            double h = currentAvatar.getProfile().getHeight();
            double w = currentAvatar.getProfile().getWeight();
            var gender = currentAvatar.getProfile().getGender();

            this.workouts.clear();
            
            try {
                java.nio.file.Path achFile = java.nio.file.Path.of("data", "unlocked_achievements.properties");
                java.nio.file.Files.deleteIfExists(achFile);
                java.nio.file.Path planFile = java.nio.file.Path.of("data", "custom_plans.properties");
                java.nio.file.Files.deleteIfExists(planFile);
                System.out.println("[沙盒重置中心] 已成功物理抹除硬碟成就與日曆設定檔。");
            } catch (java.io.IOException ex) {
                System.err.println("[沙盒重置中心] 刪除存檔失敗: " + ex.getMessage());
            }

            this.player = new PlayerState(currentName, h, w, gender);
            this.lastKnownLevel = 1; 

            if (achievementWallPage != null) pageContainer.remove(achievementWallPage);
            if (calendarPage != null) pageContainer.remove(calendarPage);

            this.achievementWallPage = new AchievementWallPage(this.player);
            this.calendarPage = new CalendarPage(this, this.workouts);

            pageContainer.add(achievementWallPage, "analytics");
            pageContainer.add(calendarPage, "calendar");
            
            pageContainer.revalidate();
            pageContainer.repaint();
            
            saveAndRefresh();
            
            JOptionPane.showMessageDialog(this, "測試沙盒、行事曆與 100 項成就紀錄已成功清空並徹底重置！", "重置成功", JOptionPane.INFORMATION_MESSAGE);
            showPage("home"); 
        }
    }

    private JPanel sectionPanel() {
        JPanel panel = new JPanel(); panel.setBackground(PANEL_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER), BorderFactory.createEmptyBorder(16, 16, 16, 16)));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    private JPanel wideSectionPanel() {
        JPanel panel = new JPanel(); panel.setBackground(PANEL_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER), BorderFactory.createEmptyBorder(18, 18, 18, 18)));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    private JLabel sectionTitle(String text) {
        label = new JLabel(text);
        label.setFont(new Font(FONT_FAMILY, Font.BOLD, 18)); label.setForeground(TEXT);
        return label;
    }
    private JLabel label; 

    private void applySolidButtonStyle(JButton button, Color background, Color foreground) {
        button.setForeground(foreground); button.setBackground(background);
        button.setOpaque(true); button.setContentAreaFilled(true); button.setBorderPainted(false); button.setFocusPainted(false);
    }

    private Component gap(int height) { return Box.createRigidArea(new Dimension(1, height)); }
    private JPanel createPlaceholderPage(String title, String body) {
        JPanel page = new JPanel(new BorderLayout()); page.setBackground(APP_BG);
        page.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        JPanel panel = sectionPanel(); panel.setLayout(new BorderLayout(0, 12));
        JLabel titleLabel = sectionTitle(title);
        JLabel bodyLabel = new JLabel(body, SwingConstants.CENTER);
        bodyLabel.setFont(new Font(FONT_FAMILY, Font.PLAIN, 22)); bodyLabel.setForeground(MUTED);
        panel.add(titleLabel, BorderLayout.NORTH); panel.add(bodyLabel, BorderLayout.CENTER);
        page.add(panel, BorderLayout.CENTER);
        return page;
    }

    private JLabel createReportBlock(JPanel parent, String title) {
        JPanel sub = new JPanel(new BorderLayout()); 
        sub.setOpaque(false);
        JLabel titleL = new JLabel(title); 
        titleL.setFont(new Font(FONT_FAMILY, Font.BOLD, 12)); 
        titleL.setForeground(MUTED);
        JLabel valL = new JLabel("0.0", SwingConstants.LEFT); 
        valL.setFont(new Font(FONT_FAMILY, Font.BOLD, 18)); 
        valL.setForeground(ACCENT);
        sub.add(titleL, BorderLayout.NORTH); 
        sub.add(valL, BorderLayout.CENTER);
        parent.add(sub);
        return valL;
    }
}