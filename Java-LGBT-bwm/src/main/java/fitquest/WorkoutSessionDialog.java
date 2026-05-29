package fitquest;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;

public class WorkoutSessionDialog extends JDialog {
    private static final Color APP_BG = new Color(0x1e222b);
    private static final Color PANEL_BG = new Color(0x282c37);
    private static final Color CELL_BG = new Color(0x20242d);
    private static final Color BORDER = new Color(0x3a4050);
    private static final Color TEXT = new Color(0xf4f6fb);
    private static final Color MUTED = new Color(0xb7c0d1);
    private static final Color ACCENT = new Color(0x2f9bff);
    private static final Color BUTTON_BG = new Color(40, 44, 55);

    private final CardLayout layout = new CardLayout();
    private final JPanel pages = new JPanel(layout);
    private final DefaultTableModel setModel = new DefaultTableModel(
            new String[] {"動作", "最近紀錄", "總重量", "備註", "組數", "重量 kg", "次數", "完成"}, 0
    ) {
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 7 ? Boolean.class : Object.class;
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 3 || column >= 5;
        }
    };
    private final JTable setTable = new JTable(setModel);
    private final List<JCheckBox> exerciseChecks = new ArrayList<>();

    public WorkoutSessionDialog(Window owner) {
        super(owner, "運動執行", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(800, 600);
        setMinimumSize(new Dimension(800, 600));
        setLocationRelativeTo(owner);

        pages.setBackground(APP_BG);
        pages.add(createSessionPage(), "session");
        pages.add(createExercisePickerPage(), "picker");
        setContentPane(pages);

        setModel.addTableModelListener(event -> {
            if (event.getType() == TableModelEvent.UPDATE
                    && (event.getColumn() == 5 || event.getColumn() == 6 || event.getColumn() == TableModelEvent.ALL_COLUMNS)) {
                updateTotalWeight(event.getFirstRow());
            }
        });

        loadChestDayPlan();
    }

    private JPanel createSessionPage() {
        JPanel page = new JPanel(new BorderLayout(0, 16));
        page.setBackground(APP_BG);
        page.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JLabel title = new JLabel("今日的運動");
        title.setForeground(TEXT);
        title.setFont(new Font("Dialog", Font.BOLD, 26));

        JPanel topActions = new JPanel(new GridLayout(1, 2, 12, 0));
        topActions.setOpaque(false);
        JButton load = actionButton("載入", BUTTON_BG);
        load.addActionListener(event -> loadChestDayPlan());
        JButton chooseExercise = actionButton("選擇運動", ACCENT);
        chooseExercise.addActionListener(event -> layout.show(pages, "picker"));
        topActions.add(load);
        topActions.add(chooseExercise);

        JPanel header = new JPanel(new BorderLayout(0, 12));
        header.setOpaque(false);
        header.add(title, BorderLayout.NORTH);
        header.add(topActions, BorderLayout.SOUTH);

        configureSetTable();
        JScrollPane tableScroll = new JScrollPane(setTable);
        tableScroll.setBorder(BorderFactory.createLineBorder(BORDER));
        tableScroll.getViewport().setBackground(CELL_BG);

        JPanel bottom = new JPanel(new BorderLayout(0, 12));
        bottom.setOpaque(false);

        JPanel setActions = new JPanel(new GridLayout(1, 3, 10, 0));
        setActions.setOpaque(false);
        JButton deleteSet = actionButton("刪除組合", new Color(0x40313a));
        deleteSet.addActionListener(event -> deleteSelectedSet());
        JButton addSet = actionButton("添加組合", BUTTON_BG);
        addSet.addActionListener(event -> addSetForSelectedExercise());
        JButton addExercise = actionButton("添加運動", BUTTON_BG);
        addExercise.addActionListener(event -> layout.show(pages, "picker"));
        setActions.add(deleteSet);
        setActions.add(addSet);
        setActions.add(addExercise);

        JButton finish = actionButton("完成運動", ACCENT);
        finish.setPreferredSize(new Dimension(100, 54));
        finish.addActionListener(event -> dispose());

        bottom.add(setActions, BorderLayout.NORTH);
        bottom.add(finish, BorderLayout.SOUTH);

        page.add(header, BorderLayout.NORTH);
        page.add(tableScroll, BorderLayout.CENTER);
        page.add(bottom, BorderLayout.SOUTH);
        return page;
    }

    private void configureSetTable() {
        setTable.setRowHeight(40);
        setTable.setFont(new Font("Dialog", Font.PLAIN, 14));
        setTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setTable.setBackground(CELL_BG);
        setTable.setForeground(TEXT);
        setTable.setGridColor(BORDER);
        setTable.setSelectionBackground(new Color(0x3a4354));
        setTable.setSelectionForeground(Color.WHITE);
        setTable.getTableHeader().setBackground(PANEL_BG);
        setTable.getTableHeader().setForeground(TEXT);
        setTable.getTableHeader().setFont(new Font("Dialog", Font.BOLD, 13));
        setTable.getColumnModel().getColumn(0).setPreferredWidth(120);
        setTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        setTable.getColumnModel().getColumn(2).setPreferredWidth(82);
        setTable.getColumnModel().getColumn(3).setPreferredWidth(130);
        setTable.getColumnModel().getColumn(4).setPreferredWidth(70);
        setTable.getColumnModel().getColumn(5).setPreferredWidth(78);
        setTable.getColumnModel().getColumn(6).setPreferredWidth(62);
        setTable.getColumnModel().getColumn(7).setPreferredWidth(58);
    }

    private JPanel createExercisePickerPage() {
        JPanel page = new JPanel(new BorderLayout(0, 14));
        page.setBackground(APP_BG);
        page.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JTextField search = new JTextField();
        search.setFont(new Font("Dialog", Font.PLAIN, 17));
        search.setForeground(TEXT);
        search.setBackground(CELL_BG);
        search.setCaretColor(TEXT);
        search.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));
        search.setText("搜尋運動");

        JComboBox<String> sorting = new JComboBox<>(new String[] {"基本順序", "最近運動順序", "按頻率", "按字母順序"});
        sorting.setFont(new Font("Dialog", Font.BOLD, 14));
        sorting.setBackground(CELL_BG);
        sorting.setForeground(TEXT);

        JPanel searchRow = new JPanel(new BorderLayout(12, 0));
        searchRow.setOpaque(false);
        searchRow.add(search, BorderLayout.CENTER);
        searchRow.add(sorting, BorderLayout.EAST);

        JTabbedPane categories = new JTabbedPane();
        categories.setFont(new Font("Dialog", Font.BOLD, 14));
        categories.addTab("標註內容", exerciseList("我的最愛：槓鈴臥推", "我的最愛：深蹲", "最近：滑輪下拉"));
        categories.addTab("全部", exerciseList(allExercises()));
        categories.addTab("胸", exerciseList("槓鈴臥推", "啞鈴臥推", "上斜啞鈴推舉", "啞鈴飛鳥", "伏地挺身"));
        categories.addTab("腿", exerciseList("深蹲", "腿推", "羅馬尼亞硬舉", "弓箭步", "腿屈伸"));
        categories.addTab("背部", exerciseList("引體向上", "槓鈴划船", "滑輪下拉", "坐姿划船", "硬舉"));
        categories.addTab("肩膀", exerciseList("肩推", "側平舉", "反向飛鳥", "阿諾推舉"));
        categories.addTab("手臂", exerciseList("啞鈴彎舉", "三頭下壓", "槓鈴彎舉", "錘式彎舉"));
        categories.addTab("舉重", exerciseList("抓舉", "挺舉", "高拉", "前蹲"));
        categories.addTab("腹部", exerciseList("仰臥起坐", "棒式", "捲腹", "死蟲", "懸垂舉腿"));
        categories.addTab("有氧運動", exerciseList("跑步", "飛輪", "划船機", "橢圓機"));

        JButton addSelected = actionButton("添加運動", ACCENT);
        addSelected.setPreferredSize(new Dimension(100, 54));
        addSelected.addActionListener(event -> addSelectedExercises());

        page.add(searchRow, BorderLayout.NORTH);
        page.add(categories, BorderLayout.CENTER);
        page.add(addSelected, BorderLayout.SOUTH);
        return page;
    }

    private String[] allExercises() {
        return new String[] {
                "槓鈴臥推", "啞鈴臥推", "深蹲", "腿推", "硬舉", "引體向上", "槓鈴划船",
                "滑輪下拉", "肩推", "側平舉", "啞鈴彎舉", "三頭下壓", "仰臥起坐", "棒式", "跑步"
        };
    }

    private JScrollPane exerciseList(String... names) {
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(CELL_BG);
        list.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        for (String name : names) {
            JCheckBox checkBox = new JCheckBox(name);
            checkBox.setFont(new Font("Dialog", Font.BOLD, 16));
            checkBox.setForeground(TEXT);
            checkBox.setBackground(CELL_BG);
            checkBox.setFocusPainted(false);
            checkBox.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
            exerciseChecks.add(checkBox);
            list.add(checkBox);
        }

        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER));
        scrollPane.getViewport().setBackground(CELL_BG);
        return scrollPane;
    }

    private JButton actionButton(String text, Color background) {
        JButton button = new JButton(text);
        button.setFont(new Font("Dialog", Font.BOLD, 16));
        button.setForeground(Color.WHITE);
        button.setBackground(background);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(12, 18, 12, 18)
        ));
        return button;
    }

    private void addSelectedExercises() {
        boolean added = false;
        for (JCheckBox checkBox : exerciseChecks) {
            if (checkBox.isSelected()) {
                addExercise(cleanExerciseName(checkBox.getText()));
                checkBox.setSelected(false);
                added = true;
            }
        }
        if (added) {
            layout.show(pages, "session");
        }
    }

    private String cleanExerciseName(String text) {
        int marker = text.indexOf('：');
        return marker >= 0 ? text.substring(marker + 1) : text;
    }

    private void loadChestDayPlan() {
        setModel.setRowCount(0);
        addSetRow("槓鈴臥推", 60, 12, "胸部主項");
        addSetRow("槓鈴臥推", 60, 12, "胸部主項");
        addSetRow("槓鈴臥推", 60, 12, "胸部主項");
    }

    private void addExercise(String name) {
        if (!hasExercise(name)) {
            addSetRow(name, 40, 8, "");
        }
    }

    private boolean hasExercise(String name) {
        for (int row = 0; row < setModel.getRowCount(); row++) {
            if (name.equals(setModel.getValueAt(row, 0))) {
                return true;
            }
        }
        return false;
    }

    private void addSetForSelectedExercise() {
        int row = setTable.getSelectedRow();
        if (row < 0 && setModel.getRowCount() > 0) {
            row = setModel.getRowCount() - 1;
        }
        if (row < 0) {
            addSetRow("槓鈴臥推", 60, 12, "");
            return;
        }
        addSetRow(String.valueOf(setModel.getValueAt(row, 0)), 40, 8, String.valueOf(setModel.getValueAt(row, 3)));
    }

    private void addSetRow(String exercise, int weight, int reps, String note) {
        int setNumber = nextSetNumber(exercise);
        int total = Math.max(0, weight) * Math.max(0, reps);
        setModel.addRow(new Object[] {
                exercise,
                recentRecordFor(exercise),
                total + " kg",
                note,
                "第 " + setNumber + " 組",
                weight,
                reps,
                false
        });
    }

    private String recentRecordFor(String exercise) {
        if ("槓鈴臥推".equals(exercise)) {
            return "60kg x 12";
        }
        if ("深蹲".equals(exercise)) {
            return "80kg x 8";
        }
        return "尚無紀錄";
    }

    private int nextSetNumber(String exercise) {
        int count = 0;
        for (int row = 0; row < setModel.getRowCount(); row++) {
            if (exercise.equals(setModel.getValueAt(row, 0))) {
                count++;
            }
        }
        return count + 1;
    }

    private void updateTotalWeight(int row) {
        if (row < 0 || row >= setModel.getRowCount()) {
            return;
        }
        int weight = parseInt(setModel.getValueAt(row, 5));
        int reps = parseInt(setModel.getValueAt(row, 6));
        setModel.setValueAt((weight * reps) + " kg", row, 2);
    }

    private int parseInt(Object value) {
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private void deleteSelectedSet() {
        int row = setTable.getSelectedRow();
        if (row >= 0) {
            setModel.removeRow(row);
        }
    }
}
