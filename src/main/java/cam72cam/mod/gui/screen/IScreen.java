package cam72cam.mod.gui.screen;

public interface IScreen {
    /** Called when screen is initially constructed */
    void init(IScreenBuilder screen);

    /** Called when enter/return is pressed */
    void onEnterKey(IScreenBuilder builder);

    /** Called during close */
    void onClose();

    /** Called once per screen draw */
    void draw(IScreenBuilder builder);
}
