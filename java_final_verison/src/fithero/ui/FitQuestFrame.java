package fithero.ui;

import fithero.infra.Storage;
import fithero.logic.manager.PlayerState;
import fithero.logic.manager.FitnessGoal;
import fithero.model.achievement.Achievement;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

public class FitQuestFrame extends JFrame {
    private static final Color APP_BG = new Color(0x1e222b);
    private static final Color PANEL_BG = new Color(0x282c37);
    private static final Color CELL_BG = new Color(0x20242d);
    private static final Color TEXT = new Color(0xf4f6fb);
    private static final Color MUTED = new Color(0xb7c0d1);
    private static final Color ACCENT = new Color(0x5aa9ff);
    private static final Color CTA_BLUE = new Color(59, 130, 246);
    private static final Color NAV_SELECTED = new Color(0x3a4354);
    private static final Color BUTTON_BG = new Color(40, 44, 55);
    private static final Color BORDER = new Color(0x3a4050);

    private static final String FONT_FAMILY = "Microsoft JhengHei";
    private static final int HOME_RECENT_RECORD_LIMIT = 5;
    private static final int DAILY_TRAINING_VOLUME_TARGET = 1000;
    private static final int DAILY_TRAINING_DANGER_THRESHOLD = 3000;
    private static final int TODAY_SET_TARGET = 3;
    private static final int TODAY_MUSCLE_TARGET = 2;

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
    private JLabel todayVolumeLabel;
    private JLabel todaySetsLabel;
    private JLabel todayMusclesLabel;
    private JLabel todayStatusLabel;
    private JLabel todayTaskSetsLabel;
    private JLabel todayTaskVolumeLabel;
    private JLabel todayTaskMusclesLabel;
    private JProgressBar todayVolumeProgress;
    private final CardLayout recentRecordsLayout = new CardLayout();
    private final JPanel recentRecordsCards = new JPanel(recentRecordsLayout);

    private JTextField nameField;
    private JTextField ageField; 
    private JTextField heightField;
    private JTextField weightField;
    private JTextField fatField; 
    private JTextField targetWeightField; 
    private JComboBox<Gender> genderBox;
    private JComboBox<FitnessGoal> goalBox; 

    private JLabel bmiLiveLabel;
    private JLabel bmrLiveLabel;
    private JLabel tdeeLiveLabel;
    private JLabel recommendCalLabel; 
    private JLabel muscleLiveLabel; 

    private final DefaultTableModel historyModel = new DefaultTableModel(
            new String[] {"時間", "訓練項目", "數量/時間", "訓練組數"}, 0
    ) {
        @Override public boolean isCellEditable(int row, int column) { return false; } 
    };

    public PlayerState getPlayerState() { return this.player; }
    public List<WorkoutEntry> getWorkoutsList() { return this.workouts; }
    public CalendarPage getCalendarPage() { return this.calendarPage; }

    public FitQuestFrame() {
        super("FitQuest - 遊戲化健身養成系統");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1320, 920); 
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

        this.calendarPage.runBootUpStreakCheck(player);
        
        // 啟動時先行鎖定當前等級快取線，防範初次載入閃窗
        this.lastKnownLevel = player.level();
        
