package cam72cam.mod.gui_v2.control;

import cam72cam.mod.entity.Player;
import cam72cam.mod.gui_v2.control.panel.SimplePane;
import cam72cam.mod.gui_v2.core.ScissorStack;
import cam72cam.mod.gui_v2.core.actions.*;
import cam72cam.mod.gui_v2.core.layout.ILayoutable;
import cam72cam.mod.gui_v2.rendering.GuiRenderFunc;
import cam72cam.mod.gui_v2.rendering.GuiRenderer;
import cam72cam.mod.input.Keyboard;

public abstract class ComposedWidget<T extends ComposedWidget<T>> extends AbstractWidget<T>
        implements IClickable, IDraggable, IUpdatable, IScrollable, IKeyboardListener {

    private final SimplePane internal;

    public ComposedWidget(int width, int height) {
        this.internal = new SimplePane(width, height);
        this.internal.parent = this;
        this.setWidth(width);
        this.setHeight(height);
    }

    //Redirects
    @Override
    public void setX(int x) {
        super.setX(x);
        internal.setX(x);
    }
    @Override
    public void setY(int y) {
        super.setY(y);
        internal.setY(y);
    }
    @Override
    public void setWidth(int width) {
        internal.setWidth(width);
        super.setWidth(width);
    }
    @Override
    public void setHeight(int height) {
        internal.setHeight(height);
        super.setHeight(height);
    }

    @Override
    public int width() {
        return internal.width();
    }
    @Override
    public int height() {
        return internal.height();
    }

    public int getChildRelativeX(ILayoutable<?> child) {
        return internal.getChildRelX(child);
    }
    public int getChildRelativeY(ILayoutable<?> child) {
        return internal.getChildRelY(child);
    }
    public void setChildRelativeX(ILayoutable<?> child, int relX) {
        internal.setChildPosition(child, relX, internal.getChildRelY(child));
    }
    public void setChildRelativeY(ILayoutable<?> child, int relY) {
        internal.setChildPosition(child, internal.getChildRelX(child), relY);
    }

    protected void addChildren(ILayoutable<?> widget, int relX, int relY) {
        internal.addChildren(widget, relX, relY);
    }

    @Override
    public void layout(int x, int y) {
        this.setX(x);
        this.setY(y);
        internal.setBound(0, 0, width(), height());
        internal.layout(x, y);
    }

    @Override
    public void setBackgroundRenderFunc(GuiRenderFunc<T> handler) {
        //NO-OP for ComposedWidget
    }
    @Override
    public void renderMain(GuiRenderer renderer, ScissorStack stack) {
        stack.push(this);
        internal.renderPanel(renderer, stack);
        stack.pop();
    }
    @Override
    public void setForegroundRenderFunc(GuiRenderFunc<T> handler) {
        //NO-OP for ComposedWidget
    }

    @Override
    public boolean onClick(Player.Hand hand, int x, int y) {
        return internal.onClick(hand, x, y);
    }
    @Override
    public boolean onDrag(Player.Hand hand, int mouseX, int mouseY) {
        return internal.onDrag(hand, mouseX, mouseY);
    }
    @Override
    public boolean onRelease(Player.Hand hand, int mouseX, int mouseY) {
        return internal.onRelease(hand, mouseX, mouseY);
    }
    @Override
    public boolean onScroll(int mouseX, int mouseY, double deltaScroll) {
        return internal.onScroll(mouseX, mouseY, deltaScroll);
    }
    @Override
    public void onTick() {
        internal.onTick();
    }
    @Override
    public boolean onKeyPressed(Keyboard.KeyCode key) {
        return internal.onKeyPressed(key);
    }
    @Override
    public boolean onCharTyped(char ch) {
        return internal.onCharTyped(ch);
    }
}