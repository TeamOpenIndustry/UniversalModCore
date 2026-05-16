package cam72cam.mod.gui_v2.core.layout;

import cam72cam.mod.gui_v2.core.ScissorStack;
import cam72cam.mod.gui_v2.rendering.GuiRenderFunc;
import cam72cam.mod.gui_v2.rendering.GuiRenderer;

public interface ILayoutable<T> {
    //Position & Size
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

    void renderBackground(GuiRenderer renderer, ScissorStack stack);
    void render(GuiRenderer renderer, ScissorStack stack);
    void renderForeground(GuiRenderer renderer, ScissorStack stack);

    void setBackgroundRenderFunc(GuiRenderFunc<T> handler);
    void setRenderFunc(GuiRenderFunc<T> handler);
    void setForegroundRenderFunc(GuiRenderFunc<T> handler);

    //Layout
    void layout(int x, int y);
}
