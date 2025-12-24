package cam72cam.mod.gui.screen;

public interface IWidget {
    /**
     * Change current widget's visibility
     */
    void setVisible(boolean visible);

    boolean isVisible();

    /**
     * Enable or disable current widget
     */
    void setEnabled(boolean enabled);

    boolean isEnabled();

    /**
     * Get current widget's content or display name
     */
    String getText();

    /**
     * Set current widget's content or display name
     */
    void setText(String text);

//    void onStateChange();
}
