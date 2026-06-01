package fithero.ui;

import fithero.logic.manager.PlayerState;
import fithero.model.player.Gender;
import fithero.model.exercise.MuscleGroup;
import fithero.model.exercise.ExerciseInfo;
import fithero.model.exercise.ExerciseRegistry;
import fithero.model.workout.WorkoutEntry;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/**
 * 訓練菜單工作台：支援自訂課表永久Properties記憶、動態分類過濾與完訓紀錄同步
 */
public class WorkoutSessionDialog extends JDialog {
    private static final Color APP_BG = new Color(0x1e222b);
    private static final Color PANEL_BG = new Color(0x282c37);
    private static final Color CELL_BG = new Color(0x20242d);
    private static final Color BORDER = new Color(0x3a4050);
    private static final Color TEXT = new Color(0xf4f6fb);
    private static final Color ACCENT = new Color(0x2f9bff);
    private static final Color BUTTON_BG = new Color(40, 44, 55);
    private static final Color NAV_SELECTED = new Color(0x3a4354);
    
    private static final String FONT_FAMILY = "Microsoft JhengHei";

    private final Path menuFile = Path.of("data", "custom_menu.properties");

    private final CardLayout layout = new CardLayout();
    private final JPanel pages = new JPanel(layout);
    
    private final DefaultTableModel setModel = new DefaultTableModel(
            new String[] {"動作名稱", "重量 kg", "數量(下/分)", "總組數", "項目勾選"}, 0
    ) {
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 4 ? Boolean.class : Object.class;
        }
        @Override
        public boolean isCellEditable(int row, int column) {
            return column >= 1 && column <= 4; 
        }
    };
    private final JTable setTable = new JTable(setModel);
    
    private final JTextField searchField = new JTextField();
    private final DefaultListModel<String> exerciseListModel = new DefaultListModel<>();
    private final JList<String> exerciseJList = new JList<>(exerciseListModel);
    
    private final Map<String, JButton> tabButtons = new HashMap<>();
    private String currentSelectedCategory = "全部"; 

    public WorkoutSessionDialog(Window owner) {
        super(owner, "訓練菜單工作台", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(780, 580);
        setMinimumSize(new Dimension(780, 580));
        setLocationRelativeTo(owner);

        pages.setBackground(APP_BG);
        pages.add(createSessionPage(), "session");
        pages.add(createExercisePickerPage(), "picker");
        setContentPane(pages);

        loadUserSavedPlan();
    }

    private JPanel createSessionPage() {
        JPanel page = new JPanel(new BorderLayout(0, 16));
        page.setBackground(APP_BG);
        page.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        
        JLabel title = new JLabel("個人訓練菜單");
        title.setForeground(TEXT);
        title.setFont(new Font(FONT_FAMILY, Font.BOLD, 24));
        headerPanel.add(title, BorderLayout.WEST);

        JButton resetPlanBtn = new JButton("恢復原廠預設菜單");
        resetPlanBtn.setFont(new Font(FONT_FAMILY, Font.PLAIN, 12));
        resetPlanBtn.setForeground(new Color(0xb7c0d1));
        resetPlanBtn.setBackground(new Color(0x323846));
        resetPlanBtn.setFocusPainted(false);
        resetPlanBtn.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        resetPlanBtn.addActionListener(event -> {
            loadDefaultPlan();
            saveUserCurrentPlan(); 
        });
        headerPanel.add(resetPlanBtn, BorderLayout.EAST);

        setTable.setRowHeight(36);
        setTable.setFont(new Font(FONT_FAMILY, Font.PLAIN, 14));
        setTable.setBackground(CELL_BG);
        setTable.setForeground(TEXT);
        setTable.setGridColor(BORDER);
        setTable.getTableHeader().setBackground(PANEL_BG);
        setTable.getTableHeader().setForeground(TEXT);
        setTable.getTableHeader().setFont(new Font(FONT_FAMILY, Font.BOLD, 13));
        
        setTable.setCellSelectionEnabled(false);
        setTable.setRowSelectionAllowed(false);
        setTable.setColumnSelectionAllowed(false);
        
        JScrollPane tableScroll = new JScrollPane(setTable);
        tableScroll.setBorder(BorderFactory.createLineBorder(BORDER));
        tableScroll.getViewport().setBackground(CELL_BG);

        JPanel bottom = new JPanel(new BorderLayout(0, 12));
        bottom.setOpaque(false);

        JPanel actionRow = new JPanel(new GridLayout(1, 2, 12, 0));
        actionRow.setOpaque(false);
        
        JButton deleteSet = actionButton("刪除選定動作 (依勾選)", new Color(0x40313a));
        deleteSet.addActionListener(event -> {
            boolean removedAny = false;
            for (int i = setModel.getRowCount() - 1; i >= 0; i--) {
                Boolean isChecked = (Boolean) setModel.getValueAt(i, 4);
                if (isChecked != null && isChecked) {
                    setModel.removeRow(i);
                    removedAny = true;
                }
            }
            if (removedAny) {
                saveUserCurrentPlan(); 
            }
        });
        
        JButton addExercise = actionButton("由清單添加項目", BUTTON_BG);
        addExercise.addActionListener(event -> {
            searchField.setText("");
            switchCategory("全部"); 
            layout.show(pages, "picker");
        });
        
        actionRow.add(deleteSet);
        actionRow.add(addExercise);

        JButton finish = actionButton("完成紀錄", ACCENT);
        finish.setPreferredSize(new Dimension(100, 50));
        finish.addActionListener(event -> {
            if (setTable.isEditing()) {
                setTable.getCellEditor().stopCellEditing();
            }

            FitQuestFrame frame = (FitQuestFrame) SwingUtilities.getWindowAncestor(this);
            PlayerState playerState = frame.getPlayerState();
            boolean processedAny = false;

            for (int i = 0; i < setModel.getRowCount(); i++) {
                Boolean isChecked = (Boolean) setModel.getValueAt(i, 4);
                if (isChecked == null || !isChecked) continue; 

                String name = (String) setModel.getValueAt(i, 0);
                double weight = parseSafeDouble(setModel.getValueAt(i, 1));
                int reps = parseSafeInt(setModel.getValueAt(i, 2));
                int sets = parseSafeInt(setModel.getValueAt(i, 3));

                ExerciseInfo info = ExerciseRegistry.getExercise(name);
                if (info == null) continue;

                if (info.isAerobic()) {
                    int totalMinutes = reps * sets;
                    int xpGained = playerState.submitAerobicWorkout(name, totalMinutes, frame.getWorkoutsList(), frame.getCalendarPage().getPlanProperties());
                    frame.getWorkoutsList().add(new WorkoutEntry(java.time.LocalDateTime.now(), name, totalMinutes, xpGained));
                } else {
                    int xpGained = playerState.submitResistanceWorkout(name, weight, reps, sets, frame.getWorkoutsList(), frame.getCalendarPage().getPlanProperties());
                    frame.getWorkoutsList().add(new WorkoutEntry(java.time.LocalDateTime.now(), name, weight, reps, sets, xpGained));
                }
                processedAny = true;
            }

            saveUserCurrentPlan();

            if (processedAny) frame.saveAndRefresh();
            dispose();
        });

        bottom.add(actionRow, BorderLayout.NORTH);
        bottom.add(finish, BorderLayout.SOUTH);

        page.add(headerPanel, BorderLayout.NORTH);
        page.add(tableScroll, BorderLayout.CENTER);
        page.add(bottom, BorderLayout.SOUTH);
        return page;
    }

    private JPanel createExercisePickerPage() {
        JPanel page = new JPanel(new BorderLayout(0, 14));
        page.setBackground(APP_BG);
        page.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        searchField.setFont(new Font(FONT_FAMILY, Font.PLAIN, 15));
        searchField.setForeground(TEXT); searchField.setBackground(CELL_BG); searchField.setCaretColor(TEXT);
        searchField.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER), BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                updateExerciseList(searchField.getText().trim(), currentSelectedCategory);
            }
        });

        JPanel searchBarRow = new JPanel(new BorderLayout(8, 0));
        searchBarRow.setOpaque(false);
        JLabel searchHint = new JLabel("關鍵字過濾："); searchHint.setFont(new Font(FONT_FAMILY, Font.BOLD, 14)); searchHint.setForeground(TEXT);
        searchBarRow.add(searchHint, BorderLayout.WEST);
        searchBarRow.add(searchField, BorderLayout.CENTER);

        JPanel tagsPanel = new JPanel(new GridLayout(1, 7, 6, 0));
        tagsPanel.setOpaque(false);
        
        String[] categories = {"全部", "胸肌", "背肌", "手臂/肩膀", "腿部", "腹部核心", "有氧"};
        for (String cat : categories) {
            JButton tagBtn = new JButton(cat);
            tagBtn.setFont(new Font(FONT_FAMILY, Font.BOLD, 12));
            tagBtn.setFocusPainted(false);
            tagBtn.setForeground(TEXT);
            tagBtn.setBackground(BUTTON_BG);
            tagBtn.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
            tagBtn.addActionListener(e -> switchCategory(cat));
            
            tabButtons.put(cat, tagBtn);
            tagsPanel.add(tagBtn);
        }

        JPanel topContainer = new JPanel(new BorderLayout(0, 10));
        topContainer.setOpaque(false);
        topContainer.add(searchBarRow, BorderLayout.NORTH);
        topContainer.add(tagsPanel, BorderLayout.SOUTH);

        exerciseJList.setFont(new Font(FONT_FAMILY, Font.BOLD, 15));
        exerciseJList.setBackground(CELL_BG); exerciseJList.setForeground(TEXT);
        exerciseJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        exerciseJList.setSelectionBackground(new Color(0x3a4354));
        
        JScrollPane listScroll = new JScrollPane(exerciseJList);
        listScroll.setBorder(BorderFactory.createLineBorder(BORDER));

        JPanel bottomActions = new JPanel(new GridLayout(1, 2, 12, 0));
        bottomActions.setOpaque(false);
        
        JButton cancelBtn = actionButton("返回菜單", BUTTON_BG);
        cancelBtn.addActionListener(event -> layout.show(pages, "session"));
        
        JButton confirmBtn = actionButton("確認追加此動作", ACCENT);
        confirmBtn.addActionListener(event -> {
            String selected = exerciseJList.getSelectedValue();
            if (selected != null) {
                addExerciseWithMergeLogic(selected);
                saveUserCurrentPlan(); 
                layout.show(pages, "session");
            }
        });
        
        bottomActions.add(cancelBtn);
        bottomActions.add(confirmBtn);

        page.add(topContainer, BorderLayout.NORTH);
        page.add(listScroll, BorderLayout.CENTER);
        page.add(bottomActions, BorderLayout.SOUTH);
        return page;
    }

    private void switchCategory(String category) {
        this.currentSelectedCategory = category;
        for (Map.Entry<String, JButton> entry : tabButtons.entrySet()) {
            boolean isTarget = entry.getKey().equals(category);
            entry.getValue().setBackground(isTarget ? NAV_SELECTED : BUTTON_BG);
            entry.getValue().setForeground(isTarget ? ACCENT : TEXT);
        }
        updateExerciseList(searchField.getText().trim(), category);
    }

    private void addExerciseWithMergeLogic(String name) {
        var ex = ExerciseRegistry.getExercise(name);
        int defaultWeight = (ex != null && ex.isAerobic()) ? 0 : 40;
        int defaultReps = (ex != null && ex.isAerobic()) ? 30 : 10;
        int defaultSets = 1;

        for (int row = 0; row < setModel.getRowCount(); row++) {
            String existingName = (String) setModel.getValueAt(row, 0);
            double existingWeight = parseSafeDouble(setModel.getValueAt(row, 1));
            int existingReps = parseSafeInt(setModel.getValueAt(row, 2));
            int existingSets = parseSafeInt(setModel.getValueAt(row, 3));

            if (existingName.equals(name) && existingWeight == defaultWeight && existingReps == defaultReps) {
                setModel.setValueAt(existingSets + 1, row, 3);
                return;
            }
        }

        setModel.addRow(new Object[]{name, defaultWeight, defaultReps, defaultSets, false});
    }

    private void loadUserSavedPlan() {
        setModel.setRowCount(0);
        
        if (!Files.exists(menuFile)) {
            loadDefaultPlan();
            return;
        }

        Properties props = new Properties();
        try (var reader = Files.newBufferedReader(menuFile, StandardCharsets.UTF_8)) {
            props.load(reader);
            
            int totalItems = parseSafeInt(props.getProperty("menu.total_items", "0"));
            if (totalItems == 0) {
                loadDefaultPlan();
                return;
            }

            for (int i = 0; i < totalItems; i++) {
                String name = props.getProperty("item." + i + ".name");
                double weight = parseSafeDouble(props.getProperty("item." + i + ".weight"));
                int reps = parseSafeInt(props.getProperty("item." + i + ".reps"));
                int sets = parseSafeInt(props.getProperty("item." + i + ".sets"));
                
                if (name != null && !name.isBlank()) {
                    setModel.addRow(new Object[]{name, weight, reps, sets, false});
                }
            }
            System.out.println("[課表記憶核心] 成功還原玩家客製化的專屬訓練組合！");
        } catch (IOException ex) {
            System.err.println("[課表記憶核心] 還原失敗，降級使用原廠預設。");
            loadDefaultPlan();
        }
    }

    private void saveUserCurrentPlan() {
        Properties props = new Properties();
        int totalItems = setModel.getRowCount();
        props.setProperty("menu.total_items", String.valueOf(totalItems));

        for (int i = 0; i < totalItems; i++) {
            props.setProperty("item." + i + ".name", String.valueOf(setModel.getValueAt(i, 0)));
            props.setProperty("item." + i + ".weight", String.valueOf(setModel.getValueAt(i, 1)));
            props.setProperty("item." + i + ".reps", String.valueOf(setModel.getValueAt(i, 2)));
            props.setProperty("item." + i + ".sets", String.valueOf(setModel.getValueAt(i, 3)));
        }

        try {
            Files.createDirectories(menuFile.getParent());
            try (var writer = Files.newBufferedWriter(menuFile, StandardCharsets.UTF_8)) {
                props.store(writer, "FitQuest User Customized Gym Menu Plan");
                System.out.println("[課表記憶核心] 玩家客製化訓練課表永久保存成功！");
            }
        } catch (IOException ex) {
            System.err.println("[課表記憶核心] 寫入硬碟失敗: " + ex.getMessage());
        }
    }

    private void loadDefaultPlan() {
        setModel.setRowCount(0);
        setModel.addRow(new Object[]{"槓鈴臥推", 60, 8, 3, false});
        setModel.addRow(new Object[]{"滑輪下拉", 45, 10, 3, false});
        setModel.addRow(new Object[]{"槓鈴深蹲", 70, 8, 4, false});
        setModel.addRow(new Object[]{"啞鈴二頭彎舉", 12, 12, 3, false});
        setModel.addRow(new Object[]{"捲腹", 0, 20, 3, false});
    }

    private void updateExerciseList(String keyword, String category) {
        exerciseListModel.clear();
        String[] full70List = {
            "慢跑 (輕鬆)", "快跑 (高強度)", "健走", "散步", "越野跑", "游泳 (蛙式)", "游泳 (自由式)", 
            "騎自行車 (休閒)", "騎自行車 (競速)", "跳繩 (慢速)", "跳繩 (快速)", "尊巴舞蹈 (Zumba)", 
            "嘻哈街舞", "高強度有氧循環 (HIIT)", "Tabata 循環", "飛輪車 (高強度)", "登階機", "划船機", 
            "橢圓機", "有氧拳擊 (BodyCombat)", "籃球 (全場比賽)", "籃球 (投籃練習)", "足球", "羽毛球 (單打)", 
            "羽毛球 (雙打)", "網球", "排球", "桌球 (乒乓球)", "壁球", "棒式核心維持", "伏地挺身", "槓鈴臥推", 
            "啞鈴臥推", "上斜啞鈴臥推", "下斜槓鈴臥推", "機械夾胸", "啞鈴飛鳥", "滑輪纜繩交叉飛鳥", 
            "雙槓體撐 (胸肌偏向)", "鑽石伏地挺身", "滑輪下拉", "引體向上", "槓鈴划船", "單臂啞鈴划船", 
            "坐姿反衝划船", "T桿划船", "直臂滑輪下拉", "超人式背肌背屈", "反向飛鳥 (後三角)", "機械夾背", 
            "徒手深蹲", "槓鈴深蹲", "傳統硬舉", "相撲硬舉", "啞鈴保加利亞分腿蹲", "機械腿推舉", "機械腿伸展", 
            "機械俯臥腿彎舉", "啞鈴弓步蹲", "提踵 (小腿訓練)", "啞鈴二頭彎舉", "槓鈴上舉肩推", "啞鈴側平舉", 
            "滑輪三頭下壓", "啞鈴錘式彎舉", "仰臥起坐", "捲腹", "俄羅斯轉體", "懸垂舉腿", "鳥狗式核心穩定"
        };

        for (String item : full70List) {
            if (!keyword.isEmpty() && !item.toLowerCase().contains(keyword.toLowerCase())) continue;

            if (!category.equals("全部")) {
                var exInfo = ExerciseRegistry.getExercise(item);
                if (exInfo == null) continue;
                
                if (exInfo.isAerobic()) {
                    if (!category.equals("有氧")) continue; 
                } else {
                    // 🔥【強型別安全重構】對接 MuscleGroup 列舉做物理比對，避免 equals 字串錯位
                    MuscleGroup target = exInfo.getTargetMuscle();
                    boolean match = false;
                    if (category.equals("胸肌") && target == MuscleGroup.CHEST) match = true;
                    else if (category.equals("背肌") && target == MuscleGroup.BACK) match = true;
                    else if (category.equals("腿部") && target == MuscleGroup.LEGS) match = true;
                    else if (category.equals("手臂/肩膀") && target == MuscleGroup.ARMS) match = true;
                    else if (category.equals("腹部核心") && target == MuscleGroup.ABS) match = true;
                    
                    if (!match) continue;
                }
            }
            exerciseListModel.addElement(item);
        }
    }

    private JButton actionButton(String text, Color background) {
        JButton button = new JButton(text);
        button.setFont(new Font(FONT_FAMILY, Font.BOLD, 15));
        button.setForeground(Color.WHITE); button.setBackground(background);
        button.setOpaque(true); button.setContentAreaFilled(true); button.setBorderPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER), BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        return button;
    }

    private int parseSafeInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Integer) return (Integer) value;
        try { return Integer.parseInt(String.valueOf(value).trim()); } 
        catch (Exception e) { return 0; }
    }

    private double parseSafeDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Double) return (Double) value;
        try { return Double.parseDouble(String.valueOf(value).trim()); } 
        catch (Exception e) { return 0.0; }
    }
}