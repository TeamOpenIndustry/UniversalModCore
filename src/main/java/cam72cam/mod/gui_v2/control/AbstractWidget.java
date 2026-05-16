package cam72cam.mod.gui_v2.control;

import cam72cam.mod.gui_v2.GuiUtils;
import cam72cam.mod.gui_v2.core.actions.IFocusable;
import cam72cam.mod.gui_v2.core.layout.ILayoutable;
import cam72cam.mod.gui_v2.core.ScissorStack;
import cam72cam.mod.gui_v2.rendering.GuiRenderer;
import cam72cam.mod.text.PlayerMessage;
import cam72cam.mod.util.With;

import java.util.function.BiConsumer;

/**
 * Basic UMC widget
 */
public abstract class AbstractWidget<T extends AbstractWidget<T>>
        implements ILayoutable<T> {
    private PlayerMessage name;
    private int nameColor;

    protected AbstractWidget<?> parent;

    private boolean visible = true;
    private boolean enabled = true;

    public AbstractWidget() {}

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
    public int getNameColor() {
        return nameColor;
    }

    public void setNameColor(int argb) {
        this.nameColor = argb;
    }

    /**
     * Is mouse over?
     */
    protected boolean isHovering() {
        return isHovering(GuiUtils.getMouseX(), GuiUtils.getMouseY());
    }

    protected boolean isHovering(float mouseX, float mouseY) {
        boolean flag = true;
        if (parent != null) {
            flag = parent.isHovering(mouseX, mouseY);
        }
        return flag && mouseX >= this.x() && mouseX <= this.x() + this.width() && mouseY >= this.y() && mouseY <= this.y() + this.height();
    }

    /* ILayoutable */
    /* Don't directly make use of these fields! */
    private int x, y, width, height;

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
        this.setX(x);
        this.setY(y);
        this.setWidth(width);
        this.setHeight(height);
    }

    protected BiConsumer<GuiRenderer, T> background = (gui, widget) -> {};
    protected BiConsumer<GuiRenderer, T> content = (gui, widget) -> {};
    protected BiConsumer<GuiRenderer, T> foreground = (gui, widget) -> {};

    @Override
    public void renderBackground(GuiRenderer renderer, ScissorStack stack) {
        try (With ctx = stack.applyScissor()) {
            background.accept(renderer, (T) this);
        }
    }
    @Override
    public void render(GuiRenderer renderer, ScissorStack stack) {
        try (With ctx = stack.applyScissor()) {
            content.accept(renderer, (T) this);
        }
    }
    @Override
    public void renderForeground(GuiRenderer renderer, ScissorStack stack) {
        try (With ctx = stack.applyScissor()) {
            foreground.accept(renderer, (T) this);
        }
    }

    @Override
    public void setBackgroundRenderFunc(BiConsumer<GuiRenderer, T> handler) {
        background = handler;
    }
    @Override
    public void setRenderFunc(BiConsumer<GuiRenderer, T> handler) {
        content = handler;
    }
    @Override
    public void setForegroundRenderFunc(BiConsumer<GuiRenderer, T> handler) {
        foreground = handler;
    }

    public void requestLayout() {
        if (this.parent != null) {
            parent.requestLayout();
            return;
        }
        //Only handle in root
        layout(this.x(), this.y());
    }

    protected void requestFocus(IFocusable focusing) {
        if (this.parent != null) {
            this.parent.requestFocus(focusing);
        }
        //Implemented in AbstractPanel
    }
    protected void freeFocus() {
        if (this.parent != null) {
            this.parent.freeFocus();
        }
    }
}
