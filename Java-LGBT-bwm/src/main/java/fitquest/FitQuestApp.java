package fitquest;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class FitQuestApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // The app still works with Java's default look and feel.
            }
            new FitQuestFrame().setVisible(true);
        });
    }
}
