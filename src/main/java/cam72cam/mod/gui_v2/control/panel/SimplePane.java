package cam72cam.mod.gui_v2.control.panel;

import cam72cam.mod.gui_v2.GuiUtils;
import cam72cam.mod.gui_v2.control.AbstractWidget;
import cam72cam.mod.gui_v2.control.PositionedPanel;
import cam72cam.mod.gui_v2.core.layout.ILayoutable;

/**
 * Fixed size panel
 */
public class SimplePane extends PositionedPanel<SimplePane> {
    public SimplePane(int width, int height) {
        super(width, height);
    }

    public static SimplePane fullScreen() {
        return new SimplePane(GuiUtils.getScreenWidth(), GuiUtils.getScreenHeight());
    }

    @Override
    public void layout(int x, int y) {
        this.setX(x);
        this.setY(y);
        int width = 0, height = 0;
        for (AbstractWidget<?> child : getVisibleChildren()) {
            int childX = x + getChildRelX(child);
            int childY = y + getChildRelY(child);
            child.layout(childX, childY);
            width = Math.max(child.width() + childX - x, width);
            height = Math.max(child.height() + childY - y, height);
        }
        this.setWidth(width);
        this.setHeight(height);
    }

    @Override
    public void addChildren(AbstractWidget<?> child, int relX, int relY) {
        super.addChildren(child, relX, relY);
    }

    @Override
    public void setChildPosition(AbstractWidget<?> child, int relX, int relY) {
        super.setChildPosition(child, relX, relY);
    }

    @Override
    public int getChildRelX(AbstractWidget<?> child) {
        return super.getChildRelX(child);
    }

    @Override
    public int getChildRelY(AbstractWidget<?> child) {
        return super.getChildRelY(child);
    }
}
