package fithero;

import fithero.ui.FitQuestFrame;
import fithero.ui.CalendarPage;
import fithero.logic.manager.PlayerState;
import fithero.infra.Storage;
import fithero.model.player.Gender;

import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import javax.sound.sampled.*;
import javax.swing.*;

public class FitQuestApp {
    private static final Color APP_BG = new Color(0x1e222b);
    private static final Color PANEL_BG = new Color(0x282c37);
    private static final Color TEXT = new Color(0xf4f6fb);
    private static final Color BORDER = new Color(0x3a4050);
    private static final Color ACCENT = new Color(0x5aa9ff);
    private static final Color CTA_BLUE = new Color(59, 130, 246);

    private static SystemTray tray;
    private static TrayIcon trayIcon;
    private static final Set<String> notifiedSet = new HashSet<>();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            initSystemTrayNotification();

            Path savePath = Path.of("data", "player.properties");
            if (Files.exists(savePath)) {
                launchMainFrame();
            } else {
                showFirstTimeRegistration();
            }
        });
    }

    private static void launchMainFrame() {
        FitQuestFrame mainFrame = new FitQuestFrame();
        mainFrame.setVisible(true);

        for (Component comp : mainFrame.getContentPane().getComponents()) {
            if (comp instanceof JPanel) {
                for (Component subComp : ((JPanel) comp).getComponents()) {
                    if (subComp instanceof CalendarPage) {
                        ((CalendarPage) subComp).runBootUpStreakCheck(mainFrame.getPlayerState());
                    }
                }
            }
        }
        startBackgroundNotificationRadar(mainFrame);
    }

    private static void initSystemTrayNotification() {
        if (!SystemTray.isSupported()) return;
        try {
            tray = SystemTray.getSystemTray();
            Image image = Toolkit.getDefaultToolkit().createImage(new byte[0]);
            trayIcon = new TrayIcon(image, "FitQuest Scientific Radar");
            trayIcon.setImageAutoSize(true);
            tray.add(trayIcon);
        } catch (Exception ignored) {}
    }

    private static void startBackgroundNotificationRadar(FitQuestFrame mainFrame) {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Path planPath = Path.of("data", "custom_plans.properties");
                if (!Files.exists(planPath)) return;

                Properties planProps = new Properties();
                try (var reader = Files.newBufferedReader(planPath, StandardCharsets.UTF_8)) {
                    planProps.load(reader);
                    
                    LocalDate today = LocalDate.now();
                    int count = Integer.parseInt(planProps.getProperty("plan." + today + ".count", "0"));

                    LocalTime nowTime = LocalTime.now();
                    String currentTimeStr = String.format("%02d:%02d", nowTime.getHour(), nowTime.getMinute());

                    for (int i = 0; i < count; i++) {
                        String scheduledTime = planProps.getProperty("plan." + today + "." + i + ".time");
                        String note = planProps.getProperty("plan." + today + "." + i + ".note", "-");

                        if (currentTimeStr.equals(scheduledTime)) {
                            String uniqueKey = today + "@" + scheduledTime + "@" + note;
                            if (notifiedSet.contains(uniqueKey)) continue;

                            // 【需求 1 修正】拋棄無聲的蜂鳴器，改用 100% 響起之音效流播放 1000Hz 科技警報音
                            playAlertSound();

                            String userName = mainFrame.getPlayerState().getAvatar().getName();
                            SwingUtilities.invokeLater(() -> showJuicyToastNotification(userName, note, scheduledTime));

                            if (trayIcon != null) {
                                trayIcon.displayMessage("FitQuest 訓練警報", "[" + scheduledTime + "] " + note, TrayIcon.MessageType.INFO);
                            }
                            notifiedSet.add(uniqueKey);
                        }
                    }
                } catch (Exception ignored) {}
            }
        }, 0, 1000 * 5); 
    }

    /**
     * 【音效引擎升級】模擬現代通訊軟體 (LINE風格) 的清脆雙音節「叮咚」和弦聲
     */
    private static void playAlertSound() {
        try {
            // 總共採樣 0.4 秒 (前 0.15 秒第一音節，後 0.25 秒第二音節)
            int sampleRate = 16000;
            byte[] buf = new byte[sampleRate * 4 / 10]; 
            
            for (int i = 0; i < buf.length; i++) {
                double time = (double) i / sampleRate;
                double frequency;
                double volumeFade = 1.0;

                if (i < sampleRate * 15 / 100) {
                    // 第一音節：880Hz (高音 A)，音量中等
                    frequency = 880.0;
                    volumeFade = 0.6;
                } else {
                    // 第二音節：1318.5Hz (高音 E)，音量較亮，並隨時間線性遞減產生餘音效果
                    frequency = 1318.51;
                    double progress = (time - 0.15) / 0.25;
                    volumeFade = 1.0 - progress; // 漸弱效果
                }

                double angle = time * frequency * 2.0 * Math.PI;
                buf[i] = (byte) (Math.sin(angle) * 127.0 * volumeFade);
            }

            AudioFormat af = new AudioFormat(sampleRate, 8, 1, true, false);
            SourceDataLine sdl = AudioSystem.getSourceDataLine(af);
            sdl.open(af); sdl.start();
            sdl.write(buf, 0, buf.length);
            sdl.drain(); sdl.close();
        } catch (Exception ignored) {}
    }

    private static void showJuicyToastNotification(String user, String note, String time) {
        JDialog toast = new JDialog((Window) null);
        toast.setUndecorated(true); toast.setAlwaysOnTop(true); toast.setSize(360, 130);
        Dimension scr = Toolkit.getDefaultToolkit().getScreenSize();
        toast.setLocation(scr.width - 380, scr.height - 180);

        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setBackground(PANEL_BG); content.setBorder(BorderFactory.createLineBorder(ACCENT, 2));

        JLabel titleL = new JLabel("運動時間到了！ (" + time + ")", SwingConstants.LEFT);
        titleL.setFont(new Font("Microsoft JhengHei", Font.BOLD, 15)); titleL.setForeground(ACCENT);
        
        JLabel msgL = new JLabel("<html>" + user + " 該動起來了！<br> 當前排程：<span style='color:#f4f6fb; font-weight:bold;'>" + note + "</span></html>");
        msgL.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 13)); msgL.setForeground(TEXT);
        msgL.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));

        JLabel countdownL = new StringCountdownLabel(toast);
        countdownL.setHorizontalAlignment(SwingConstants.RIGHT); countdownL.setForeground(new Color(0xef4444));
        countdownL.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 12));

        content.add(titleL, BorderLayout.NORTH); content.add(msgL, BorderLayout.CENTER); content.add(countdownL, BorderLayout.SOUTH);
        toast.setContentPane(content); toast.setVisible(true);
    }

    private static class StringCountdownLabel extends JLabel {
        private int remaining = 10;
        StringCountdownLabel(JDialog target) {
            setFont(new Font("Microsoft JhengHei", Font.BOLD, 11));
            setText("視窗將於 " + remaining + " 秒後自動關閉 ");
            new javax.swing.Timer(1000, e -> {
                remaining--;
                if (remaining <= 0) {
                    ((javax.swing.Timer)e.getSource()).stop(); target.dispose();
                } else {
                    setText("視窗將於 " + remaining + " 秒後自動關閉 ");
                }
            }).start();
        }
    }

    private static void showFirstTimeRegistration() {
        JDialog regDialog = new JDialog((Window) null, "帳號註冊");
        regDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        regDialog.setSize(450, 480); regDialog.setResizable(false); regDialog.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(APP_BG);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        mainPanel.add(Box.createRigidArea(new Dimension(1, 4)));
        mainPanel.add(Box.createRigidArea(new Dimension(1, 28)));

        JPanel formPanel = new JPanel(new GridLayout(4, 2, 12, 24));
        formPanel.setOpaque(false);
        formPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 100));

        JTextField nameField = createStyledTextField("請輸入暱稱");
        JTextField heightField = createStyledTextField("175.0");
        JTextField weightField = createStyledTextField("70.0");
        JComboBox<Gender> genderBox = new JComboBox<>(Gender.values());
        genderBox.setFont(new Font("Microsoft JhengHei", Font.BOLD, 15));
        genderBox.setBackground(new Color(0x20242d)); genderBox.setForeground(Color.BLACK);

        formPanel.add(createFormLabel("角色暱稱："));   formPanel.add(nameField);
        formPanel.add(createFormLabel("身高 (cm)："));   formPanel.add(heightField);
        formPanel.add(createFormLabel("體重 (kg)："));   formPanel.add(weightField);
        formPanel.add(createFormLabel("玩家性別："));    formPanel.add(genderBox);

        mainPanel.add(formPanel); mainPanel.add(Box.createRigidArea(new Dimension(1, 32)));

        JButton submitBtn = new JButton("建構人偶並開始遊戲");
        submitBtn.setFont(new Font("Microsoft JhengHei", Font.BOLD, 16));
        submitBtn.setForeground(Color.WHITE); submitBtn.setBackground(CTA_BLUE);
        submitBtn.setOpaque(true); submitBtn.setContentAreaFilled(true); submitBtn.setBorderPainted(false); submitBtn.setFocusPainted(false);
        submitBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        submitBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        
        submitBtn.addActionListener(event -> {
            String name = nameField.getText().trim();
            if (name.isEmpty() || name.equals("請輸入暱稱")) {
                JOptionPane.showMessageDialog(regDialog, "請輸入有效的角色暱稱！", "序列錯誤", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                double height = Double.parseDouble(heightField.getText());
                double weight = Double.parseDouble(weightField.getText());
                Gender gender = (Gender) genderBox.getSelectedItem();

                PlayerState firstPlayer = new PlayerState(name, height, weight, gender);
                Path dataDir = Path.of("data");
                if (!Files.exists(dataDir)) Files.createDirectories(dataDir);
                
                Storage initialStorage = new Storage(dataDir);
                initialStorage.savePlayer(firstPlayer);

                regDialog.dispose();
                launchMainFrame();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(regDialog, "輸入數據校正錯誤！", "錯誤", JOptionPane.ERROR_MESSAGE);
            }
        });

        mainPanel.add(submitBtn); regDialog.setContentPane(mainPanel); regDialog.setVisible(true);
    }

    private static JLabel createFormLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.RIGHT);
        label.setFont(new Font("Microsoft JhengHei", Font.BOLD, 15)); label.setForeground(TEXT);
        return label;
    }

    private static JTextField createStyledTextField(String defaultText) {
        JTextField field = new JTextField(defaultText);
        field.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 15));
        field.setForeground(TEXT); field.setBackground(new Color(0x20242d)); field.setCaretColor(TEXT);
        field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER), BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        return field;
    }
}