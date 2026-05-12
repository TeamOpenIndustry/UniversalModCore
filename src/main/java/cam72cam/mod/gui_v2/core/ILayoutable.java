package cam72cam.mod.gui_v2.core;

import cam72cam.mod.gui_v2.rendering.GUIRenderer;

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
    void renderBackground(GUIRenderer renderer);
    void render(GUIRenderer renderer);
    void renderForeground(GUIRenderer renderer);

    void setBackgroundRenderFunc(BiConsumer<GUIRenderer, T> handler);
    void setRenderFunc(BiConsumer<GUIRenderer, T> handler);
    void setForegroundRenderFunc(BiConsumer<GUIRenderer, T> handler);

    //Layout
    void layout(int x, int y);
}
