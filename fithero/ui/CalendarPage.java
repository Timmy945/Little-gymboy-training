package fithero.ui;

import fithero.logic.manager.PlayerState;
import fithero.model.exercise.ExerciseInfo;
import fithero.model.exercise.ExerciseRegistry;
import fithero.model.exercise.MuscleGroup;
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
import java.awt.RenderingHints; // 🛠️ 補上漏掉的繪圖渲染提示
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
import javax.swing.DefaultListModel;
import javax.swing.SwingConstants;

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

    private static final String FONT_FAMILY = "Microsoft JhengHei";

    // 🛠️ 修正點：定義底部與繪製一致的配色指針
    private static final Color COLOR_CHEST = new Color(0xef4444); 
    private static final Color COLOR_BACK = new Color(0xf97316);  
    private static final Color COLOR_LEGS = new Color(0xeab308);  
    private static final Color COLOR_ARMS = new Color(0x22c55e);  
    private static final Color COLOR_CORE = new Color(0x3b82f6);  
    private static final Color COLOR_AEROBIC = new Color(0xa855f7); 
    private static final Color COLOR_PLAN_GREEN = new Color(0x10b981); 

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
            String isTrainKey = "plan." + checkDate + ".is_train";
            String statusKey = "plan." + checkDate + ".status";

            if ("true".equals(planProperties.getProperty(isTrainKey)) && planProperties.getProperty(statusKey) == null) {
                boolean hasWorkout = workouts.stream()
                        .anyMatch(w -> w.time().toLocalDate().equals(checkDate));

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
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);

        JPanel centerCtrl = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 0));
        centerCtrl.setOpaque(false);

        JButton prevBtn = navButton("◀");
        prevBtn.addActionListener(e -> changeMonth(-1));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy 年 M 月", Locale.TAIWAN);
        titleLabel = new JLabel(formatter.format(currentMonth), SwingConstants.CENTER);
        titleLabel.setFont(new Font(FONT_FAMILY, Font.BOLD, 24));
        titleLabel.setForeground(TEXT);
        titleLabel.setPreferredSize(new Dimension(180, 44));

        JButton nextBtn = navButton("▶");
        nextBtn.addActionListener(e -> changeMonth(1));

        centerCtrl.add(prevBtn);
        centerCtrl.add(titleLabel);
        centerCtrl.add(nextBtn);

        JPanel spacerL = new JPanel() {{ setOpaque(false); setPreferredSize(new Dimension(52, 44)); }};
        JPanel spacerR = new JPanel() {{ setOpaque(false); setPreferredSize(new Dimension(52, 44)); }};

        header.add(spacerL, BorderLayout.WEST);
        header.add(centerCtrl, BorderLayout.CENTER);
        header.add(spacerR, BorderLayout.EAST);
        return header;
    }

    private JButton navButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font(FONT_FAMILY, Font.BOLD, 16));
        btn.setForeground(TEXT); btn.setBackground(PANEL_BG);
        btn.setOpaque(true); btn.setContentAreaFilled(true); btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(44, 38));
        btn.setBorder(BorderFactory.createLineBorder(BORDER));
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
            ExerciseInfo scienceInfo = ExerciseRegistry.getExercise(workout.getExerciseName());
            Color targetColor = COLOR_AEROBIC; 
            
            if (scienceInfo != null) {
                if (scienceInfo.isAerobic()) {
                    targetColor = COLOR_AEROBIC; 
                } else {
                    targetColor = switch (scienceInfo.getTargetMuscle()) {
                        case CHEST -> COLOR_CHEST;   
                        case BACK -> COLOR_BACK;     
                        case LEGS -> COLOR_LEGS;     
                        case ARMS -> COLOR_ARMS;     
                        case ABS -> COLOR_CORE;     
                    };
                }
            }
            
            markers.computeIfAbsent(date, key -> new ArrayList<>());
            if (!markers.get(date).contains(targetColor)) {
                markers.get(date).add(targetColor);
            }
        }
        return markers;
    }

    private JPanel createLegendPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        panel.setOpaque(false);
        panel.add(new JLabel("<html><span style='color:#ef4444;'>●</span> 胸肌紅</html>"));
        panel.add(new JLabel("<html><span style='color:#f97316;'>●</span> 背肌橙</html>"));
        panel.add(new JLabel("<html><span style='color:#eab308;'>●</span> 腿部黃</html>"));
        panel.add(new JLabel("<html><span style='color:#22c55e;'>●</span> 手臂綠</html>"));
        panel.add(new JLabel("<html><span style='color:#3b82f6;'>●</span> 核心藍</html>"));
        panel.add(new JLabel("<html><span style='color:#a855f7;'>●</span> 有氧紫</html>"));
        panel.add(new JLabel("<html><span style='color:#10b981;'>■</span> 預約訓練日</html>"));
        
        for (Component c : panel.getComponents()) {
            c.setForeground(MUTED);
            c.setFont(new Font(FONT_FAMILY, Font.BOLD, 12));
        }
        return panel;
    }

    private class CalendarGridPanel extends JPanel {
        private final String[] weekdays = {"日", "一", "二", "三", "四", "五", "六"};
        private int cachedLeft = 18;
        private int cachedGridTop = 0;
        private int cachedCellWidth = 0;
        private int cachedCellHeight = 0;
        private int cachedGap = 8;

        CalendarGridPanel() {
            setBackground(PANEL_BG);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER), BorderFactory.createEmptyBorder(14, 14, 14, 14)
            ));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    handleGridClick(e.getX(), e.getY());
                }
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
                LocalDate clickedDate = firstDay.plusDays(index - leadingDays);

                showDayWorkoutsDetails(clickedDate);
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

            this.cachedLeft = left;
            this.cachedGridTop = gridTop;
            this.cachedCellWidth = cellWidth;
            this.cachedCellHeight = cellHeight;
            this.cachedGap = gap;

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
                int labelWidth = metrics.stringWidth(weekdays[i]);
                g.drawString(weekdays[i], x + (cellWidth - labelWidth) / 2, top + 22);
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
                boolean inMonth = YearMonth.from(date).equals(currentMonth);
                boolean today = date.equals(LocalDate.now());

                drawCell(g, x, y, cellWidth, cellHeight, column, date, inMonth, today, markers.get(date));
            }
        }

        private void drawCell(Graphics2D g, int x, int y, int width, int height, int column, LocalDate date, boolean inMonth, boolean today, List<Color> markers) {
            boolean isPlanDay = "true".equals(planProperties.getProperty("plan." + date + ".is_train"));

            g.setColor(today ? new Color(0x263b55) : CELL_BG);
            g.fill(new RoundRectangle2D.Double(x, y, width, height, 10, 10));
            
            g.setColor(today ? ACCENT : (isPlanDay ? COLOR_PLAN_GREEN : BORDER));
            g.setStroke(new BasicStroke((today || isPlanDay) ? 2f : 1f));
            g.draw(new RoundRectangle2D.Double(x, y, width, height, 10, 10));

            Color dayColor = colorForColumn(column);
            if (!inMonth) {
                dayColor = new Color(dayColor.getRed(), dayColor.getGreen(), dayColor.getBlue(), 95);
            }

            g.setColor(dayColor);
            g.setFont(new Font(FONT_FAMILY, Font.BOLD, 18));
            g.drawString(String.valueOf(date.getDayOfMonth()), x + 14, y + 30);

            if (isPlanDay && inMonth) {
                g.setFont(new Font(FONT_FAMILY, Font.PLAIN, 11));
                g.setColor(COLOR_PLAN_GREEN);
                String timeHint = planProperties.getProperty("plan." + date + ".time", "08:00");
                g.drawString(timeHint, x + width - 52, y + 20);
            }

            if (markers != null && !markers.isEmpty() && inMonth) {
                int markerWidth = Math.min(20, Math.max(10, (width - 40) / Math.min(6, markers.size())));
                int totalWidth = markerWidth * Math.min(6, markers.size()) + 5 * (Math.min(6, markers.size()) - 1);
                int markerX = x + (width - totalWidth) / 2;
                int markerY = y + height - 16;
                for (int i = 0; i < Math.min(6, markers.size()); i++) {
                    g.setColor(markers.get(i));
                    g.fillRoundRect(markerX + i * (markerWidth + 5), markerY, markerWidth, 5, 4, 4);
                }
            }

            String status = planProperties.getProperty("plan." + date + ".status");
            if ("lazy".equals(status) && inMonth) {
                g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(new Color(0x6b7280)); 
                int padding = 22;
                g.drawLine(x + padding, y + padding, x + width - padding, y + height - padding);
                g.drawLine(x + width - padding, y + padding, x + padding, y + height - padding);
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
                .filter(w -> w.time().toLocalDate().equals(date))
                .collect(Collectors.toList());

        JDialog detailDialog = new JDialog(owner, "日期核心看板 [" + date + "]", JDialog.ModalityType.APPLICATION_MODAL);
        detailDialog.setSize(440, 420);
        detailDialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(PANEL_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JLabel infoTitle = new JLabel("日期特徵面板: " + date, SwingConstants.CENTER);
        infoTitle.setFont(new Font(FONT_FAMILY, Font.BOLD, 18)); infoTitle.setForeground(TEXT);
        panel.add(infoTitle, BorderLayout.NORTH);

        JPanel centerContainer = new JPanel();
        centerContainer.setLayout(new BoxLayout(centerContainer, BoxLayout.Y_AXIS));
        centerContainer.setOpaque(false);

        JPanel planCard = new JPanel(new GridLayout(2, 2, 8, 8));
        planCard.setBackground(CELL_BG);
        planCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        JCheckBox trainDayCheck = new JCheckBox("設定此日為【預期訓練計畫日】");
        trainDayCheck.setFont(new Font(FONT_FAMILY, Font.BOLD, 13));
        trainDayCheck.setForeground(TEXT); trainDayCheck.setOpaque(false);
        boolean initiallyChecked = "true".equals(planProperties.getProperty("plan." + date + ".is_train"));
        trainDayCheck.setSelected(initiallyChecked);

        JPanel timePickerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        timePickerPanel.setOpaque(false);
        
        String[] hours = new String[24]; for(int i=0; i<24; i++) hours[i] = String.format("%02d", i);
        String[] mins = {"00", "15", "30", "45"};
        
        JComboBox<String> hourBox = new JComboBox<>(hours);
        JComboBox<String> minBox = new JComboBox<>(mins);
        
        String savedTime = planProperties.getProperty("plan." + date + ".time", "08:00");
        String[] timeParts = savedTime.split(":");
        if (timeParts.length == 2) {
            hourBox.setSelectedItem(timeParts[0]);
            minBox.setSelectedItem(timeParts[1]);
        }

        JLabel timeLabel = new JLabel("預定提醒時間："); timeLabel.setFont(new Font(FONT_FAMILY, Font.PLAIN, 13)); timeLabel.setForeground(MUTED);
        timePickerPanel.add(timeLabel); timePickerPanel.add(hourBox); timePickerPanel.add(new JLabel(":") {{ setForeground(TEXT); }}); timePickerPanel.add(minBox);

        planCard.add(trainDayCheck);
        planCard.add(timePickerPanel);
        centerContainer.add(planCard);
        centerContainer.add(Box.createRigidArea(new Dimension(1, 14)));

        JLabel logLabel = new JLabel("當日科學完訓成果："); logLabel.setForeground(MUTED); logLabel.setFont(new Font(FONT_FAMILY, Font.BOLD, 13));
        logLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerContainer.add(logLabel);
        centerContainer.add(Box.createRigidArea(new Dimension(1, 6)));

        DefaultListModel<String> listModel = new DefaultListModel<>();
        if (dayWorkouts.isEmpty()) {
            listModel.addElement(" 這天尚無任何完訓打卡紀錄。");
        } else {
            for (WorkoutEntry w : dayWorkouts) {
                var exInfo = ExerciseRegistry.getExercise(w.getExerciseName());
                if (exInfo != null && exInfo.isAerobic()) {
                    listModel.addElement(String.format(" %s [%d 分鐘] ➔ +%d XP", w.getExerciseName(), w.amount(), w.xp()));
                } else {
                    String weightStr = w.weight() == 0 ? "自重" : w.weight() + "kg";
                    listModel.addElement(String.format(" %s [%s × %d下 × %d組] ➔ +%d XP", w.getExerciseName(), weightStr, w.amount(), w.sets(), w.xp()));
                }
            }
        }

        JList<String> jList = new JList<>(listModel);
        jList.setBackground(CELL_BG); jList.setForeground(TEXT); jList.setFont(new Font(FONT_FAMILY, Font.PLAIN, 13));
        JScrollPane scroll = new JScrollPane(jList);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerContainer.add(scroll);

        panel.add(centerContainer, BorderLayout.CENTER);

        JButton saveBtn = new JButton("儲存計畫並關閉");
        saveBtn.setFont(new Font(FONT_FAMILY, Font.BOLD, 14));
        saveBtn.setBackground(ACCENT); saveBtn.setForeground(Color.WHITE); saveBtn.setBorderPainted(false);
        
        saveBtn.addActionListener(e -> {
            String planKey = "plan." + date + ".is_train";
            String timeKey = "plan." + date + ".time";
            
            if (trainDayCheck.isSelected()) {
                planProperties.setProperty(planKey, "true");
                planProperties.setProperty(timeKey, hourBox.getSelectedItem() + ":" + minBox.getSelectedItem());
                System.out.println("[FitQuest 提醒核心] 已成功預約通知！" + date + " " + hourBox.getSelectedItem() + ":" + minBox.getSelectedItem());
            } else {
                planProperties.remove(planKey);
                planProperties.remove(timeKey);
            }
            savePlanProperties(); 
            gridPanel.repaint(); 
            detailDialog.dispose();
        });
        panel.add(saveBtn, BorderLayout.SOUTH);

        detailDialog.setContentPane(panel);
        detailDialog.setVisible(true);
    }
}