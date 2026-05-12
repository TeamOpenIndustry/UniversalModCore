package cam72cam.mod.gui_v2.core;

import cam72cam.mod.gui_v2.rendering.GuiRenderer;

import java.util.function.BiConsumer;

public interface ILayoutable<T> {
    //Position
    int x();
    int y();
    int width();
    int height();
    void setX(int x);
    void setY(int y);
    void setWidth(int width);
    void setHeight(int height);
    void setBound(int x, int y, int width, int height);

    //Rendering
    boolean isVisible();
    void setVisible(boolean visible);

    void renderBackground(GuiRenderer renderer);
    void render(GuiRenderer renderer);
    void renderForeground(GuiRenderer renderer);

    void setBackgroundRenderFunc(BiConsumer<GuiRenderer, T> handler);
    void setRenderFunc(BiConsumer<GuiRenderer, T> handler);
    void setForegroundRenderFunc(BiConsumer<GuiRenderer, T> handler);

    //Layout
    void layout(int x, int y);
}
