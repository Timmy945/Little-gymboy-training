package fithero;

import fithero.model.player.Gender;
import fithero.ui.FitQuestFrame;
import fithero.ui.CalendarPage;
import fithero.logic.manager.PlayerState;
import fithero.infra.Storage;
import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.*;

/**
 * 應用程式啟動進入點：具備首次開機智慧分流與個人體態註冊系統。
 */
public class FitQuestApp {
    // 統一視覺風格色彩 (Color Palette)
    private static final Color APP_BG = new Color(0x1e222b);
    private static final Color PANEL_BG = new Color(0x282c37);
    private static final Color TEXT = new Color(0xf4f6fb);
    private static final Color BORDER = new Color(0x3a4050);
    private static final Color ACCENT = new Color(0x5aa9ff);
    private static final Color CTA_BLUE = new Color(59, 130, 246); // 移至上方統一管理

    private static SystemTray tray;
    private static TrayIcon trayIcon;
    private static LocalDate lastNotifiedDate = null; // 用於防止當天重複通知的擋板

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            // 初始化作業系統本機通知托盤
            initSystemTrayNotification();

            Path savePath = Path.of("data", "player.properties");

            // 【智慧分流】檢查是否有歷史存檔
            if (Files.exists(savePath)) {
                System.out.println("[啟動系統] 偵測到現有存檔，直接進入 FitQuest 控制中心。");
                launchMainFrame();
            } else {
                System.out.println("[啟動系統] 未偵測到存檔，觸發首次啟動生物特徵註冊機制。");
                showFirstTimeRegistration();
            }
        });
    }

    /**
     * 正式啟動遊戲主畫面與核心雷達
     */
    private static void launchMainFrame() {
        FitQuestFrame mainFrame = new FitQuestFrame();
        mainFrame.setVisible(true);

        // 【智慧連動點】開機啟動時，立即要求日曆分頁執行「昨日偷懶追溯結算與扣分」
        for (Component comp : mainFrame.getContentPane().getComponents()) {
            if (comp instanceof JPanel) {
                for (Component subComp : ((JPanel) comp).getComponents()) {
                    if (subComp instanceof CalendarPage) {
                        ((CalendarPage) subComp).runBootUpStreakCheck(mainFrame.getPlayerState());
                    }
                }
            }
        }

        // 啟動不滅背景雷達（確保 mainFrame 已生成並取得正確的 PlayerState）
        startBackgroundNotificationRadar(mainFrame);
    }

    /**
     * 初始化作業系統托盤防線
     */
    private static void initSystemTrayNotification() {
        if (!SystemTray.isSupported()) {
            System.out.println("[通知系統] 警告：當前作業系統不支援托盤通知。");
            return;
        }
        try {
            tray = SystemTray.getSystemTray();
            // 建立一個透明像素作為托盤圖示（若有專屬 icon 檔案，建議更換為 ImageIO.read）
            Image image = Toolkit.getDefaultToolkit().createImage(new byte[0]);
            trayIcon = new TrayIcon(image, "FitQuest Scientific Radar");
            trayIcon.setImageAutoSize(true);
            tray.add(trayIcon);
            System.out.println("[通知系統] 本機系統托盤攔截器初始化成功。");
        } catch (Exception e) {
            System.err.println("[通知系統] 托盤初始化失敗: " + e.getMessage());
        }
    }

    /**
     * 【不滅背景通知雷達】每 30 秒掃描一次硬碟計畫檔，時間到彈出 Windows 本機通知
     */
    private static void startBackgroundNotificationRadar(FitQuestFrame mainFrame) {
        Timer timer = new Timer(true); // 宣告為安全守護執行緒 (Daemon Thread)
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Path planPath = Path.of("data", "custom_plans.properties");
                if (!Files.exists(planPath)) return;

                Properties planProps = new Properties();
                try (var reader = Files.newBufferedReader(planPath, StandardCharsets.UTF_8)) {
                    planProps.load(reader);
                    
                    LocalDate today = LocalDate.now();
                    
                    // 如果今天已經跳過通知了，這天就不再重複讀取與彈窗
                    if (today.equals(lastNotifiedDate)) return;

                    String isTrain = planProps.getProperty("plan." + today + ".is_train");
                    String scheduledTime = planProps.getProperty("plan." + today + ".time"); // 格式範例 "16:45"
                    
                    if ("true".equals(isTrain) && scheduledTime != null) {
                        LocalTime nowTime = LocalTime.now();
                        String currentTimeStr = String.format("%02d:%02d", nowTime.getHour(), nowTime.getMinute());
                        
                        // 當電腦本機時鐘與預定提醒時間吻合時
                        if (currentTimeStr.equals(scheduledTime)) {
                            String userName = mainFrame.getPlayerState().getAvatar().getName();
                            
                            if (trayIcon != null) {
                                trayIcon.displayMessage(
                                    "FitQuest 重力訓練預警",
                                    userName + "！您今天計畫的鋼鐵肌群破壞時間已到！請立即就位突破基因限制！",
                                    TrayIcon.MessageType.INFO
                                );
                                System.out.println("[發射通知] 成功在作業系統核心彈出備忘提醒。");
                                lastNotifiedDate = today; // 標記今日已通知
                            }
                        }
                    }
                } catch (IOException ignored) {}
            }
        }, 0, 1000 * 30); // 改為每 30 秒雷達掃描一次，兼顧效能與精準度
    }

    /**
     * 建立並顯示首次進入遊戲的個人資料輸入對話框
     */
    private static void showFirstTimeRegistration() {
        JDialog regDialog = new JDialog((Window) null, "FitQuest 初始基因序列初始化");
        regDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        regDialog.setSize(450, 480);
        regDialog.setResizable(false);
        regDialog.setLocationRelativeTo(null);

        // 主面板排版
        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(APP_BG);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        // 歡迎與提示標題
        JLabel title = new JLabel("初始化你的虛擬火柴人");
        title.setFont(new Font("Dialog", Font.BOLD, 22));
        title.setForeground(TEXT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("請輸入真實生物特徵，以精準校正科學熱量算式。");
        subtitle.setFont(new Font("Dialog", Font.PLAIN, 12));
        subtitle.setForeground(new Color(0xb7c0d1));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        mainPanel.add(title);
        mainPanel.add(Box.createRigidArea(new Dimension(1, 4)));
        mainPanel.add(subtitle);
        mainPanel.add(Box.createRigidArea(new Dimension(1, 28)));

        // 輸入表單區域
        JPanel formPanel = new JPanel(new GridLayout(4, 2, 12, 24));
        formPanel.setOpaque(false);

        JTextField nameField = createStyledTextField("基因代號 (姓名)");
        JTextField heightField = createStyledTextField("175.0");
        JTextField weightField = createStyledTextField("70.0");
        
        JComboBox<Gender> genderBox = new JComboBox<>(Gender.values());
        genderBox.setFont(new Font("Dialog", Font.BOLD, 15));
        genderBox.setBackground(new Color(0x20242d));
        genderBox.setForeground(TEXT);

        formPanel.add(createFormLabel("使用者姓名："));   formPanel.add(nameField);
        formPanel.add(createFormLabel("身高 (cm)："));   formPanel.add(heightField);
        formPanel.add(createFormLabel("體重 (kg)："));   formPanel.add(weightField);
        formPanel.add(createFormLabel("玩家性別："));    formPanel.add(genderBox);

        mainPanel.add(formPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(1, 32)));

        // 確認送出按鈕
        JButton submitBtn = new JButton("建構人偶並踏入領域");
        submitBtn.setFont(new Font("Dialog", Font.BOLD, 16));
        submitBtn.setForeground(Color.WHITE);
        submitBtn.setBackground(CTA_BLUE);
        submitBtn.setOpaque(true);
        submitBtn.setContentAreaFilled(true);
        submitBtn.setBorderPainted(false);
        submitBtn.setFocusPainted(false);
        submitBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        submitBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        
        submitBtn.addActionListener(event -> {
            String name = nameField.getText().trim();
            if (name.isEmpty() || name.equals("基因代號 (姓名)")) {
                JOptionPane.showMessageDialog(regDialog, "請輸入有效的玩家姓名代號！", "序列錯誤", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                double height = Double.parseDouble(heightField.getText());
                double weight = Double.parseDouble(weightField.getText());
                Gender gender = (Gender) genderBox.getSelectedItem();

                // 創建並儲存第一份玩家資料
                PlayerState firstPlayer = new PlayerState(name, height, weight, gender);
                
                // 自動建立 data 資料夾（避免目錄不存在導致 IOException）
                Path dataDir = Path.of("data");
                if (!Files.exists(dataDir)) {
                    Files.createDirectories(dataDir);
                }
                
                Storage initialStorage = new Storage(dataDir);
                initialStorage.savePlayer(firstPlayer);

                // 關閉輸入框並呼叫主畫面啟動方法
                regDialog.dispose();
                launchMainFrame();

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(regDialog, "身高與體重必須輸入正確的數字（可帶小數點）！", "數據校正錯誤", JOptionPane.ERROR_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(regDialog, "無法建立存檔目錄: " + ex.getMessage(), "系統錯誤", JOptionPane.ERROR_MESSAGE);
            }
        });

        mainPanel.add(submitBtn);
        regDialog.setContentPane(mainPanel);
        regDialog.setVisible(true);
    }

    private static JLabel createFormLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.RIGHT);
        label.setFont(new Font("Dialog", Font.BOLD, 15));
        label.setForeground(TEXT);
        return label;
    }

    private static JTextField createStyledTextField(String defaultText) {
        JTextField field = new JTextField(defaultText);
        field.setFont(new Font("Dialog", Font.PLAIN, 15));
        field.setForeground(TEXT);
        field.setBackground(new Color(0x20242d));
        field.setCaretColor(TEXT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        return field;
    }
}