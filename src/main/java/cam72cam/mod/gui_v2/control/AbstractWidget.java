package cam72cam.mod.gui_v2.control;

import cam72cam.mod.gui_v2.GUIUtils;
import cam72cam.mod.gui_v2.core.ILayoutable;
import cam72cam.mod.text.PlayerMessage;

/**
 * Basic UMC widget
 */
public abstract class AbstractWidget implements ILayoutable {
    protected PlayerMessage name;
    protected int nameColor;

    protected AbstractPanel parent;

    protected int x, y, width, height;
    protected boolean visible;
    protected boolean enabled;

    /**
     * Change current widget's visibility
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return visible;
    }

    /**
     * Enable or disable current widget
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Get/set current widget's display name
     */
    public PlayerMessage getName() {
        return name;
    }

    public void setName(PlayerMessage text) {
        this.name = text;
    }

    /** Override the text color */
    public void setNameColor(int argb) {
        this.nameColor = argb;
    }

    /**
     * Is mouse over?
     */
    public boolean isHovering() {
        return isHovering(GUIUtils.getMouseX(), GUIUtils.getMouseY());
    }

    private boolean isHovering(float mouseX, float mouseY) {
        return mouseX >= this.x && mouseX  <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + this.height;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public void setX(int x) {
        this.x = x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public void setY(int y) {
        this.y = y;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public void setWidth(int width) {
        this.width = width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public void setBound(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
}
