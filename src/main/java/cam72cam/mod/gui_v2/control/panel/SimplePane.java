package cam72cam.mod.gui_v2.control.panel;

import cam72cam.mod.gui_v2.control.PositionedPanel;
import cam72cam.mod.gui_v2.core.layout.ILayoutable;

/**
 * Fixed size panel
 */
public class SimplePane extends PositionedPanel<SimplePane> {
    public SimplePane(int width, int height) {
        super(width, height);
    }

    @Override
    public void layout(int x, int y) {
        this.setX(x);
        this.setY(y);
        int width = 0, height = 0;
        for (ILayoutable<?> child : children) {
            int childX = x + getChildRelX(child);
            int childY = y + getChildRelY(child);
            child.layout(childX, childY);
            width = Math.max(child.width() + childX, width);
            height = Math.max(child.height() + childY, height);
        }
        this.setWidth(width);
        this.setHeight(height);
    }
}
