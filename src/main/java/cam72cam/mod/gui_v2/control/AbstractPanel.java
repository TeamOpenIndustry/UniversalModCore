package cam72cam.mod.gui_v2.control;

import cam72cam.mod.entity.Player;
import cam72cam.mod.gui_v2.core.ILayoutable;
import cam72cam.mod.gui_v2.core.ScissorStack;
import cam72cam.mod.gui_v2.core.actions.IClickable;
import cam72cam.mod.gui_v2.core.actions.IDraggable;
import cam72cam.mod.gui_v2.core.actions.IScrollable;
import cam72cam.mod.gui_v2.core.actions.IUpdatable;
import cam72cam.mod.gui_v2.rendering.GuiRenderer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class AbstractPanel<T extends AbstractPanel<T>> extends AbstractWidget<T>
        implements IClickable, IDraggable, IScrollable {
    protected List<ILayoutable<?>> children;

    public AbstractPanel(int width, int height) {
        super();
        this.setBound(0, 0, width, height);
        this.children = new ArrayList<>();
    }

    public void addChildren(ILayoutable<?> child) {
        addChildren(Collections.singleton(child));
    }

    public void addChildren(ILayoutable<?>... children) {
        addChildren(Arrays.asList(children));
    }

    public void addChildren(Iterable<ILayoutable<?>> children) {
        for (ILayoutable<?> child : children) {
            this.children.add(child);
            if (child instanceof AbstractWidget<?>) {
                ((AbstractWidget<?>)child).parent = this;
            }
        }
        layout(this.x(), this.y());
    }

    public void renderPanel(GuiRenderer renderer, ScissorStack stack) {
        stack.push(this);
        this.children.stream().filter(ILayoutable::isVisible).forEach(child -> {
            stack.push(child);
            if (child instanceof AbstractPanel) {
                ((AbstractPanel<?>) child).renderPanel(renderer, stack);
            } else {
                child.renderBackground(renderer, stack);
                child.render(renderer, stack);
                child.renderForeground(renderer, stack);
            }
            stack.pop();
            if (child instanceof IUpdatable) {
                ((IUpdatable) child).postRender();
            }
        });
        this.renderBackground(renderer, stack);
        this.render(renderer, stack);
        this.renderForeground(renderer, stack);
        this.renderBound(renderer);
        stack.pop();
        if (this instanceof IUpdatable) {
            ((IUpdatable) this).postRender();
        }
    }
    void renderBound(GuiRenderer renderer) {
        renderer.drawRect(x(), y(), 1, height(), 0x000000);
        renderer.drawRect(x(), y(), width(), 1, 0x000000);
        renderer.drawRect(x() + width(), y(), 1, height(), 0x000000);
        renderer.drawRect(x(), y() + height(), width(), 1, 0x000000);
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
        //TODO Tracking
        if (!isHovering()) {
            return false;
        }
        return children.stream()
                       .filter(c -> c instanceof IDraggable)
                       .anyMatch(c -> ((IDraggable) c).onDrag(hand, mouseX, mouseY));
    }

    @Override
    public boolean onRelease(Player.Hand hand, int mouseX, int mouseY) {
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
}
