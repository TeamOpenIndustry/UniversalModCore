package cam72cam.mod.gui_v2.core;

import cam72cam.mod.gui_v2.rendering.GUIRenderer;

public interface ILayoutable {
    //Position
    int getX();
    int getY();
    int getWidth();
    int getHeight();
    void setX(int x);
    void setY(int y);
    void setWidth(int width);
    void setHeight(int height);
    void setBound(int x, int y, int width, int height);

    //Rendering
    void renderBackground(GUIRenderer renderer);
    void render(GUIRenderer renderer);
    void renderForeground(GUIRenderer renderer);

    //Layout
    void layout(int x, int y);
}
