package cam72cam.mod.gui_v2.core.actions;

public interface IFocusable {
    boolean isFocusing();
    void onFocusGained();
    void onFocusLost();
}
