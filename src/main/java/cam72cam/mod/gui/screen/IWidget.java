package cam72cam.mod.gui.screen;

public interface IWidget {
    /**
     * Change current widget's visibility
     */
    void setVisible(boolean visible);

    /**
     * Enable or disable current widget
     */
    void setEnabled(boolean enabled);

    /**
     * Get current widget's content or display name
     */
    String getText();

    /**
     * Set current widget's content or display name
     */
    void setText(String text);
}
