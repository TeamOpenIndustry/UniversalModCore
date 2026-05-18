package cam72cam.mod.gui_v2.control;

import cam72cam.mod.entity.Player;
import cam72cam.mod.gui_v2.GuiUtils;
import cam72cam.mod.gui_v2.core.actions.*;
import cam72cam.mod.gui_v2.core.layout.ILayoutable;
import cam72cam.mod.gui_v2.core.ScissorStack;
import cam72cam.mod.gui_v2.rendering.GuiRenderer;
import cam72cam.mod.input.Keyboard;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractPanel<T extends AbstractPanel<T>> extends AbstractWidget<T>
        implements IClickable, IDraggable, IUpdatable, IScrollable, IKeyboardListener {
    private final List<ILayoutable<?>> children;
    private final List<ILayoutable<?>> controller;

    private IFocusable active;

    public AbstractPanel(int width, int height) {
        super();
        this.setBound(0, 0, width, height);
        this.children = new ArrayList<>();
        this.controller = new ArrayList<>();

//        this.setForegroundRenderFunc((gui, panel) -> panel.renderBound(gui, 0xFFFFFFFF));
    }

    public void addChildren(ILayoutable<?> child) {
        addChildren(Collections.singleton(child));
    }

    public void addChildren(ILayoutable<?>... children) {
        addChildren(Arrays.asList(children));
    }

    public void addChildren(Iterable<ILayoutable<?>> children) {
        for (ILayoutable<?> child : children) {
            if (child == this) {
                throw new IllegalArgumentException("Cannot add self as child panel!");
            }
            this.children.add(child);
            if (child instanceof AbstractWidget<?>) {
                ((AbstractWidget<?>) child).parent = this;
            }
        }
        layout(this.x(), this.y());
    }

    public List<ILayoutable<?>> getChildren() {
        return children;
    }

    public List<ILayoutable<?>> getVisibleChildren() {
        return children.stream().filter(ILayoutable::isVisible).collect(Collectors.toList());
    }

    public void clearChildren() {
        this.children.clear();
    }

    protected void addController(ILayoutable<?> ctrl) {
        this.controller.add(ctrl);
        if (ctrl instanceof AbstractWidget<?>) {
            ((AbstractWidget<?>) ctrl).parent = this;
        }
    }

    public void renderPanel(GuiRenderer renderer, ScissorStack stack) {
        stack.pushPanel(this);
        this.getVisibleChildren().forEach(child -> {
            stack.push(child);
            drawWidget(child, renderer, stack);
            stack.pop();
        });
        stack.pop();
        stack.push(this);
        this.controller.forEach(ctrl -> {
            stack.push(ctrl);
            drawWidget(ctrl, renderer, stack);
            stack.pop();
        });
        drawWidget(this, renderer, stack);
        stack.pop();
    }

    private void drawWidget(ILayoutable<?> widget, GuiRenderer renderer, ScissorStack stack) {
        if (widget instanceof IUpdatable) {
            ((IUpdatable) widget).preRender();
        }
        if (widget != this && widget instanceof AbstractPanel) {
            ((AbstractPanel<?>) widget).renderPanel(renderer, stack);
        } else {
            widget.renderBackground(renderer, stack);
            widget.renderMain(renderer, stack);
            widget.renderForeground(renderer, stack);
        }
        if (widget instanceof IUpdatable) {
            ((IUpdatable) widget).postRender();
        }
    }

    public void renderBound(GuiRenderer renderer, int argb) {
        renderer.drawRect(x(), y(), 1, height(), argb);
        renderer.drawRect(x(), y(), width()-1, 1, argb);
        renderer.drawRect(x() + width()-1, y(), 1, height(), argb);
        renderer.drawRect(x(), y() + height()-1, width(), 1, argb);
    }

    /* Indicate actual range excluded panel basics like ScrollPane's scroll bar */
    public int panelX() {
        return x();
    }
    public int panelY() {
        return y();
    }
    public int panelWidth() {
        return width();
    }
    public int panelHeight() {
        return height();
    }

    protected boolean isHoveringPanel() {
        return isHoveringPanel(GuiUtils.getMouseX(), GuiUtils.getMouseY());
    }
    protected boolean isHoveringPanel(int mouseX, int mouseY) {
        boolean flag = true;
        if (parent != null) {
            flag = parent.isHovering(mouseX, mouseY);
        }
        return flag && mouseX >= this.panelX() && mouseX <= this.panelX() + this.width()
                    && mouseY >= this.panelY() && mouseY <= this.panelY() + this.height();
    }

    @Override
    public boolean onClick(Player.Hand hand, int x, int y) {
        if (!isHovering()) {
            return false;
        }
        if (castedStream(controller, IClickable.class).anyMatch(c -> c.onClick(hand, x, y))) {
            return true;
        }
        return isHoveringPanel() && castedStream(getVisibleChildren(), IClickable.class)
                .anyMatch(c -> c.onClick(hand, x, y));
    }

    @Override
    public boolean onDrag(Player.Hand hand, int mouseX, int mouseY) {
        if (active instanceof IDraggable) {
            return ((IDraggable) active).onDrag(hand, mouseX, mouseY);
        }
        if (!isHovering()) {
            return false;
        }
        if (castedStream(controller, IDraggable.class).anyMatch(c -> c.onDrag(hand, mouseX, mouseY))) {
            return true;
        }
        return isHoveringPanel() && castedStream(getVisibleChildren(), IDraggable.class)
                .anyMatch(c -> c.onDrag(hand, mouseX, mouseY));
    }

    @Override
    public boolean onRelease(Player.Hand hand, int mouseX, int mouseY) {
        if (active instanceof IDraggable) {
            return ((IDraggable) active).onRelease(hand, mouseX, mouseY);
        }
        if (!isHovering()) {
            return false;
        }
        if (castedStream(controller, IDraggable.class).anyMatch(c -> c.onRelease(hand, mouseX, mouseY))) {
            return true;
        }
        return isHoveringPanel() && castedStream(getVisibleChildren(), IDraggable.class)
                .anyMatch(c -> c.onRelease(hand, mouseX, mouseY));
    }

    @Override
    public boolean onScroll(int mouseX, int mouseY, double deltaScroll) {
        if (!isHovering()) {
            return false;
        }
        if (castedStream(controller, IScrollable.class).anyMatch(c -> c.onScroll(mouseX, mouseY, deltaScroll))) {
            return true;
        }
        return isHoveringPanel() && castedStream(getVisibleChildren(), IScrollable.class)
                .anyMatch(c -> c.onScroll(mouseX, mouseY, deltaScroll));
    }

    @Override
    public void onTick() {
        castedStream(controller, IUpdatable.class).forEach(IUpdatable::onTick);
        castedStream(getVisibleChildren(), IUpdatable.class).forEach(IUpdatable::onTick);
    }

    @Override
    public boolean onKeyPressed(Keyboard.KeyCode key) {
        if (active instanceof IKeyboardListener) {
            return ((IKeyboardListener) active).onKeyPressed(key);
        }
        if (castedStream(controller, IKeyboardListener.class).anyMatch(c -> c.onKeyPressed(key))) {
            return true;
        }
        return castedStream(getVisibleChildren(), IKeyboardListener.class)
                .anyMatch(c -> c.onKeyPressed(key));
    }

    @Override
    public boolean onCharTyped(char ch) {
        if (active instanceof IKeyboardListener) {
            if (((IKeyboardListener) active).onCharTyped(ch)) return true;
        }
        if (castedStream(controller, IKeyboardListener.class).anyMatch(c -> c.onCharTyped(ch))) {
            return true;
        }
        return castedStream(getVisibleChildren(), IKeyboardListener.class)
                .anyMatch(c -> c.onCharTyped(ch));
    }

    @Override
    protected void requestFocus(IFocusable focusing) {
        if (this.parent != null) {
            super.requestFocus(focusing);
            return;
        }
        if (this.active != null) {
            this.active.onFocusLost();
        }
        focusing.onFocusGained();
        this.active = focusing;
    }
    @Override
    public void freeFocus() {
        if (this.parent != null) {
            super.freeFocus();
            return;
        }
        if (this.active != null) {
            this.active.onFocusLost();
        }
        this.active = null;
    }

    private static <E, I> Stream<I> castedStream(Collection<E> elements, Class<I> interface1) {
        return elements.stream().filter(interface1::isInstance).map(interface1::cast);
    }
}
