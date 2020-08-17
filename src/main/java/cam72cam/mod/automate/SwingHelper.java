package cam72cam.mod.automate;

import javax.swing.*;

public class SwingHelper {
    public static void alert(String s) {
        alert(null, s);
    }

    public static void alert(JComponent component, String s) {
        JOptionPane.showMessageDialog(component, s);
    }
}
