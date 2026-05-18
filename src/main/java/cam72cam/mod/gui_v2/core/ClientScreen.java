package cam72cam.mod.gui_v2.core;

import cam72cam.mod.gui_v2.control.panel.AnchorPane;
import cam72cam.mod.gui_v2.overlay.PostEffect;

public abstract class ClientScreen {
    private ScreenWrapper internal;

    public abstract void init(AnchorPane root);

    public void onClose() {}

    public void onGuiResize(int newWidth, int newHeight) {}

    public final void requestClose() {
        GuiManager.closeUMCScreen();
    }

    public final void addEffect(PostEffect layer) {
        internal.effects.add(layer);
    }

    void bootstrap(ScreenWrapper internal) {
        this.internal = internal;
        init(this.internal.root);
    }
}
