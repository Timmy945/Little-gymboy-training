package fithero.ui;

import fithero.logic.manager.PlayerState;
import fithero.model.exercise.ExerciseInfo;
import fithero.model.exercise.ExerciseRegistry;
import fithero.model.workout.WorkoutEntry;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints; 
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
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
import javax.swing.JOptionPane;      
import javax.swing.DefaultListModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.DefaultTableCellRenderer;

public class CalendarPage extends JPanel {
    private static final Color APP_BG = new Color(0x1e222b);
    private static final Color PANEL_BG = new Color(0x282c37);
    private static final Color CELL_BG = new Color(0x20242d);
    private static final Color BORDER = new Color(0x3a4050);
    private static final Color TEXT = new Color(0xf4f6fb);
    private static final Color ACCENT = new Color(0x2f9bff);
    private static final Color SUNDAY = new Color(0xff6b7a);
    private static final Color SATURDAY = new Color(0x72b7ff);
    private static final Color MUTED = new Color(0xb7c0d1);

    private static final Color COLOR_CHEST = new Color(0xef4444); 
    private static final Color COLOR_BACK = new Color(0xf97316);  
    private static final Color COLOR_LEGS = new Color(0xeab308);  
    private static final Color COLOR_ARMS = new Color(0x22c55e);  
    private static final Color COLOR_CORE = new Color(0x3b82f6);  
    private static final Color COLOR_AEROBIC = new Color(0xa855f7); 
    private static final Color COLOR_PLAN_GREEN = new Color(0x10b981); 

    private static final String FONT_FAMILY = "Microsoft JhengHei";

    private final Window owner;
    private final List<WorkoutEntry> workouts;
    
    private YearMonth currentMonth;
    private JLabel titleLabel;
    private CalendarGridPanel gridPanel;

    private final Path planFile = Path.of("data", "custom_plans.properties");
    private final Properties planProperties = new Properties();

    public CalendarPage(Window owner, List<WorkoutEntry> workouts) {
        this.owner = owner;
        this.workouts = workouts;
        this.currentMonth = YearMonth.now();

        loadPlanProperties(); 

        setLayout(new BorderLayout(0, 16));
        setBackground(APP_BG);
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        add(createHeader(), BorderLayout.NORTH);
        gridPanel = new CalendarGridPanel();
        add(gridPanel, BorderLayout.CENTER);
        add(createLegendPanel(), BorderLayout.SOUTH); 
    }

    public Properties getPlanProperties() { return this.planProperties; }

    public void runBootUpStreakCheck(PlayerState playerState) {
        LocalDate today = LocalDate.now();
        boolean dataChanged = false;

        for (int i = 1; i <= 30; i++) {
            LocalDate checkDate = today.minusDays(i);
            String countKey = "plan." + checkDate + ".count";
            String statusKey = "plan." + checkDate + ".status";

            int planCount = Integer.parseInt(planProperties.getProperty(countKey, "0"));
            if (planCount > 0 && planProperties.getProperty(statusKey) == null) {
                boolean hasWorkout = workouts.stream().anyMatch(w -> w.time().toLocalDate().equals(checkDate));
                if (hasWorkout) {
                    planProperties.setProperty(statusKey, "done"); 
                } else {
                    planProperties.setProperty(statusKey, "lazy");
                    playerState.applyLazyPenalty(10.0); 
                    dataChanged = true;
                }
            }
        }
        if (dataChanged) {
            savePlanProperties();
            playerState.triggerAchievementCheck();
            gridPanel.repaint();
        }
    }

    private void loadPlanProperties() {
        if (Files.exists(planFile)) {
            try (var reader = Files.newBufferedReader(planFile, StandardCharsets.UTF_8)) {
                planProperties.load(reader);
            } catch (IOException ignored) {}
        }
    }

