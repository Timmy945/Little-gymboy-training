package fithero.sandbox;

import fithero.ui.FitQuestFrame;
import fithero.ui.WorkoutSessionDialog;
import fithero.model.exercise.MuscleGroup;
import javax.swing.SwingUtilities;

/**
 * 獨立測試沙盒：用於單獨隔離測試自訂菜單視窗組件。
 */
public class DialogSandbox {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            System.out.println("=== [對話框介面測試沙盒啟動] ===");

            // 隔離模擬主框架參考
            FitQuestFrame mockFrame = new FitQuestFrame() {
                @Override
                public void saveAndRefresh() {
                    System.out.println("\n[UI 虛擬通知] 觸發模擬存檔成功！");
                    System.out.println("當前等級: Lv." + getPlayerState().level() + ", 當前累積 XP: " + getPlayerState().xp());
                    System.out.println("當前背肌視覺等級: Lv." + getPlayerState().muscleLevel(MuscleGroup.BACK));
                }
            };
            
            mockFrame.setVisible(false);

            System.out.println(">> 開啟運動工作台頁面... 請在 UI 上按下完訓確認。");
            WorkoutSessionDialog dialog = new WorkoutSessionDialog(mockFrame);
            dialog.setVisible(true);

            dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(java.awt.event.WindowEvent e) {
                    System.out.println("\n=== [測試結束] 對話框關閉。 ===");
                    System.exit(0);
                }
            });
        });
    }
}