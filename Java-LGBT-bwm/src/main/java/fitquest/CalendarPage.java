package fitquest;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.geom.RoundRectangle2D;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class CalendarPage extends JPanel {
    private static final Color APP_BG = new Color(0x1e222b);
    private static final Color PANEL_BG = new Color(0x282c37);
    private static final Color CELL_BG = new Color(0x20242d);
    private static final Color BORDER = new Color(0x3a4050);
    private static final Color TEXT = new Color(0xf4f6fb);
    private static final Color MUTED = new Color(0xb7c0d1);
    private static final Color ACCENT = new Color(0x2f9bff);
    private static final Color SUNDAY = new Color(0xff6b7a);
    private static final Color SATURDAY = new Color(0x72b7ff);

    private static final DateTimeFormatter MONTH_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy 年 M 月", Locale.TAIWAN);

    private final Window owner;
    private final List<WorkoutEntry> workouts;
    private final CalendarGridPanel calendarGrid;

    public CalendarPage(Window owner, List<WorkoutEntry> workouts) {
        this.owner = owner;
        this.workouts = workouts;
        this.calendarGrid = new CalendarGridPanel();

        setLayout(new BorderLayout(0, 16));
        setBackground(APP_BG);
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        add(createHeader(), BorderLayout.NORTH);
        add(calendarGrid, BorderLayout.CENTER);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JButton settingsButton = new JButton("⚙");
        settingsButton.setFont(new Font("Dialog", Font.BOLD, 22));
        settingsButton.setForeground(TEXT);
        settingsButton.setBackground(PANEL_BG);
        settingsButton.setOpaque(true);
        settingsButton.setContentAreaFilled(true);
        settingsButton.setBorderPainted(false);
        settingsButton.setFocusPainted(false);
        settingsButton.setToolTipText("設定");
        settingsButton.setPreferredSize(new Dimension(52, 44));
        settingsButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        settingsButton.addActionListener(event -> showSettingsSheet());

        JLabel title = new JLabel(MONTH_FORMATTER.format(YearMonth.now()), SwingConstants.CENTER);
        title.setFont(new Font("Dialog", Font.BOLD, 26));
        title.setForeground(TEXT);

        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        spacer.setPreferredSize(new Dimension(52, 44));

        header.add(settingsButton, BorderLayout.WEST);
        header.add(title, BorderLayout.CENTER);
        header.add(spacer, BorderLayout.EAST);
        return header;
    }

    private void showSettingsSheet() {
        JDialog dialog = new JDialog(owner, "日曆設定");
        dialog.setModal(false);
        dialog.setUndecorated(true);
        dialog.setContentPane(createSettingsContent(dialog));
        dialog.pack();

        int width = Math.min(620, Math.max(480, owner.getWidth() - 220));
        int height = dialog.getPreferredSize().height;
        int x = owner.getX() + (owner.getWidth() - width) / 2;
        int y = owner.getY() + owner.getHeight() - height - 104;
        dialog.setSize(width, height);
        dialog.setLocation(x, y);
        dialog.setVisible(true);
    }

    private JPanel createSettingsContent(JDialog dialog) {
        JPanel content = new JPanel(new BorderLayout(0, 18));
        content.setBackground(PANEL_BG);
        content.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(20, 22, 20, 22)
        ));

        JLabel title = new JLabel("編輯顏色意義");
        title.setForeground(TEXT);
        title.setFont(new Font("Dialog", Font.BOLD, 20));

        JPanel editor = new JPanel(new GridLayout(3, 2, 12, 12));
        editor.setOpaque(false);
        editor.add(colorMeaningField("紅色", "練胸", new Color(0xdc4646)));
        editor.add(colorMeaningField("藍色", "練腿", new Color(0x4691e6)));
        editor.add(colorMeaningField("紫色", "練背", new Color(0x915fd2)));
        editor.add(colorMeaningField("橘色", "手臂", new Color(0xf0962d)));
        editor.add(colorMeaningField("綠色", "腿部", new Color(0x46aa6e)));
        editor.add(colorMeaningField("亮藍", "腹部 / 有氧", ACCENT));

        JButton confirm = new JButton("確認");
        confirm.setFont(new Font("Dialog", Font.BOLD, 16));
        confirm.setForeground(Color.WHITE);
        confirm.setBackground(ACCENT);
        confirm.setOpaque(true);
        confirm.setContentAreaFilled(true);
        confirm.setBorderPainted(false);
        confirm.setFocusPainted(false);
        confirm.setBorder(BorderFactory.createEmptyBorder(12, 26, 12, 26));
        confirm.addActionListener(event -> dialog.dispose());

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        footer.setOpaque(false);
        footer.add(confirm);

        content.add(title, BorderLayout.NORTH);
        content.add(editor, BorderLayout.CENTER);
        content.add(footer, BorderLayout.SOUTH);
        return content;
    }

    private JPanel colorMeaningField(String colorName, String meaning, Color color) {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setOpaque(false);
        panel.add(new PillLabel("● " + colorName, color), BorderLayout.WEST);

        JTextField field = new JTextField(meaning);
        field.setForeground(TEXT);
        field.setBackground(CELL_BG);
        field.setCaretColor(TEXT);
        field.setFont(new Font("Dialog", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    private Map<LocalDate, List<Color>> workoutMarkers() {
        Map<LocalDate, List<Color>> markers = new HashMap<>();
        for (WorkoutEntry workout : workouts) {
            LocalDate date = workout.time().toLocalDate();
            Color color = workout.type().mainMuscle().color();
            markers.computeIfAbsent(date, key -> new ArrayList<>());
            if (!markers.get(date).contains(color)) {
                markers.get(date).add(color);
            }
        }
        return markers;
    }

    private class CalendarGridPanel extends JPanel {
        private final String[] weekdays = {"日", "一", "二", "三", "四", "五", "六"};

        CalendarGridPanel() {
            setBackground(PANEL_BG);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER),
                    BorderFactory.createEmptyBorder(14, 14, 14, 14)
            ));
            setPreferredSize(new Dimension(900, 560));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int left = 18;
            int top = 18;
            int weekdayHeight = 34;
            int gap = 8;
            int gridTop = top + weekdayHeight + 8;
            int cellWidth = (width - left * 2 - gap * 6) / 7;
            int cellHeight = (height - gridTop - 18 - gap * 5) / 6;

            drawWeekdays(g, left, top, cellWidth, gap);
            drawDates(g, left, gridTop, cellWidth, cellHeight, gap);

            g.dispose();
        }

        private void drawWeekdays(Graphics2D g, int left, int top, int cellWidth, int gap) {
            g.setFont(new Font("Dialog", Font.BOLD, 15));
            FontMetrics metrics = g.getFontMetrics();
            for (int i = 0; i < weekdays.length; i++) {
                int x = left + i * (cellWidth + gap);
                g.setColor(colorForColumn(i));
                int labelWidth = metrics.stringWidth(weekdays[i]);
                g.drawString(weekdays[i], x + (cellWidth - labelWidth) / 2, top + 22);
            }
        }

        private void drawDates(Graphics2D g, int left, int top, int cellWidth, int cellHeight, int gap) {
            YearMonth month = YearMonth.now();
            LocalDate firstDay = month.atDay(1);
            int leadingDays = firstDay.getDayOfWeek().getValue() % 7;
            Map<LocalDate, List<Color>> markers = workoutMarkers();

            g.setFont(new Font("Dialog", Font.BOLD, 18));
            for (int index = 0; index < 42; index++) {
                int row = index / 7;
                int column = index % 7;
                int x = left + column * (cellWidth + gap);
                int y = top + row * (cellHeight + gap);
                LocalDate date = firstDay.plusDays(index - leadingDays);
                boolean inMonth = YearMonth.from(date).equals(month);
                boolean today = date.equals(LocalDate.now());

                drawCell(g, x, y, cellWidth, cellHeight, column, date, inMonth, today, markers.get(date));
            }
        }

        private void drawCell(
                Graphics2D g,
                int x,
                int y,
                int width,
                int height,
                int column,
                LocalDate date,
                boolean inMonth,
                boolean today,
                List<Color> markers
        ) {
            g.setColor(today ? new Color(0x263b55) : CELL_BG);
            g.fill(new RoundRectangle2D.Double(x, y, width, height, 10, 10));
            g.setColor(today ? ACCENT : BORDER);
            g.setStroke(new BasicStroke(today ? 2f : 1f));
            g.draw(new RoundRectangle2D.Double(x, y, width, height, 10, 10));

            Color dayColor = colorForColumn(column);
            if (!inMonth) {
                dayColor = new Color(dayColor.getRed(), dayColor.getGreen(), dayColor.getBlue(), 95);
            }

            g.setColor(dayColor);
            g.setFont(new Font("Dialog", Font.BOLD, 18));
            g.drawString(String.valueOf(date.getDayOfMonth()), x + 14, y + 30);

            if (markers != null && !markers.isEmpty() && inMonth) {
                int markerWidth = Math.min(20, Math.max(10, (width - 40) / Math.min(4, markers.size())));
                int totalWidth = markerWidth * Math.min(4, markers.size()) + 6 * (Math.min(4, markers.size()) - 1);
                int markerX = x + (width - totalWidth) / 2;
                int markerY = y + height - 18;
                for (int i = 0; i < Math.min(4, markers.size()); i++) {
                    g.setColor(markers.get(i));
                    g.fillRoundRect(markerX + i * (markerWidth + 6), markerY, markerWidth, 6, 6, 6);
                }
            }
        }

        private Color colorForColumn(int column) {
            if (column == DayOfWeek.SUNDAY.getValue() % 7) {
                return SUNDAY;
            }
            if (column == DayOfWeek.SATURDAY.getValue() % 7) {
                return SATURDAY;
            }
            return TEXT;
        }
    }

    private static class PillLabel extends JLabel {
        private final Color accent;

        PillLabel(String text, Color accent) {
            super(text);
            this.accent = accent;
            setForeground(TEXT);
            setFont(new Font("Dialog", Font.BOLD, 15));
            setBorder(BorderFactory.createEmptyBorder(9, 14, 9, 14));
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            return new Dimension(size.width + 12, size.height + 2);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 45));
            g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g.setColor(accent);
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
            g.dispose();
            super.paintComponent(graphics);
        }
    }
}
