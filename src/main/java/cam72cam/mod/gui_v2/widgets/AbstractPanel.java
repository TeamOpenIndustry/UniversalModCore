package cam72cam.mod.gui_v2.widgets;

import cam72cam.mod.entity.Player;
import cam72cam.mod.gui_v2.core.ILayoutable;
import cam72cam.mod.gui_v2.core.actions.IClickable;
import cam72cam.mod.gui_v2.rendering.GUIRenderer;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractPanel
        extends AbstractWidget
        implements IClickable {
    protected List<ILayoutable> children;

    public AbstractPanel(int x, int y, int width, int height) {
        this.setX(x);
        this.setY(y);
        this.setWidth(width);
        this.setHeight(height);
        this.children = new ArrayList<>();
    }

    public void addChildren(ILayoutable child) {
        this.children.add(child);
        layout(this.getX(), this.getY());
    }

    @Override
    public void render(GUIRenderer renderer) {
        this.children.forEach(child -> child.render(renderer));
    }

    @Override
    public void renderBackground(GUIRenderer renderer) {

    }

    @Override
    public void renderForeground(GUIRenderer renderer) {

    }

    @Override
    public boolean consumeClick(Player.Hand hand, float x, float y) {
        return children.stream()
                       .filter(c -> c instanceof IClickable)
                       .anyMatch(c -> ((IClickable) c).consumeClick(hand, x, y));
    }
}
