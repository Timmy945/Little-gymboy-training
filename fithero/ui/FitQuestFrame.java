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
import javax.swing.JDialog;
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
    private JTextField fatField; // 全新擴充：體脂率輸入框
    private JTextField targetWeightField; 
    private JComboBox<Gender> genderBox;
    private JComboBox<FitnessGoal> goalBox; 

    private JLabel bmiLiveLabel;
    private JLabel bmrLiveLabel;
    private JLabel tdeeLiveLabel;
    private JLabel recommendCalLabel; 
    private JLabel muscleLiveLabel; // 全新擴充：預估肌肉量科學標籤

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

        this.calendarPage.runBootUpStreakCheck(player);
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

        JButton gearButton = new JButton("⚙");
        gearButton.setFont(new Font(FONT_FAMILY, Font.BOLD, 24));
        gearButton.setForeground(TEXT); gearButton.setBackground(PANEL_BG);
        gearButton.setOpaque(true); gearButton.setContentAreaFilled(true); gearButton.setBorderPainted(false); gearButton.setFocusPainted(false);
        gearButton.setPreferredSize(new Dimension(52, 44)); gearButton.setBorder(BorderFactory.createLineBorder(BORDER));

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

        // 【優化點 1】新增體脂率輸入欄位
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

        // 加大科研回報面板
        gbc.gridx = 0; gbc.gridy = 8; gbc.gridwidth = 2; gbc.insets = new Insets(10, 15, 5, 15);
        JPanel calcReportPanel = new JPanel(new GridLayout(3, 2, 15, 8)); // 擴展為 3x2 網格
        calcReportPanel.setBackground(CELL_BG);
        calcReportPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER, 1), BorderFactory.createEmptyBorder(10, 14, 10, 14)));

        bmiLiveLabel = createReportBlock(calcReportPanel, "身體質量指數 (BMI)");
        bmrLiveLabel = createReportBlock(calcReportPanel, "基礎代謝率 (BMR) [雙引擎自適應]");
        tdeeLiveLabel = createReportBlock(calcReportPanel, "每日總熱量消耗 (TDEE) [智慧動態滾動]");
        recommendCalLabel = createReportBlock(calcReportPanel, "每日建議熱量攝取");
        muscleLiveLabel = createReportBlock(calcReportPanel, "全身預估精準肌肉量 (Muscle Mass)"); // 補上第 5 看板
        muscleLiveLabel.setForeground(new Color(241, 196, 15)); // 精緻鵝黃金

        formPanel.add(calcReportPanel, gbc);

        DocumentListener liveEngine = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { triggerLiveScientificCalcs(); }
            public void removeUpdate(DocumentEvent e) { triggerLiveScientificCalcs(); }
            public void changedUpdate(DocumentEvent e) { triggerLiveScientificCalcs(); }
        };
        ageField.getDocument().addDocumentListener(liveEngine);
        heightField.getDocument().addDocumentListener(liveEngine);
        weightField.getDocument().addDocumentListener(liveEngine);
        fatField.getDocument().addDocumentListener(liveEngine); // 體脂加入監聽

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

            // 虛擬動態載入大腦變數進行預計算
            PlayerState tempState = new PlayerState("Temp", h, w, g);
            tempState.setAge(parsedAge);
            tempState.setBodyFatPercent(fat);
            tempState.setFitnessGoal(goal);

            bmiLiveLabel.setText(String.format("%.1f (身體質量)", tempState.calculateBMI()));
            bmrLiveLabel.setText(String.format("%.1f 大卡 (%s)", tempState.calculateBMR(), (fat > 0.0 ? "Katch精準引擎" : "Mifflin引擎")));
            
            // 餵入真實歷史日誌發動滾動計算
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
            player.setBodyFatPercent(parsedFat); // 寫入持久化大腦
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

        JPanel progressHeaderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); progressHeaderPanel.setOpaque(false);
        weightProgressLabel = new JLabel("距離目標體重還差：計算中...");
        weightProgressLabel.setFont(new Font(FONT_FAMILY, Font.BOLD, 15)); weightProgressLabel.setForeground(ACCENT);
        progressHeaderPanel.add(weightProgressLabel);

        JPanel leftAvatarContainer = new JPanel(new BorderLayout(0, 10)); leftAvatarContainer.setOpaque(false);
        leftAvatarContainer.add(progressHeaderPanel, BorderLayout.NORTH); leftAvatarContainer.add(avatarPanel, BorderLayout.CENTER);

        JPanel dualPanel = new JPanel(new BorderLayout(18, 0)); dualPanel.setOpaque(false);
        dualPanel.add(leftAvatarContainer, BorderLayout.WEST); dualPanel.add(createDashboard(), BorderLayout.CENTER); 

        content.add(dualPanel); content.add(gap(18));

        JPanel bottomBtnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0)); bottomBtnRow.setOpaque(false);
        JButton startButton = new JButton("紀錄運動"); startButton.setFont(new Font(FONT_FAMILY, Font.BOLD, 20));
        applySolidButtonStyle(startButton, CTA_BLUE, Color.WHITE); startButton.setBorder(BorderFactory.createEmptyBorder(14, 64, 14, 64));
        startButton.addActionListener(event -> new WorkoutSessionDialog(this).setVisible(true));
        bottomBtnRow.add(startButton);

        JButton clearDataButton = new JButton("重置並清空所有資料"); clearDataButton.setFont(new Font(FONT_FAMILY, Font.BOLD, 14));
        applySolidButtonStyle(clearDataButton, RESET_RED, Color.WHITE); clearDataButton.setBorder(BorderFactory.createEmptyBorder(14, 24, 14, 24));
        clearDataButton.addActionListener(event -> performDataPurgeWithConfirmation());
        bottomBtnRow.add(clearDataButton);

        content.add(bottomBtnRow);
        JScrollPane scrollPane = new JScrollPane(content); scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(APP_BG); scrollPane.getViewport().setBackground(APP_BG);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        page.add(scrollPane, BorderLayout.CENTER);
        return page;
    }

    private JPanel createDashboard() {
        JPanel content = new JPanel(); content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS)); content.setOpaque(false);
        content.add(createWorkoutChartSection()); content.add(gap(14)); content.add(createHistoryPanel());
        return content;
    }

    private JPanel createWorkoutChartSection() {
        JPanel panel = wideSectionPanel(); panel.setLayout(new BorderLayout(0, 12)); panel.setPreferredSize(new Dimension(500, 310));
        JPanel topHeader = new JPanel(new BorderLayout()); topHeader.setOpaque(false);
        topHeader.add(sectionTitle("運動紀錄趨勢"), BorderLayout.WEST);

        JPanel scaleSelectorRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0)); scaleSelectorRow.setOpaque(false);
        addScaleButton(scaleSelectorRow, "WEEK", "週規模"); addScaleButton(scaleSelectorRow, "MONTH", "月規模"); addScaleButton(scaleSelectorRow, "YEAR", "年規模");
        topHeader.add(scaleSelectorRow, BorderLayout.EAST);

        panel.add(topHeader, BorderLayout.NORTH); panel.add(chartPanel, BorderLayout.CENTER); 
        return panel;
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

    private JPanel createHistoryPanel() {
        JPanel panel = sectionPanel(); panel.setLayout(new BorderLayout(0, 10)); panel.setPreferredSize(new Dimension(500, 290));
        panel.add(sectionTitle("歷史運動紀錄"), BorderLayout.NORTH);

        JTable table = new JTable(historyModel);
        table.setRowHeight(30); table.setBackground(CELL_BG); table.setForeground(TEXT); table.setGridColor(BORDER);
        table.setFont(new Font(FONT_FAMILY, Font.PLAIN, 13)); table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setDefaultRenderer((table1, value, isSelected, hasFocus, row, column) -> {
            JLabel headerLabel = new JLabel(String.valueOf(value), SwingConstants.LEFT);
            headerLabel.setOpaque(true); headerLabel.setBackground(PANEL_BG); headerLabel.setForeground(TEXT);     
            headerLabel.setFont(new Font(FONT_FAMILY, Font.BOLD, 13));
            headerLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, BORDER), BorderFactory.createEmptyBorder(6, 8, 6, 8)));
            return headerLabel;
        });

        JScrollPane scrollPane = new JScrollPane(table); scrollPane.setBorder(BorderFactory.createLineBorder(BORDER));
        scrollPane.getViewport().setBackground(CELL_BG); panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }
    
    private void showPage(String pageName) { pageLayout.show(pageContainer, pageName); }

    public void saveAndRefresh() {
        storage.savePlayer(player); storage.saveWorkouts(workouts);
        this.workouts = new ArrayList<>(storage.loadWorkouts()); 
        refreshAll();
    }

    /**
     * 【前端全域阻塞通知】高質感暗黑風升級工作台
     * 使用 Modal 阻塞機制，確保連續升級時，玩家必須點擊「確認」才會依序彈出下一級！
     */
    private void triggerBlockingLevelUpDialog(int level) {
        JDialog dialog = new JDialog(this, "LEVEL UP", JDialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true); dialog.setSize(340, 150); dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(PANEL_BG);
        panel.setBorder(BorderFactory.createLineBorder(new Color(0xeab308), 2)); // 奢華黃金榮譽框

        JLabel textLabel = new JLabel("<html><center><span style='font-size:18px; font-weight:bold; color:#eab308;'>🎉 突破基因鎖 🎉</span><br><br><span style='color:#f4f6fb; font-size:13px;'>您的肉體已成功進化，踏入 <span style='font-size:16px; font-weight:bold; color:#5aa9ff;'>" + level + "</span> 級領域！</span></center></html>", SwingConstants.CENTER);
        panel.add(textLabel, BorderLayout.CENTER);

        JButton okBtn = new JButton("接受榮耀");
        okBtn.setFont(new Font(FONT_FAMILY, Font.BOLD, 13));
        okBtn.setBackground(CELL_BG); okBtn.setForeground(ACCENT);
        okBtn.setFocusPainted(false); okBtn.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        okBtn.addActionListener(e -> dialog.dispose()); // 點擊後釋放當前視窗，才會釋放阻塞讓下一組 Loop 跑進來
        panel.add(okBtn, BorderLayout.SOUTH);

        dialog.setContentPane(panel); 
        dialog.setVisible(true); // ➔ 這一行會卡住執行緒，直到玩家關閉它
    }

    /*
     * 【前端成就阻塞通知】頂級暗黑科幻風榮譽覺醒工作台
     * 專門解決多成就連發被吞掉的 Bug，利用 Modal 鎖定執行緒，讓成就依序連彈！
     */
    private void triggerBlockingAchievementDialog(String title, String description, String difficulty) {
        JDialog dialog = new JDialog(this, "ACHIEVEMENT UNLOCKED", JDialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true); 
        
        // 🛠️【優化點 1】將視窗高度從 160 微調至 190，徹底撐開文字防禦線
        dialog.setSize(380, 190); 
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(PANEL_BG);
        
        Color borderMutedGold = new Color(0xd4af37);
        panel.setBorder(BorderFactory.createLineBorder(borderMutedGold, 2)); 

        JLabel headLabel = new JLabel(" ⚔  榮 譽 成 就 覺 醒  ⚔", SwingConstants.CENTER);
        headLabel.setFont(new Font(FONT_FAMILY, Font.BOLD, 14));
        headLabel.setForeground(borderMutedGold);
        headLabel.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0)); // 給予頂部舒適外邊距
        panel.add(headLabel, BorderLayout.NORTH);

        // 🛠️【優化點 2】剔除粗暴的 <br><br>，改用精準的 CSS margin 行高微操，確保字型永不被截斷
        JLabel textLabel = new JLabel("<html><center>"
            + "<div style='margin-bottom: 8px; font-size: 16px; font-weight: bold; color: #ffffff;'>【" + title + "】</div>"
            + "<div style='margin-bottom: 6px; color: #b7c0d1; font-size: 12px;'>" + description + "</div>"
            + "<div style='color: #ef4444; font-size: 11px; font-weight: bold;'>[" + difficulty + " 挑戰成功]</div>"
            + "</center></html>", SwingConstants.CENTER);
        
        // 🛠️ 稍微縮減文字區塊的上下 Padding，把像素完美留給字體本身
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

    private void refreshAll() {
        if (scaleButtons.containsKey("WEEK") && scaleButtons.get("WEEK").getBackground().equals(BUTTON_BG)) {
            scaleButtons.get("WEEK").doClick();
        }

        int currentLevel = player.level();
        if (lastKnownLevel == -1) {
            lastKnownLevel = currentLevel; 
        } else if (currentLevel > lastKnownLevel) {
            int levelsGained = currentLevel - lastKnownLevel;
            int startLevel = lastKnownLevel;
            lastKnownLevel = currentLevel; // 先行同步防線

            // 依序串聯彈窗：利用 JDialog 的 APPLICATION_MODAL 特性，點完一級才會跳出下一級！
            for (int i = 1; i <= levelsGained; i++) {
                triggerBlockingLevelUpDialog(startLevel + i);
            }
        }

        List<Achievement> newUnlocks = player.triggerAchievementCheck();
        if (newUnlocks != null && !newUnlocks.isEmpty()) {
            // 利用阻塞特性，依序將本次打卡噴出的所有成就一個個彈給使用者看
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
            fatField.setText(player.getBodyFatPercent() > 0.0 ? String.valueOf(player.getBodyFatPercent()) : ""); // 體脂還原
            targetWeightField.setText(String.valueOf(player.getTargetWeight()));
            genderBox.setSelectedItem(prof.getGender());
            goalBox.setSelectedItem(player.getFitnessGoal()); 
            triggerLiveScientificCalcs(); 
        }
    }

    private void refreshHistory() {
        historyModel.setRowCount(0); 
        workouts.stream().sorted(Comparator.comparing(WorkoutEntry::time).reversed()).limit(30).forEach(entry -> {
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
    }

    private void performDataPurgeWithConfirmation() {
        int choice = JOptionPane.showConfirmDialog(this, "確定要清空所有測試資料嗎？", "刪除警告", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
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