    private void savePlanProperties() {
        try {
            Files.createDirectories(planFile.getParent());
            try (var writer = Files.newBufferedWriter(planFile, StandardCharsets.UTF_8)) {
                planProperties.store(writer, "FitQuest Training Day Schedule Plans");
            }
        } catch (IOException ignored) {}
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 0)); header.setOpaque(false);
        JPanel centerCtrl = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 0)); centerCtrl.setOpaque(false);

        JButton prevBtn = navButton("◀"); prevBtn.addActionListener(e -> changeMonth(-1));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy 年 M 月", Locale.TAIWAN);
        titleLabel = new JLabel(formatter.format(currentMonth), SwingConstants.CENTER);
        titleLabel.setFont(new Font(FONT_FAMILY, Font.BOLD, 24)); titleLabel.setForeground(TEXT);
        titleLabel.setPreferredSize(new Dimension(180, 44));

        JButton nextBtn = navButton("▶"); nextBtn.addActionListener(e -> changeMonth(1));
        centerCtrl.add(prevBtn); centerCtrl.add(titleLabel); centerCtrl.add(nextBtn);

        JPanel spacerL = new JPanel() {{ setOpaque(false); setPreferredSize(new Dimension(52, 44)); }};
        JPanel spacerR = new JPanel() {{ setOpaque(false); setPreferredSize(new Dimension(52, 44)); }};
        header.add(spacerL, BorderLayout.WEST); header.add(centerCtrl, BorderLayout.CENTER); header.add(spacerR, BorderLayout.EAST);
        return header;
    }

    private JButton navButton(String text) {
        JButton btn = new JButton(text); btn.setFont(new Font(FONT_FAMILY, Font.BOLD, 16));
        btn.setForeground(TEXT); btn.setBackground(PANEL_BG);
        btn.setOpaque(true); btn.setContentAreaFilled(true); btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(44, 38)); btn.setBorder(BorderFactory.createLineBorder(BORDER));
        return btn;
    }

    private void changeMonth(int monthsToAdd) {
        currentMonth = currentMonth.plusMonths(monthsToAdd);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy 年 M 月", Locale.TAIWAN);
        titleLabel.setText(formatter.format(currentMonth));
        gridPanel.repaint();
    }

    private Map<LocalDate, List<Color>> workoutMarkers() {
        Map<LocalDate, List<Color>> markers = new HashMap<>();
        for (WorkoutEntry workout : workouts) {
            LocalDate date = workout.time().toLocalDate();
            // 向 ExerciseRegistry 要求該運動名稱的詳細科學資料
            ExerciseInfo scienceInfo = ExerciseRegistry.getExercise(workout.getExerciseName());
            Color targetColor = COLOR_AEROBIC; 
            if (scienceInfo != null) {
                if (scienceInfo.isAerobic()) {
                    targetColor = COLOR_AEROBIC; 
                } else {
                    targetColor = switch (scienceInfo.getTargetMuscle()) {
                        case CHEST -> COLOR_CHEST;   case BACK -> COLOR_BACK;     
                        case LEGS -> COLOR_LEGS;     case ARMS -> COLOR_ARMS;     
                        case ABS -> COLOR_CORE;     
                    };
                }
            }
            markers.computeIfAbsent(date, key -> new ArrayList<>());
            if (!markers.get(date).contains(targetColor)) markers.get(date).add(targetColor);
        }
        return markers;
    }

    private JPanel createLegendPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0)); panel.setOpaque(false);
        panel.add(new JLabel("<html><span style='color:#ef4444;'>●</span> 胸肌</html>"));
        panel.add(new JLabel("<html><span style='color:#f97316;'>●</span> 背肌</html>"));
        panel.add(new JLabel("<html><span style='color:#eab308;'>●</span> 腿部</html>"));
        panel.add(new JLabel("<html><span style='color:#22c55e;'>●</span> 手臂</html>"));
        panel.add(new JLabel("<html><span style='color:#3b82f6;'>●</span> 核心</html>"));
        panel.add(new JLabel("<html><span style='color:#a855f7;'>●</span> 有氧</html>"));
        panel.add(new JLabel("<html><span style='color:#10b981;'>■</span> 預約排程日</html>"));
        for (Component c : panel.getComponents()) {
            c.setForeground(MUTED); c.setFont(new Font(FONT_FAMILY, Font.BOLD, 12));
        }
        return panel;
    }

    private class CalendarGridPanel extends JPanel {
        private final String[] weekdays = {"日", "一", "二", "三", "四", "五", "六"};
        private int cachedLeft = 18; int cachedGridTop = 0; int cachedCellWidth = 0; int cachedCellHeight = 0; int cachedGap = 8;

        CalendarGridPanel() {
            setBackground(PANEL_BG);
            setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER), BorderFactory.createEmptyBorder(14, 14, 14, 14)));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { handleGridClick(e.getX(), e.getY()); }
            });
        }

        private void handleGridClick(int mouseX, int mouseY) {
            if (cachedCellWidth <= 0 || cachedCellHeight <= 0) return;
            if (mouseX < cachedLeft || mouseY < cachedGridTop) return;
            int col = (mouseX - cachedLeft) / (cachedCellWidth + cachedGap);
            int row = (mouseY - cachedGridTop) / (cachedCellHeight + cachedGap);
            if (col >= 0 && col < 7 && row >= 0 && row < 6) {
                int index = row * 7 + col;
                LocalDate firstDay = currentMonth.atDay(1);
                int leadingDays = firstDay.getDayOfWeek().getValue() % 7;
                showDayWorkoutsDetails(firstDay.plusDays(index - leadingDays));
            }
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth(); int height = getHeight();
            int left = 18; int top = 18; int weekdayHeight = 34; int gap = 8;
            int gridTop = top + weekdayHeight + 8;
            int cellWidth = (width - left * 2 - gap * 6) / 7;
            int cellHeight = (height - gridTop - 18 - gap * 5) / 6;

            this.cachedLeft = left; this.cachedGridTop = gridTop; this.cachedCellWidth = cellWidth; this.cachedCellHeight = cellHeight; this.cachedGap = gap;
            drawWeekdays(g, left, top, cellWidth, gap);
            drawDates(g, left, gridTop, cellWidth, cellHeight, gap);
            g.dispose();
        }

        private void drawWeekdays(Graphics2D g, int left, int top, int cellWidth, int gap) {
            g.setFont(new Font(FONT_FAMILY, Font.BOLD, 15));
            FontMetrics metrics = g.getFontMetrics();
            for (int i = 0; i < weekdays.length; i++) {
                int x = left + i * (cellWidth + gap);
                g.setColor(colorForColumn(i));
                g.drawString(weekdays[i], x + (cellWidth - metrics.stringWidth(weekdays[i])) / 2, top + 22);
            }
        }

        private void drawDates(Graphics2D g, int left, int top, int cellWidth, int cellHeight, int gap) {
            LocalDate firstDay = currentMonth.atDay(1);
            int leadingDays = firstDay.getDayOfWeek().getValue() % 7;
            Map<LocalDate, List<Color>> markers = workoutMarkers();

            g.setFont(new Font(FONT_FAMILY, Font.BOLD, 18));
            for (int index = 0; index < 42; index++) {
                int row = index / 7; int column = index % 7;
                int x = left + column * (cellWidth + gap);
                int y = top + row * (cellHeight + gap);
                LocalDate date = firstDay.plusDays(index - leadingDays);
                drawCell(g, x, y, cellWidth, cellHeight, column, date, YearMonth.from(date).equals(currentMonth), date.equals(LocalDate.now()), markers.get(date));
            }
        }

        private void drawCell(Graphics2D g, int x, int y, int width, int height, int column, LocalDate date, boolean inMonth, boolean today, List<Color> markers) {
            int planCount = Integer.parseInt(planProperties.getProperty("plan." + date + ".count", "0"));
            boolean isPlanDay = planCount > 0;

            g.setColor(today ? new Color(0x263b55) : CELL_BG);
            g.fill(new RoundRectangle2D.Double(x, y, width, height, 10, 10));
            g.setColor(today ? ACCENT : (isPlanDay ? COLOR_PLAN_GREEN : BORDER));
            g.setStroke(new BasicStroke((today || isPlanDay) ? 2f : 1f));
            g.draw(new RoundRectangle2D.Double(x, y, width, height, 10, 10));

            Color dayColor = colorForColumn(column);
            if (!inMonth) dayColor = new Color(dayColor.getRed(), dayColor.getGreen(), dayColor.getBlue(), 95);
            g.setColor(dayColor); g.setFont(new Font(FONT_FAMILY, Font.BOLD, 18));
            g.drawString(String.valueOf(date.getDayOfMonth()), x + 14, y + 30);

            if (isPlanDay && inMonth) {
                g.setFont(new Font(FONT_FAMILY, Font.PLAIN, 10)); g.setColor(COLOR_PLAN_GREEN);
                g.drawString(planCount + " 個排程", x + width - 60, y + 18);
            }

            if (markers != null && !markers.isEmpty() && inMonth) {
                int markerWidth = Math.min(20, Math.max(10, (width - 40) / Math.min(6, markers.size())));
                int totalWidth = markerWidth * Math.min(6, markers.size()) + 5 * (Math.min(6, markers.size()) - 1);
                int markerX = x + (width - totalWidth) / 2; int markerY = y + height - 16;
                for (int i = 0; i < Math.min(6, markers.size()); i++) {
                    g.setColor(markers.get(i)); g.fillRoundRect(markerX + i * (markerWidth + 5), markerY, markerWidth, 5, 4, 4);
                }
            }

            if ("lazy".equals(planProperties.getProperty("plan." + date + ".status")) && inMonth) {
                g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); g.setColor(new Color(0x6b7280)); 
                int p = 22; g.drawLine(x + p, y + p, x + width - p, y + height - p); g.drawLine(x + width - p, y + p, x + p, y + height - p);
            }
        }

        private Color colorForColumn(int column) {
            if (column == DayOfWeek.SUNDAY.getValue() % 7) return SUNDAY;
            if (column == DayOfWeek.SATURDAY.getValue() % 7) return SATURDAY;
            return TEXT;
        }
    }

    private void showDayWorkoutsDetails(LocalDate date) {
        List<WorkoutEntry> dayWorkouts = workouts.stream()
            .filter(w -> {
                // 將時間強制轉換為電腦當前的本地時區，再切出日期
                LocalDate workoutDate = w.time()
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDate();
                return workoutDate.equals(date);
            })
            .collect(Collectors.toList());
        DateTimeFormatter titlePattern = DateTimeFormatter.ofPattern("dd MMMM EEEE", Locale.ENGLISH);

        JDialog detailDialog = new JDialog(owner, titlePattern.format(date), JDialog.ModalityType.APPLICATION_MODAL);
        detailDialog.setSize(540, 600); detailDialog.setLocationRelativeTo(this);
        
        JPanel mainContainer = new JPanel(new BorderLayout(0, 14));
        mainContainer.setBackground(PANEL_BG); mainContainer.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        // 1. 頂部區：當日運動紀錄清單
        JPanel topPanel = new JPanel(); 
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS)); 
        topPanel.setOpaque(false);
        
        JLabel mainHeader = new JLabel("當日運動紀錄");
        mainHeader.setFont(new Font(FONT_FAMILY, Font.PLAIN, 15)); 
        mainHeader.setForeground(TEXT); 
        // 【需求 1 修正】強制將單一組件的水平對齊錨點拉至左端，達成完美左貼
        mainHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        topPanel.add(mainHeader); topPanel.add(Box.createRigidArea(new Dimension(1, 6)));

        DefaultListModel<String> listModel = new DefaultListModel<>();
        if (dayWorkouts.isEmpty()) {
            listModel.addElement(" 尚無運動紀錄。");
        } else {
            for (WorkoutEntry w : dayWorkouts) {
                var exInfo = ExerciseRegistry.getExercise(w.getExerciseName());
                if (exInfo != null && exInfo.isAerobic()) {
                    listModel.addElement(String.format("  %s [%d 分鐘]", w.getExerciseName(), w.amount()));
                } else {
                    String weightStr = w.weight() == 0 ? "自重" : w.weight() + "kg";
                    listModel.addElement(String.format("  %s [%s × %d下 × %d組]", w.getExerciseName(), weightStr, w.amount(), w.sets()));
                }
            }
        }
        JList<String> jList = new JList<>(listModel);
        jList.setBackground(CELL_BG); jList.setForeground(MUTED); jList.setFont(new Font(FONT_FAMILY, Font.PLAIN, 13));
        
        JScrollPane scroll = new JScrollPane(jList); scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        // 【需求 1 修正】高度由 140 擴展至 180 大視界，完美解鎖更大寬容空間
        scroll.setPreferredSize(new Dimension(480, 180)); 
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT); // 強制 ScrollPane 也置左
        
        topPanel.add(scroll);
        mainContainer.add(topPanel, BorderLayout.NORTH);

        // 2. 中部區：當日運動提醒
        JPanel centerPanel = new JPanel(new BorderLayout(0, 8)); centerPanel.setOpaque(false);
        JLabel schedTitle = new JLabel("當日運動提醒"); 
        schedTitle.setFont(new Font(FONT_FAMILY, Font.PLAIN, 15)); schedTitle.setForeground(TEXT);
        centerPanel.add(schedTitle, BorderLayout.NORTH);

        DefaultTableModel schedModel = new DefaultTableModel(new String[]{"時間", "備註"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable schedTable = new JTable(schedModel);
        schedTable.setRowHeight(32); schedTable.setBackground(CELL_BG); schedTable.setForeground(TEXT);
        schedTable.setGridColor(BORDER); schedTable.setFont(new Font(FONT_FAMILY, Font.PLAIN, 13));
        
        // 【需求 2 修正】強制重構 JTableHeader 表格欄位名稱渲染器，全面灌注亮白字，終結隱形字 Bug！
        JTableHeader tableHeader = schedTable.getTableHeader();
        tableHeader.setDefaultRenderer((table1, value, isSelected, hasFocus, row, column) -> {
            JLabel headerLabel = new JLabel(String.valueOf(value), SwingConstants.LEFT);
            headerLabel.setOpaque(true);
            headerLabel.setBackground(PANEL_BG); 
            headerLabel.setForeground(TEXT); // 亮白字體
            headerLabel.setFont(new Font(FONT_FAMILY, Font.BOLD, 13));
            headerLabel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 1, BORDER),
                    BorderFactory.createEmptyBorder(4, 8, 4, 8)
            ));
            return headerLabel;
        });
        
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean isS, boolean hasF, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, isS, hasF, r, c);
                l.setForeground(TEXT); l.setBackground(isS ? new Color(0x3a4354) : CELL_BG);
                l.setFont(new Font(FONT_FAMILY, Font.PLAIN, 13));
                return l;
            }
        };
        schedTable.setDefaultRenderer(Object.class, cellRenderer);

        schedTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        schedTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        schedTable.getColumnModel().getColumn(0).setMaxWidth(100);

        int savedCount = Integer.parseInt(planProperties.getProperty("plan." + date + ".count", "0"));
        for (int i = 0; i < savedCount; i++) {
            String time = planProperties.getProperty("plan." + date + "." + i + ".time");
            String note = planProperties.getProperty("plan." + date + "." + i + ".note", "-");
            schedModel.addRow(new Object[]{time, note});
        }
        JScrollPane schedScroll = new JScrollPane(schedTable); schedScroll.setBorder(BorderFactory.createLineBorder(BORDER));
        schedScroll.getViewport().setBackground(CELL_BG);
        centerPanel.add(schedScroll, BorderLayout.CENTER);

        // 需求 8：將「＋新增提醒」按鈕安置在表格區塊的右下角
        JPanel subControlRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0)); subControlRow.setOpaque(false);
        JButton addSchedBtn = new JButton("＋ 新增提醒");
        addSchedBtn.setFont(new Font(FONT_FAMILY, Font.BOLD, 12)); addSchedBtn.setBackground(new Color(0x323846)); addSchedBtn.setForeground(ACCENT);
        addSchedBtn.setFocusPainted(false); addSchedBtn.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        
        addSchedBtn.addActionListener(e -> {
            String[] hours = new String[24]; for(int i=0; i<24; i++) hours[i] = String.format("%02d", i);
            String[] mins = {"00", "10", "20", "30", "40", "50"};

            // 【需求 4 修正】全面強刷為高強度白底黑字（Color.WHITE 與 Color.BLACK），杜絕暗色黑底隱形！
            JComboBox<String> hBox = new JComboBox<>(hours); hBox.setBackground(Color.WHITE); hBox.setForeground(Color.BLACK);
            JComboBox<String> mBox = new JComboBox<>(mins); mBox.setBackground(Color.WHITE); mBox.setForeground(Color.BLACK);
            
            JTextField noteField = new JTextField(); 
            noteField.setBackground(Color.WHITE); // 強制白底
            noteField.setForeground(Color.BLACK); // 強制黑字
            noteField.setCaretColor(Color.BLACK); 
            noteField.setFont(new Font(FONT_FAMILY, Font.PLAIN, 14));

            JPanel pickerForm = new JPanel(new GridLayout(3, 1, 0, 8));
            pickerForm.setOpaque(false);
            
            JPanel timeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0)); timeRow.setOpaque(false);
            JLabel labelH = new JLabel("時間設定："); labelH.setForeground(Color.BLACK); labelH.setFont(new Font(FONT_FAMILY, Font.BOLD, 13));
            timeRow.add(labelH); timeRow.add(hBox); timeRow.add(new JLabel(" : ") {{ setForeground(Color.BLACK); }}); timeRow.add(mBox);
            
            JLabel labelN = new JLabel("簡短備註："); labelN.setForeground(Color.BLACK); labelN.setFont(new Font(FONT_FAMILY, Font.BOLD, 13));
            pickerForm.add(timeRow); pickerForm.add(labelN); pickerForm.add(noteField);

            // 需求 3：視窗外框名稱精準修正正名為 "設置新提醒"
            int select = JOptionPane.showConfirmDialog(detailDialog, pickerForm, "設置新提醒", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (select == JOptionPane.OK_OPTION) {
                String targetTime = hBox.getSelectedItem() + ":" + mBox.getSelectedItem();
                String targetNote = noteField.getText().trim();
                if (targetNote.isEmpty()) targetNote = "-"; // 需求 2 保底
                schedModel.addRow(new Object[]{targetTime, targetNote});
            }
        });
        subControlRow.add(addSchedBtn);
        centerPanel.add(subControlRow, BorderLayout.SOUTH);
        mainContainer.add(centerPanel, BorderLayout.CENTER);

        // 3. 底部全域按鈕橫列
        JPanel bottomRow = new JPanel(new BorderLayout()); bottomRow.setOpaque(false);

        // 需求 9：「刪除所選提醒」精準正名並安置在整個視窗的最左下角
        JButton deleteSchedBtn = new JButton("刪除所選提醒");
        deleteSchedBtn.setFont(new Font(FONT_FAMILY, Font.PLAIN, 12));
        deleteSchedBtn.setBackground(new Color(0x40313a)); deleteSchedBtn.setForeground(new Color(0xff6b7a));
        deleteSchedBtn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16)); deleteSchedBtn.setFocusPainted(false);
        deleteSchedBtn.addActionListener(ev -> {
            int selectedRow = schedTable.getSelectedRow();
            if (selectedRow >= 0) schedModel.removeRow(selectedRow);
        });

        // 需求 10：「確認並儲存」精準正名並置於最右下角
        JButton saveAllBtn = new JButton("確認並儲存");
        saveAllBtn.setFont(new Font(FONT_FAMILY, Font.BOLD, 13));
        saveAllBtn.setBackground(COLOR_PLAN_GREEN); saveAllBtn.setForeground(Color.WHITE);
        saveAllBtn.setBorderPainted(false); saveAllBtn.setFocusPainted(false);
        saveAllBtn.setBorder(BorderFactory.createEmptyBorder(8, 24, 8, 24));

        saveAllBtn.addActionListener(e -> {
            int oldCount = Integer.parseInt(planProperties.getProperty("plan." + date + ".count", "0"));
            for (int i = 0; i < oldCount; i++) {
                planProperties.remove("plan." + date + "." + i + ".time");
                planProperties.remove("plan." + date + "." + i + ".note");
            }
            int newCount = schedModel.getRowCount();
            planProperties.setProperty("plan." + date + ".count", String.valueOf(newCount));
            for (int i = 0; i < newCount; i++) {
                planProperties.setProperty("plan." + date + "." + i + ".time", String.valueOf(schedModel.getValueAt(i, 0)));
                planProperties.setProperty("plan." + date + "." + i + ".note", String.valueOf(schedModel.getValueAt(i, 1)));
            }
            savePlanProperties(); gridPanel.repaint(); detailDialog.dispose();
        });

        bottomRow.add(deleteSchedBtn, BorderLayout.WEST); bottomRow.add(saveAllBtn, BorderLayout.EAST);
        mainContainer.add(bottomRow, BorderLayout.SOUTH);

        detailDialog.setContentPane(mainContainer); detailDialog.setVisible(true);
    }
}