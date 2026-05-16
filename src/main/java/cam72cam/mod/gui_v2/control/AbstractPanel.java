package cam72cam.mod.gui_v2.control;

import cam72cam.mod.entity.Player;
import cam72cam.mod.gui_v2.core.actions.*;
import cam72cam.mod.gui_v2.core.layout.ILayoutable;
import cam72cam.mod.gui_v2.core.ScissorStack;
import cam72cam.mod.gui_v2.rendering.GuiRenderer;
import cam72cam.mod.input.Keyboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractPanel<T extends AbstractPanel<T>> extends AbstractWidget<T>
        implements IClickable, IDraggable, IUpdatable, IScrollable, IKeyboardListener {
    private final List<ILayoutable<?>> children;

    private IFocusable active;

    public AbstractPanel(int width, int height) {
        super();
        this.setBound(0, 0, width, height);
        this.children = new ArrayList<>();

        this.setForegroundRenderFunc((gui, panel) -> panel.renderBound(gui, 0xFFFFFFFF));
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
                ((AbstractWidget<?>)child).parent = this;
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

    public void renderPanel(GuiRenderer renderer, ScissorStack stack) {
        stack.push(this);
        this.children.stream().filter(ILayoutable::isVisible).forEach(child -> {
            stack.push(child);
            drawWidget(child, renderer, stack);
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
            widget.render(renderer, stack);
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

    @Override
    public boolean onClick(Player.Hand hand, int x, int y) {
        if (!isHovering()) {
            return false;
        }
        return children.stream()
                       .filter(c -> c instanceof IClickable)
                       .anyMatch(c -> ((IClickable) c).onClick(hand, x, y));
    }

    @Override
    public boolean onDrag(Player.Hand hand, int mouseX, int mouseY) {
        if (active instanceof IDraggable) {
            return ((IDraggable) active).onDrag(hand, mouseX, mouseY);
        }
        //Defaults
        if (!isHovering()) {
            return false;
        }
        return children.stream()
                       .filter(c -> c instanceof IDraggable)
                       .anyMatch(c -> ((IDraggable) c).onDrag(hand, mouseX, mouseY));
    }

    @Override
    public boolean onRelease(Player.Hand hand, int mouseX, int mouseY) {
        if (active instanceof IDraggable) {
            return ((IDraggable) active).onRelease(hand, mouseX, mouseY);
        }
        //Defaults
        if (!isHovering()) {
            return false;
        }
        return children.stream()
                       .filter(c -> c instanceof IDraggable)
                       .anyMatch(c -> ((IDraggable) c).onRelease(hand, mouseX, mouseY));
    }

    @Override
    public boolean onScroll(int mouseX, int mouseY, double deltaScroll) {
        if (!isHovering()) {
            return false;
        }

        return children.stream()
                       .filter(c -> c instanceof IScrollable)
                       .anyMatch(c -> ((IScrollable) c).onScroll(mouseX, mouseY, deltaScroll));
    }

    @Override
    public void onTick() {
        for (ILayoutable<?> child : children) {
            if (child instanceof IUpdatable) {
                ((IUpdatable) child).onTick();
            }
        }
    }

    @Override
    public boolean onKeyPressed(Keyboard.KeyCode key) {
        if (active instanceof IKeyboardListener) {
            return ((IKeyboardListener) active).onKeyPressed(key);
        }

        return children.stream()
                       .filter(c -> c instanceof IKeyboardListener)
                       .anyMatch(c -> ((IKeyboardListener) c).onKeyPressed(key));
    }

    @Override
    public boolean onCharTyped(char ch) {
        if (active instanceof IKeyboardListener) {
            return ((IKeyboardListener) active).onCharTyped(ch);
        }

        return children.stream()
                       .filter(c -> c instanceof IKeyboardListener)
                       .anyMatch(c -> ((IKeyboardListener) c).onCharTyped(ch));
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
    protected void freeFocus() {
        if (this.parent != null) {
            super.freeFocus();
            return;
        }
        if (this.active != null) {
            this.active.onFocusLost();
        }
        this.active = null;
    }
}
