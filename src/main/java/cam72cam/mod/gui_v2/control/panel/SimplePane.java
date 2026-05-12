package cam72cam.mod.gui_v2.control.panel;

import cam72cam.mod.gui_v2.control.AbstractPanel;
import cam72cam.mod.gui_v2.core.ILayoutable;

/**
 * Fixed size panel
 */
public class SimplePane extends AbstractPanel<SimplePane> {
    public SimplePane(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    @Override
    public void layout(int x, int y) {
        this.setX(x);
        this.setY(y);
        int width = 0, height = 0;
        for (ILayoutable<?> child : children) {
            child.layout(x, y);
            width = Math.max(child.width() + child.x() - this.x(), width);
            height = Math.max(child.height() + child.y() - this.y(), height);
        }
        this.setWidth(width);
        this.setHeight(height);
    }
}
