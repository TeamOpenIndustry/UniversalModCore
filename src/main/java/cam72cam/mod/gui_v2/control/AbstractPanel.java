package cam72cam.mod.gui_v2.control;

import cam72cam.mod.entity.Player;
import cam72cam.mod.gui_v2.core.ILayoutable;
import cam72cam.mod.gui_v2.core.actions.IClickable;
import cam72cam.mod.gui_v2.core.actions.IDraggable;
import cam72cam.mod.gui_v2.core.actions.IScrollable;
import cam72cam.mod.gui_v2.core.actions.IUpdatable;
import cam72cam.mod.gui_v2.rendering.GuiRenderer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractPanel<T extends AbstractPanel<T>> extends AbstractWidget<T>
        implements IClickable, IDraggable, IScrollable {
    protected List<ILayoutable<?>> children;

    public AbstractPanel(int x, int y, int width, int height) {
        this.setX(x);
        this.setY(y);
        this.setWidth(width);
        this.setHeight(height);
        this.children = new ArrayList<>();
    }

    public void addChildren(ILayoutable<?> child) {
        this.children.add(child);
        layout(this.x(), this.y());
    }

    public void addChildren(ILayoutable<?>... children) {
        this.children.addAll(Arrays.asList(children));
        layout(this.x(), this.y());
    }

    public void addChildren(Iterable<ILayoutable<?>> children) {
        for (ILayoutable<?> child : children) {
            this.children.add(child);
        }
        layout(this.x(), this.y());
    }

    public void renderPanel(GuiRenderer renderer) {
        this.children.stream().filter(ILayoutable::isVisible).forEach(child -> {
            child.renderBackground(renderer);
            child.render(renderer);
            child.renderForeground(renderer);
            if (child instanceof IUpdatable) {
                ((IUpdatable) child).postRender();
            }
        });
        this.renderBackground(renderer);
        this.render(renderer);
        this.renderForeground(renderer);
        if (this instanceof IUpdatable) {
            ((IUpdatable) this).postRender();
        }
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