        refreshAll(); 
        showPage("home");
        setLocationRelativeTo(null); 
    }

    private JButton createHoverNavigationGear() {
        JPopupMenu navMenu = new JPopupMenu();
        navMenu.setBackground(PANEL_BG); navMenu.setBorder(BorderFactory.createLineBorder(BORDER));

        String[] menuLabels = {"首頁", "運動行事曆", "榮譽成就牆", "個人資料"};
        String[] menuKeys = {"home", "calendar", "analytics", "profile"};

        for (int i = 0; i < menuLabels.length; i++) {
            final String targetPage = menuKeys[i];
            JMenuItem item = new JMenuItem(menuLabels[i]);
            item.setFont(new Font(FONT_FAMILY, Font.BOLD, 14));
            item.setForeground(TEXT); item.setBackground(PANEL_BG); item.setOpaque(true);
            item.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
            item.addActionListener(e -> { showPage(targetPage); navMenu.setVisible(false); });
            item.addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { item.setBackground(NAV_SELECTED); item.setForeground(ACCENT); }
                @Override public void mouseExited(MouseEvent e) { item.setBackground(PANEL_BG); item.setForeground(TEXT); }
            });
            navMenu.add(item);
        }

        JButton gearButton = new JButton("選單");
        gearButton.setFont(new Font(FONT_FAMILY, Font.BOLD, 13));
        gearButton.setForeground(TEXT); gearButton.setBackground(PANEL_BG);
        gearButton.setOpaque(true); gearButton.setContentAreaFilled(true); gearButton.setBorderPainted(false); gearButton.setFocusPainted(false);
        gearButton.setPreferredSize(new Dimension(64, 44)); gearButton.setBorder(BorderFactory.createLineBorder(BORDER));

        gearButton.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { navMenu.show(gearButton, 0, gearButton.getHeight()); }
        });
        return gearButton;
    }

    private JPanel createProfilePage() {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(APP_BG); page.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JPanel wrapperPanel = sectionPanel(); wrapperPanel.setLayout(new BorderLayout(0, 12));
        wrapperPanel.add(sectionTitle("個人生物特徵與目標體重設定"), BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridBagLayout()); formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 15, 4, 15); gbc.fill = GridBagConstraints.HORIZONTAL;

        Font labelFont = new Font(FONT_FAMILY, Font.BOLD, 14);
        Font inputFont = new Font(FONT_FAMILY, Font.PLAIN, 14);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.15;
        JLabel nameLabel = new JLabel("使用者暱稱："); nameLabel.setFont(labelFont); nameLabel.setForeground(TEXT);
        formPanel.add(nameLabel, gbc);
        gbc.gridx = 1; gbc.weightx = 0.85;
        nameField = new JTextField(); setupInputField(nameField, inputFont); formPanel.add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.15;
        JLabel ageLabel = new JLabel("當前年齡："); ageLabel.setFont(labelFont); ageLabel.setForeground(TEXT);
        formPanel.add(ageLabel, gbc);
        gbc.gridx = 1; gbc.weightx = 0.85;
        ageField = new JTextField(); setupInputField(ageField, inputFont); formPanel.add(ageField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.15;
        JLabel heightLabel = new JLabel("現時身高 (cm)："); heightLabel.setFont(labelFont); heightLabel.setForeground(TEXT);
        formPanel.add(heightLabel, gbc);
        gbc.gridx = 1; gbc.weightx = 0.85;
        heightField = new JTextField(); setupInputField(heightField, inputFont); formPanel.add(heightField, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.15;
        JLabel weightLabel = new JLabel("現時體重 (kg)："); weightLabel.setFont(labelFont); weightLabel.setForeground(TEXT);
        formPanel.add(weightLabel, gbc);
        gbc.gridx = 1; gbc.weightx = 0.85;
        weightField = new JTextField(); setupInputField(weightField, inputFont); formPanel.add(weightField, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.15;
        JLabel fatLabel = new JLabel("體脂肪率 (%, 選填)："); fatLabel.setFont(labelFont); fatLabel.setForeground(ACCENT);
        formPanel.add(fatLabel, gbc);
        gbc.gridx = 1; gbc.weightx = 0.85;
        fatField = new JTextField(); setupInputField(fatField, inputFont); formPanel.add(fatField, gbc);

        gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 0.15;
        JLabel targetWeightLabel = new JLabel("目標體重 (kg)："); targetWeightLabel.setFont(labelFont); targetWeightLabel.setForeground(TEXT);
        formPanel.add(targetWeightLabel, gbc);
        gbc.gridx = 1; gbc.weightx = 0.85;
        targetWeightField = new JTextField(); setupInputField(targetWeightField, inputFont); formPanel.add(targetWeightField, gbc);

        gbc.gridx = 0; gbc.gridy = 6; gbc.weightx = 0.15;
        JLabel genderLabel = new JLabel("生物學性別："); genderLabel.setFont(labelFont); genderLabel.setForeground(TEXT);
        formPanel.add(genderLabel, gbc);
        gbc.gridx = 1; gbc.weightx = 0.85;
        genderBox = new JComboBox<>(Gender.values()); genderBox.setFont(inputFont); setupComboBoxTheme(genderBox);
        genderBox.addActionListener(e -> triggerLiveScientificCalcs()); formPanel.add(genderBox, gbc);

        gbc.gridx = 0; gbc.gridy = 7; gbc.weightx = 0.15;
        JLabel goalLabel = new JLabel("體態核心目標："); goalLabel.setFont(labelFont); goalLabel.setForeground(TEXT);
        formPanel.add(goalLabel, gbc);
        gbc.gridx = 1; gbc.weightx = 0.85;
        goalBox = new JComboBox<>(FitnessGoal.values()); goalBox.setFont(inputFont); setupComboBoxTheme(goalBox);
        goalBox.addActionListener(e -> triggerLiveScientificCalcs()); formPanel.add(goalBox, gbc);

        gbc.gridx = 0; gbc.gridy = 8; gbc.gridwidth = 2; gbc.insets = new Insets(10, 15, 5, 15);
        JPanel calcReportPanel = new JPanel(new GridLayout(3, 2, 15, 8)); 
        calcReportPanel.setBackground(CELL_BG);
        calcReportPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER, 1), BorderFactory.createEmptyBorder(10, 14, 10, 14)));

        bmiLiveLabel = createReportBlock(calcReportPanel, "身體質量指數 (BMI)");
        bmrLiveLabel = createReportBlock(calcReportPanel, "基礎代謝率 (BMR) [雙引擎自適應]");
        tdeeLiveLabel = createReportBlock(calcReportPanel, "每日總熱量消耗 (TDEE) [智慧動態滾動]");
        recommendCalLabel = createReportBlock(calcReportPanel, "每日建議熱量攝取");
        muscleLiveLabel = createReportBlock(calcReportPanel, "全身預估精準肌肉量 (Muscle Mass)"); 
        muscleLiveLabel.setForeground(new Color(241, 196, 15)); 

        formPanel.add(calcReportPanel, gbc);

        DocumentListener liveEngine = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { triggerLiveScientificCalcs(); }
            public void removeUpdate(DocumentEvent e) { triggerLiveScientificCalcs(); }
            public void changedUpdate(DocumentEvent e) { triggerLiveScientificCalcs(); }
        };
        ageField.getDocument().addDocumentListener(liveEngine);
        heightField.getDocument().addDocumentListener(liveEngine);
        weightField.getDocument().addDocumentListener(liveEngine);
        fatField.getDocument().addDocumentListener(liveEngine); 

        wrapperPanel.add(formPanel, BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 5)); btnRow.setOpaque(false);
        JButton saveBtn = new JButton("儲存修改並更新冒險者狀態");
        saveBtn.setFont(new Font(FONT_FAMILY, Font.BOLD, 16)); applySolidButtonStyle(saveBtn, CTA_BLUE, Color.WHITE);
        saveBtn.setBorder(BorderFactory.createEmptyBorder(10, 40, 10, 40));
        saveBtn.addActionListener(e -> handleProfileSave());
        btnRow.add(saveBtn);
        
        wrapperPanel.add(btnRow, BorderLayout.SOUTH);
        page.add(wrapperPanel, BorderLayout.CENTER);
        return page;
    }

    private JLabel createReportBlock(JPanel parent, String title) {
        JPanel sub = new JPanel(new BorderLayout()); sub.setOpaque(false);
        JLabel titleL = new JLabel(title); titleL.setFont(new Font(FONT_FAMILY, Font.BOLD, 12)); titleL.setForeground(MUTED);
        JLabel valL = new JLabel("0.0", SwingConstants.LEFT); valL.setFont(new Font(FONT_FAMILY, Font.BOLD, 16)); valL.setForeground(ACCENT);
        sub.add(titleL, BorderLayout.NORTH); sub.add(valL, BorderLayout.CENTER);
        parent.add(sub);
        return valL;
    }

    private <T> void setupComboBoxTheme(JComboBox<T> box) {
        box.setBackground(CELL_BG); box.setForeground(TEXT); box.setBorder(BorderFactory.createLineBorder(BORDER));
        box.setRenderer(new ListCellRenderer<T>() {
            @Override public Component getListCellRendererComponent(JList<? extends T> list, T value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = new JLabel(value != null ? value.toString() : ""); lbl.setOpaque(true);
                lbl.setFont(new Font(FONT_FAMILY, Font.PLAIN, 14)); lbl.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
                lbl.setBackground(isSelected ? NAV_SELECTED : CELL_BG); lbl.setForeground(isSelected ? ACCENT : TEXT);
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
            int parsedAge = Integer.parseInt(ageField.getText().trim());
            Gender g = (Gender) genderBox.getSelectedItem();
            FitnessGoal goal = (FitnessGoal) goalBox.getSelectedItem();

            String rawFat = fatField.getText().trim();
            double fat = rawFat.isEmpty() ? 0.0 : Double.parseDouble(rawFat);

            PlayerState tempState = new PlayerState("Temp", h, w, g);
            tempState.setAge(parsedAge);
            tempState.setBodyFatPercent(fat);
            tempState.setFitnessGoal(goal);

            bmiLiveLabel.setText(String.format("%.1f (身體質量)", tempState.calculateBMI()));
            bmrLiveLabel.setText(String.format("%.1f 大卡 (%s)", tempState.calculateBMR(), (fat > 0.0 ? "Katch精準引擎" : "Mifflin引擎")));
            
            double tdee = tempState.calculateTDEE(workouts);
            tdeeLiveLabel.setText(String.format("%.1f 大卡 (過去7天滾動活性加權)", tdee));
            recommendCalLabel.setText(String.format("%.1f 大卡 / 日", tempState.calculateRecommendedCalories(workouts)));
            muscleLiveLabel.setText(String.format("%.1f kg (全身預估淨肌肉骨骼總重)", tempState.estimateMuscleMass()));

        } catch (Exception ex) {
            String waitStr = "等待合法輸入...";
            bmiLiveLabel.setText(waitStr); bmrLiveLabel.setText(waitStr); tdeeLiveLabel.setText(waitStr); recommendCalLabel.setText(waitStr); muscleLiveLabel.setText(waitStr);
        }
    }

    private void handleProfileSave() {
        String inputName = nameField.getText().trim();
        if (inputName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "暱稱欄位不能留白！", "輸入錯誤", JOptionPane.ERROR_MESSAGE); return;
        }
        try {
            int parsedAge = Integer.parseInt(ageField.getText().trim());
            double parsedHeight = Double.parseDouble(heightField.getText().trim());
            double parsedWeight = Double.parseDouble(weightField.getText().trim());
            double parsedTarget = Double.parseDouble(targetWeightField.getText().trim());
            
            String rawFat = fatField.getText().trim();
            double parsedFat = rawFat.isEmpty() ? 0.0 : Double.parseDouble(rawFat);

            if (parsedAge <= 0 || parsedHeight <= 0 || parsedWeight <= 0 || parsedTarget <= 0 || parsedFat < 0.0) {
                JOptionPane.showMessageDialog(this, "數值輸入異常！", "錯誤", JOptionPane.ERROR_MESSAGE); return;
            }

            var avatar = player.getAvatar();
            avatar.setName(inputName); 
            avatar.getProfile().setHeight(parsedHeight);
            avatar.getProfile().setWeight(parsedWeight);
            avatar.getProfile().setGender((Gender) genderBox.getSelectedItem());
            
            player.setAge(parsedAge);
            player.setTargetWeight(parsedTarget);
            player.setBodyFatPercent(parsedFat); 
            player.setFitnessGoal((FitnessGoal) goalBox.getSelectedItem());

            saveAndRefresh();
            JOptionPane.showMessageDialog(this, "科學身體特徵規劃目標已永久同步存檔！", "更新成功", JOptionPane.INFORMATION_MESSAGE);
            showPage("home"); 
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "請檢查欄位格式！", "格式錯誤", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel createHomePage() {
        JPanel page = new JPanel(new BorderLayout()); page.setBackground(APP_BG);
        JPanel content = new JPanel(); content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(APP_BG); content.setBorder(BorderFactory.createEmptyBorder(6, 18, 18, 18)); 

        JPanel heroRow = new JPanel(new BorderLayout(18, 0));
        heroRow.setOpaque(false);
        heroRow.add(buildCharacterPanel(), BorderLayout.WEST);
        heroRow.add(buildHomeRightColumn(), BorderLayout.CENTER);

        content.add(heroRow);
        content.add(gap(18));
        content.add(buildRecentRecordsPanel());
        content.add(gap(14));
        content.add(buildActionButtonPanel());

        JScrollPane scrollPane = new JScrollPane(content); scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(APP_BG); scrollPane.getViewport().setBackground(APP_BG);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        page.add(scrollPane, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildCharacterPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(610, 738));

        JPanel progressHeaderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); progressHeaderPanel.setOpaque(false);
        weightProgressLabel = new JLabel("距離目標體重還差：計算中...");
        weightProgressLabel.setFont(new Font(FONT_FAMILY, Font.BOLD, 15)); weightProgressLabel.setForeground(ACCENT);
        progressHeaderPanel.add(weightProgressLabel);

        panel.add(progressHeaderPanel, BorderLayout.NORTH);
        panel.add(avatarPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildHomeRightColumn() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.add(buildTrendPanel());
        content.add(gap(14));
        content.add(buildTodayMissionPanel());
        return content;
    }

    private JPanel buildTrendPanel() {
        JPanel panel = wideSectionPanel();
        panel.setLayout(new BorderLayout(0, 10));
        panel.setPreferredSize(new Dimension(500, 390));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 390));

        JPanel topHeader = new JPanel(new BorderLayout()); topHeader.setOpaque(false);
        JPanel titleBox = new JPanel();
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.setOpaque(false);
        titleBox.add(sectionTitle("每日訓練量趨勢"));
        JLabel unitHint = new JLabel("重訓量 = 重量 × 次數 × 組數；自重與有氧以完成量計入");
        unitHint.setFont(new Font(FONT_FAMILY, Font.PLAIN, 11)); unitHint.setForeground(MUTED);
        titleBox.add(Box.createRigidArea(new Dimension(1, 3))); titleBox.add(unitHint);
        topHeader.add(titleBox, BorderLayout.WEST);

        JPanel scaleSelectorRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0)); scaleSelectorRow.setOpaque(false);
        addScaleButton(scaleSelectorRow, "WEEK", "週規模"); addScaleButton(scaleSelectorRow, "MONTH", "月規模"); addScaleButton(scaleSelectorRow, "YEAR", "年規模");
        topHeader.add(scaleSelectorRow, BorderLayout.EAST);

        panel.add(topHeader, BorderLayout.NORTH);
        panel.add(chartPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildTodayMissionPanel() {
        JPanel panel = sectionPanel();
        panel.setLayout(new BorderLayout(0, 12));
        panel.setPreferredSize(new Dimension(500, 330));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 330));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(sectionTitle("今日任務 / 今日進度"), BorderLayout.WEST);
        todayStatusLabel = new JLabel("今日尚未開始", SwingConstants.RIGHT);
        todayStatusLabel.setFont(new Font(FONT_FAMILY, Font.BOLD, 13));
        todayStatusLabel.setForeground(MUTED);
        header.add(todayStatusLabel, BorderLayout.EAST);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);

        JPanel metrics = new JPanel(new GridLayout(1, 3, 10, 0));
        metrics.setOpaque(false);
        todayVolumeLabel = new JLabel("0 / " + DAILY_TRAINING_VOLUME_TARGET, SwingConstants.LEFT);
        todaySetsLabel = new JLabel("0 組", SwingConstants.LEFT);
        todayMusclesLabel = new JLabel("尚未訓練", SwingConstants.LEFT);
        metrics.add(createMissionMetric("今日訓練量", todayVolumeLabel, ACCENT));
        metrics.add(createMissionMetric("今日完成組數", todaySetsLabel, new Color(0xeab308)));
        metrics.add(createMissionMetric("今日訓練部位", todayMusclesLabel, new Color(0x10b981)));
        todayVolumeLabel.setFont(new Font(FONT_FAMILY, Font.BOLD, 13));
        todayMusclesLabel.setFont(new Font(FONT_FAMILY, Font.BOLD, 11));

        todayVolumeProgress = new JProgressBar(0, DAILY_TRAINING_VOLUME_TARGET);
        todayVolumeProgress.setValue(0);
        todayVolumeProgress.setStringPainted(true);
        todayVolumeProgress.setString("今日目標進度 0%");
        todayVolumeProgress.setFont(new Font(FONT_FAMILY, Font.BOLD, 11));
        todayVolumeProgress.setForeground(new Color(0x10b981));
        todayVolumeProgress.setBackground(CELL_BG);
        todayVolumeProgress.setBorder(BorderFactory.createLineBorder(BORDER));
        todayVolumeProgress.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));

        JPanel tasks = new JPanel(new GridLayout(1, 3, 8, 0));
        tasks.setOpaque(false);
        todayTaskSetsLabel = createMissionTaskLabel();
        todayTaskVolumeLabel = createMissionTaskLabel();
        todayTaskMusclesLabel = createMissionTaskLabel();
        tasks.add(todayTaskSetsLabel); tasks.add(todayTaskVolumeLabel); tasks.add(todayTaskMusclesLabel);

        body.add(metrics);
        body.add(Box.createRigidArea(new Dimension(1, 12)));
        body.add(todayVolumeProgress);
        body.add(Box.createRigidArea(new Dimension(1, 12)));
        body.add(tasks);

        panel.add(header, BorderLayout.NORTH);
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createMissionMetric(String title, JLabel valueLabel, Color valueColor) {
        JPanel metric = new JPanel();
        metric.setLayout(new BoxLayout(metric, BoxLayout.Y_AXIS));
        metric.setBackground(CELL_BG);
        metric.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(9, 10, 9, 10)));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font(FONT_FAMILY, Font.PLAIN, 11));
        titleLabel.setForeground(MUTED);
        valueLabel.setFont(new Font(FONT_FAMILY, Font.BOLD, 15));
        valueLabel.setForeground(valueColor);

        metric.add(titleLabel);
        metric.add(Box.createRigidArea(new Dimension(1, 5)));
        metric.add(valueLabel);
        return metric;
    }

    private JLabel createMissionTaskLabel() {
        JLabel task = new JLabel("待完成", SwingConstants.CENTER);
        task.setOpaque(true);
        task.setBackground(CELL_BG);
        task.setForeground(MUTED);
        task.setFont(new Font(FONT_FAMILY, Font.BOLD, 11));
        task.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(9, 5, 9, 5)));
        return task;
    }

    private void addScaleButton(JPanel container, String scaleKey, String label) {
        JButton btn = new JButton(label); btn.setFont(new Font(FONT_FAMILY, Font.BOLD, 12)); btn.setFocusPainted(false);
        applySolidButtonStyle(btn, BUTTON_BG, TEXT); btn.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        btn.addActionListener(e -> {
            chartPanel.setScaleMode(scaleKey, workouts); 
            for (Map.Entry<String, JButton> entry : scaleButtons.entrySet()) {
                boolean isTarget = entry.getKey().equals(scaleKey);
                entry.getValue().setBackground(isTarget ? NAV_SELECTED : BUTTON_BG); entry.getValue().setForeground(isTarget ? ACCENT : MUTED);
            }
        });
        scaleButtons.put(scaleKey, btn); container.add(btn);
    }

    private JPanel buildRecentRecordsPanel() {
        JPanel panel = sectionPanel();
        panel.setLayout(new BorderLayout(0, 10));
        panel.setPreferredSize(new Dimension(1100, 280));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));

        JPanel titleBox = new JPanel();
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.setOpaque(false);
        titleBox.add(sectionTitle("最近訓練紀錄"));
        JLabel hint = new JLabel("首頁顯示最近 " + HOME_RECENT_RECORD_LIMIT + " 筆，完整紀錄仍保存在 data/workouts.csv");
        hint.setFont(new Font(FONT_FAMILY, Font.PLAIN, 11)); hint.setForeground(MUTED);
        titleBox.add(Box.createRigidArea(new Dimension(1, 3))); titleBox.add(hint);
        panel.add(titleBox, BorderLayout.NORTH);

        JTable table = new JTable(historyModel);
        table.setRowHeight(32); table.setBackground(CELL_BG); table.setForeground(TEXT); table.setGridColor(BORDER);
        table.setFont(new Font(FONT_FAMILY, Font.PLAIN, 13)); table.setFillsViewportHeight(true);

        // 右鍵刪除容錯管線
        JPopupMenu tablePopupMenu = new JPopupMenu();
        tablePopupMenu.setBackground(PANEL_BG);
        tablePopupMenu.setBorder(BorderFactory.createLineBorder(BORDER));
        
        JMenuItem deleteItem = new JMenuItem("刪除此筆紀錄 (將扣除對應XP與肌肉量)");
        deleteItem.setFont(new Font(FONT_FAMILY, Font.BOLD, 12));
        deleteItem.setForeground(new Color(0xff8a8a)); // 警示紅
        deleteItem.setBackground(PANEL_BG); deleteItem.setOpaque(true);
        
        deleteItem.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow != -1) {
                // 1. 取得畫面上選中的日期與運動名稱
                String displayTime = (String) historyModel.getValueAt(selectedRow, 0);
                String exerciseName = (String) historyModel.getValueAt(selectedRow, 1);
                
                // 2. 從記憶體 workouts 集合中，精準過濾揪出那一筆對應的 WorkoutEntry 實體
                WorkoutEntry targetEntry = workouts.stream()
                        .filter(w -> w.displayTime().equals(displayTime) && w.getExerciseName().equals(exerciseName))
                        .findFirst().orElse(null);
                
                if (targetEntry != null) {
                    // 二次彈窗確認，防止二次誤觸
                    int confirm = JOptionPane.showConfirmDialog(this, 
                            "確定要刪除這筆【" + exerciseName + "】紀錄嗎？\n角色等級與體態會同步逆向扣除！", 
                            "刪除確認警告", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    
                    if (confirm == JOptionPane.YES_OPTION) {
                        // A. 發動大腦退化引擎
                        player.deleteWorkoutReward(targetEntry);
                        // B. 從全域清單中移除
                        workouts.remove(targetEntry);
                        // C. 永久同步寫入磁碟並全視窗重繪
                        saveAndRefresh();
                        
                        JOptionPane.showMessageDialog(this, "紀錄已成功撤銷，時空數據已完成逆向水平校正！");
                    }
                }
            }
        });
        tablePopupMenu.add(deleteItem);

        // 綁定滑鼠右鍵點擊事件
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) { handleTablePopup(e); }
            @Override
            public void mouseReleased(MouseEvent e) { handleTablePopup(e); }
            
            private void handleTablePopup(MouseEvent e) {
                if (e.isPopupTrigger()) { // 判定是否為當前作業系統的右鍵觸發行為
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0 && row < table.getRowCount()) {
                        table.setRowSelectionInterval(row, row); // 自動幫玩家選取該列
                        tablePopupMenu.show(table, e.getX(), e.getY()); // 在滑鼠座標點噴出選單
                    }
                }
            }
        });

        JTableHeader header = table.getTableHeader();
        header.setDefaultRenderer((table1, value, isSelected, hasFocus, row, column) -> {
            JLabel headerLabel = new JLabel(String.valueOf(value), SwingConstants.LEFT);
            headerLabel.setOpaque(true); headerLabel.setBackground(PANEL_BG); headerLabel.setForeground(TEXT);     
            headerLabel.setFont(new Font(FONT_FAMILY, Font.BOLD, 13));
            headerLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, BORDER), BorderFactory.createEmptyBorder(6, 8, 6, 8)));
            return headerLabel;
        });

        JScrollPane scrollPane = new JScrollPane(table); scrollPane.setBorder(BorderFactory.createLineBorder(BORDER));
        scrollPane.getViewport().setBackground(CELL_BG);

        JPanel emptyPanel = new JPanel(new BorderLayout());
        emptyPanel.setBackground(CELL_BG);
        emptyPanel.setBorder(BorderFactory.createLineBorder(BORDER));
        JLabel emptyLabel = new JLabel(
                "<html><center><b>今天還沒有訓練紀錄</b><br><span style='color:#b7c0d1;'>按下「+ 新增今日訓練」開始累積角色成長。</span></center></html>",
                SwingConstants.CENTER);
        emptyLabel.setFont(new Font(FONT_FAMILY, Font.PLAIN, 14));
        emptyLabel.setForeground(TEXT);
        emptyPanel.add(emptyLabel, BorderLayout.CENTER);

        recentRecordsCards.setBackground(PANEL_BG);
        recentRecordsCards.add(scrollPane, "table");
        recentRecordsCards.add(emptyPanel, "empty");
        panel.add(recentRecordsCards, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildActionButtonPanel() {
        JPanel panel = new JPanel(new BorderLayout(24, 0));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));

        JButton startButton = new JButton("+ 新增今日訓練");
        startButton.setFont(new Font(FONT_FAMILY, Font.BOLD, 18));
        applySolidButtonStyle(startButton, CTA_BLUE, Color.WHITE);
        startButton.setBorder(BorderFactory.createEmptyBorder(13, 54, 13, 54));
        startButton.addActionListener(event -> new WorkoutSessionDialog(this).setVisible(true));

        JButton clearDataButton = new JButton("清空資料...");
        clearDataButton.setFont(new Font(FONT_FAMILY, Font.PLAIN, 12));
        applySolidButtonStyle(clearDataButton, BUTTON_BG, new Color(0xff8a8a));
        clearDataButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x5c3940)),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)));
        clearDataButton.addActionListener(event -> performDataPurgeWithConfirmation());

        JPanel primaryBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        primaryBox.setOpaque(false); primaryBox.add(startButton);
        JPanel dangerBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 8));
        dangerBox.setOpaque(false); dangerBox.add(clearDataButton);

        panel.add(primaryBox, BorderLayout.WEST);
        panel.add(dangerBox, BorderLayout.EAST);
        return panel;
    }
    
    private void showPage(String pageName) { pageLayout.show(pageContainer, pageName); }

    public void saveAndRefresh() {
        storage.savePlayer(player); storage.saveWorkouts(workouts);
        List<WorkoutEntry> reloadedWorkouts = storage.loadWorkouts();
        this.workouts.clear();
        this.workouts.addAll(reloadedWorkouts);
        refreshAll();
    }

    /**
     * 【前端全域阻塞通知】高質感暗黑風升級工作台
     */
    private void triggerBlockingLevelUpDialog(int level) {
        JDialog dialog = new JDialog(this, "LEVEL UP", JDialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true); dialog.setSize(340, 150); dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(PANEL_BG);
        panel.setBorder(BorderFactory.createLineBorder(new Color(0xeab308), 2)); 

        JLabel textLabel = new JLabel("<html><center><span style='font-size:18px; font-weight:bold; color:#eab308;'>🎉 突破基因鎖 🎉</span><br><br><span style='color:#f4f6fb; font-size:13px;'>您的肉體已成功進化，踏入 <span style='font-size:16px; font-weight:bold; color:#5aa9ff;'>" + level + "</span> 級領域！</span></center></html>", SwingConstants.CENTER);
        panel.add(textLabel, BorderLayout.CENTER);

        JButton okBtn = new JButton("接受榮耀");
        okBtn.setFont(new Font(FONT_FAMILY, Font.BOLD, 13));
        okBtn.setBackground(CELL_BG); okBtn.setForeground(ACCENT);
        okBtn.setFocusPainted(false); okBtn.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        okBtn.addActionListener(e -> dialog.dispose()); 
        panel.add(okBtn, BorderLayout.SOUTH);

        dialog.setContentPane(panel); 
        dialog.setVisible(true); 
    }

    /**
     * 【前端成就阻塞通知】頂級暗黑科幻風榮譽覺醒工作台
     */
    private void triggerBlockingAchievementDialog(String title, String description, String difficulty) {
        JDialog dialog = new JDialog(this, "ACHIEVEMENT UNLOCKED", JDialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true); 
        dialog.setSize(380, 190); 
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(PANEL_BG);
        
        Color borderMutedGold = new Color(0xd4af37);
        panel.setBorder(BorderFactory.createLineBorder(borderMutedGold, 2)); 

        JLabel headLabel = new JLabel(" ⚔  榮 譽 成 就 覺 醒  ⚔", SwingConstants.CENTER);
        headLabel.setFont(new Font(FONT_FAMILY, Font.BOLD, 14));
        headLabel.setForeground(borderMutedGold);
        headLabel.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0)); 
        panel.add(headLabel, BorderLayout.NORTH);

        JLabel textLabel = new JLabel("<html><center>"
            + "<div style='margin-bottom: 8px; font-size: 16px; font-weight: bold; color: #ffffff;'>【" + title + "】</div>"
            + "<div style='margin-bottom: 6px; color: #b7c0d1; font-size: 12px;'>" + description + "</div>"
            + "<div style='color: #ef4444; font-size: 11px; font-weight: bold;'>[" + difficulty + " 挑戰成功]</div>"
            + "</center></html>", SwingConstants.CENTER);
        
        textLabel.setBorder(BorderFactory.createEmptyBorder(4, 16, 4, 16));
        panel.add(textLabel, BorderLayout.CENTER);

        JButton okBtn = new JButton("將榮耀刻入牆面");
        okBtn.setFont(new Font(FONT_FAMILY, Font.BOLD, 12));
        okBtn.setBackground(CELL_BG); okBtn.setForeground(ACCENT);
        okBtn.setFocusPainted(false); okBtn.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        okBtn.addActionListener(e -> dialog.dispose());
        panel.add(okBtn, BorderLayout.SOUTH);

        dialog.setContentPane(panel); 
        dialog.setVisible(true); 
    }

    private int lastKnownLevel = -1; 

    public void refreshAll() {
        if (scaleButtons.containsKey("WEEK") && scaleButtons.get("WEEK").getBackground().equals(BUTTON_BG)) {
            scaleButtons.get("WEEK").doClick();
        }

        // 在刷新畫面前，精準捕捉大腦物件的「跨級跳躍」差值
        int currentLevel = player.level();
        if (lastKnownLevel == -1) {
            lastKnownLevel = currentLevel; 
        } else if (currentLevel > lastKnownLevel) {
            int levelsGained = currentLevel - lastKnownLevel;
            int startLevel = lastKnownLevel;
            lastKnownLevel = currentLevel; // 立即同步快取線，防止異步重入

            // 依序串聯 Modal 彈窗：點完一級才會跳出下一級！
            for (int i = 1; i <= levelsGained; i++) {
                triggerBlockingLevelUpDialog(startLevel + i);
            }
        }

        // 觸發成就解鎖鏈
        List<Achievement> newUnlocks = player.triggerAchievementCheck();
        if (newUnlocks != null && !newUnlocks.isEmpty()) {
            for (Achievement ach : newUnlocks) {
                triggerBlockingAchievementDialog(
                    ach.getTitle(), 
                    ach.getDescription(), 
                    ach.getDifficulty()
                );
            }
        }

        avatarPanel.setPlayer(player); 
        
        String activeScale = "WEEK";
        for (Map.Entry<String, JButton> entry : scaleButtons.entrySet()) {
            if (entry.getValue().getBackground().equals(NAV_SELECTED)) { activeScale = entry.getKey(); break; }
        }
        chartPanel.setScaleMode(activeScale, workouts);
        refreshTodayMission();
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
            fatField.setText(player.getBodyFatPercent() > 0.0 ? String.valueOf(player.getBodyFatPercent()) : ""); 
            targetWeightField.setText(String.valueOf(player.getTargetWeight()));
            genderBox.setSelectedItem(prof.getGender());
            goalBox.setSelectedItem(player.getFitnessGoal()); 
            triggerLiveScientificCalcs(); 
        }
    }

    private void refreshTodayMission() {
        if (todayVolumeLabel == null) return;

        LocalDate today = LocalDate.now();
        double totalVolume = 0.0;
        int totalSets = 0;
        int earnedXp = 0;
        Set<String> trainedParts = new LinkedHashSet<>();

        for (WorkoutEntry entry : workouts) {
            if (!entry.time().toLocalDate().equals(today)) continue;

            totalVolume += entry.trainingVolume();
            totalSets += entry.sets();
            earnedXp += entry.xp();

            var info = ExerciseRegistry.getExercise(entry.getExerciseName());
            if (info == null || info.isAerobic()) {
                trainedParts.add("有氧");
            } else if (info.getTargetMuscle() != null) {
                trainedParts.add(switch (info.getTargetMuscle()) {
                    case CHEST -> "胸";
                    case BACK -> "背";
                    case LEGS -> "腿";
                    case ARMS -> "手臂";
                    case ABS -> "腹部";
                });
            }
        }

        int roundedVolume = (int) Math.round(totalVolume);
        boolean setsDone = totalSets >= TODAY_SET_TARGET;
        boolean volumeDone = totalVolume >= DAILY_TRAINING_VOLUME_TARGET;
        boolean musclesDone = trainedParts.size() >= TODAY_MUSCLE_TARGET;
        boolean allDone = setsDone && volumeDone && musclesDone;

        todayVolumeLabel.setText(roundedVolume + " / " + DAILY_TRAINING_VOLUME_TARGET);
        todaySetsLabel.setText(totalSets + " 組");
        
        // 🛠️【佈局防護優化】當部位字數變多時，限制最多顯示 3 個動作，其餘以 ... 代替，徹底解決介面撞牆破圖 Bug
        String trainedPartsText;
        if (trainedParts.isEmpty()) {
            trainedPartsText = "尚未訓練";
        } else {
            List<String> partsList = new ArrayList<>(trainedParts);
            if (partsList.size() > 3) {
                trainedPartsText = partsList.size() + " 個：" + String.join("、", partsList.subList(0, 3)) + "...";
            } else {
                trainedPartsText = partsList.size() + " 個：" + String.join("、", partsList);
            }
        }
        todayMusclesLabel.setText("<html>" + trainedPartsText + "</html>");
        todayMusclesLabel.setToolTipText(String.join("、", trainedParts)); // 懸浮提示依然顯示完整列表

        int progressValue = Math.min(DAILY_TRAINING_VOLUME_TARGET, roundedVolume);
        int progressPercent = Math.min(100, (int) Math.round(totalVolume * 100.0 / DAILY_TRAINING_VOLUME_TARGET));
        todayVolumeProgress.setValue(progressValue);
        todayVolumeProgress.setString("今日目標進度 " + progressPercent + "%");

        updateMissionTask(todayTaskSetsLabel, setsDone, TODAY_SET_TARGET + " 組訓練");
        updateMissionTask(todayTaskVolumeLabel, volumeDone, "累積訓練量達標");
        updateMissionTask(todayTaskMusclesLabel, musclesDone, "訓練 " + TODAY_MUSCLE_TARGET + " 個不同部位");

        if (allDone) {
            todayStatusLabel.setText("今日目標已完成！已從訓練獲得 " + earnedXp + " XP");
            todayStatusLabel.setForeground(new Color(0x10b981));
        } else if (totalSets > 0) {
            todayStatusLabel.setText("今日任務進行中");
            todayStatusLabel.setForeground(ACCENT);
        } else {
            todayStatusLabel.setText("今日尚未開始");
            todayStatusLabel.setForeground(MUTED);
        }
    }

    private void updateMissionTask(JLabel label, boolean completed, String text) {
        label.setText((completed ? "已完成：" : "待完成：") + text);
        label.setForeground(completed ? new Color(0x10b981) : MUTED);
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(completed ? new Color(0x176c56) : BORDER),
                BorderFactory.createEmptyBorder(9, 5, 9, 5)));
    }

    private void refreshHistory() {
        historyModel.setRowCount(0); 
        workouts.stream().sorted(Comparator.comparing(WorkoutEntry::time).reversed()).limit(HOME_RECENT_RECORD_LIMIT).forEach(entry -> {
            String name = entry.getExerciseName(); var exInfo = ExerciseRegistry.getExercise(name);
            String amountDisplay = ""; String setsDisplay = "";
            if (exInfo != null && exInfo.isAerobic()) {
                amountDisplay = entry.amount() + " 分鐘"; setsDisplay = "心肺有氧";
            } else {
                amountDisplay = entry.weight() == 0 ? entry.amount() + " 下 (自重)" : String.format("%.1f", entry.weight()) + "kg × " + entry.amount() + "下";
                setsDisplay = entry.sets() + " 組";
            }
            historyModel.addRow(new Object[] { entry.displayTime(), name, amountDisplay, setsDisplay });
        });
        recentRecordsLayout.show(recentRecordsCards, historyModel.getRowCount() == 0 ? "empty" : "table");
    }

    private void performDataPurgeWithConfirmation() {
        int choice = JOptionPane.showConfirmDialog(
                this,
                "確定要清空所有玩家進度與訓練紀錄嗎？\n此操作無法復原。",
                "清空資料確認",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            var currentAvatar = player.getAvatar();
            this.workouts.clear();
            try {
                java.nio.file.Files.deleteIfExists(java.nio.file.Path.of("data", "unlocked_achievements.properties"));
                java.nio.file.Files.deleteIfExists(java.nio.file.Path.of("data", "custom_plans.properties"));
            } catch (Exception ignored) {}

            this.player = new PlayerState(currentAvatar.getName(), currentAvatar.getProfile().getHeight(), currentAvatar.getProfile().getWeight(), currentAvatar.getProfile().getGender());
            this.lastKnownLevel = 1; 

            if (achievementWallPage != null) pageContainer.remove(achievementWallPage);
            if (calendarPage != null) pageContainer.remove(calendarPage);

            this.achievementWallPage = new AchievementWallPage(this.player);
            this.calendarPage = new CalendarPage(this, this.workouts);

            pageContainer.add(achievementWallPage, "analytics"); pageContainer.add(calendarPage, "calendar");
            pageContainer.revalidate(); pageContainer.repaint();
            saveAndRefresh();
            showPage("home"); 
        }
    }

    private JPanel sectionPanel() {
        JPanel panel = new JPanel(); panel.setBackground(PANEL_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER), BorderFactory.createEmptyBorder(16, 16, 16, 16)));
        return panel;
    }

    private JPanel wideSectionPanel() {
        JPanel panel = new JPanel(); panel.setBackground(PANEL_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER), BorderFactory.createEmptyBorder(18, 18, 18, 18)));
        return panel;
    }

    private JLabel sectionTitle(String text) {
        label = new JLabel(text); label.setFont(new Font(FONT_FAMILY, Font.BOLD, 18)); label.setForeground(TEXT);
        return label;
    }
    private JLabel label; 

    private void applySolidButtonStyle(JButton button, Color background, Color foreground) {
        button.setForeground(foreground); button.setBackground(background);
        button.setOpaque(true); button.setContentAreaFilled(true); button.setBorderPainted(false); button.setFocusPainted(false);
    }

    private Component gap(int height) { return Box.createRigidArea(new Dimension(1, height)); }
}