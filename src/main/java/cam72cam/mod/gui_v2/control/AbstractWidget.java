package cam72cam.mod.gui_v2.control;

import cam72cam.mod.gui_v2.GUIUtils;
import cam72cam.mod.gui_v2.core.ILayoutable;
import cam72cam.mod.gui_v2.rendering.GUIRenderer;
import cam72cam.mod.text.PlayerMessage;

import java.util.function.BiConsumer;

/**
 * Basic UMC widget
 */
public abstract class AbstractWidget<T extends AbstractWidget<T>>
        implements ILayoutable<T> {
    protected PlayerMessage name;
    protected int nameColor;

    protected AbstractPanel parent;

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

    /* ILayoutable */
    protected int x, y, width, height;

    @Override
    public int y() {
        return y;
    }
    @Override
    public int x() {
        return x;
    }
    @Override
    public int width() {
        return width;
    }
    @Override
    public int height() {
        return height;
    }
    @Override
    public void setX(int x) {
        this.x = x;
    }
    @Override
    public void setY(int y) {
        this.y = y;
    }
    @Override
    public void setWidth(int width) {
        this.width = width;
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

    protected BiConsumer<GUIRenderer, T> background = (gui, widget) -> {};
    protected BiConsumer<GUIRenderer, T> content = (gui, widget) -> {};
    protected BiConsumer<GUIRenderer, T> foreground = (gui, widget) -> {};

    @Override
    public void renderBackground(GUIRenderer renderer) {
        background.accept(renderer, (T) this);
    }
    @Override
    public void render(GUIRenderer renderer) {
        content.accept(renderer, (T) this);
    }
    @Override
    public void renderForeground(GUIRenderer renderer) {
        foreground.accept(renderer, (T) this);
    }

    @Override
    public void setBackgroundRenderFunc(BiConsumer<GUIRenderer, T> handler) {
        background = handler;
    }
    @Override
    public void setRenderFunc(BiConsumer<GUIRenderer, T> handler) {
        content = handler;
    }
    @Override
    public void setForegroundRenderFunc(BiConsumer<GUIRenderer, T> handler) {
        foreground = handler;
    }
}
