package cam72cam.mod.gui_v2.overlay;

import cam72cam.mod.gui_v2.rendering.GuiRenderer;

public abstract class PostEffect {
    public abstract void drawAt(GuiRenderer guiRenderer, int x, int y);
    public abstract boolean isAlive();
}
