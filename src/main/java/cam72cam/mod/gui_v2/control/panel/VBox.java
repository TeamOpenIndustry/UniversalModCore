package cam72cam.mod.gui_v2.control.panel;

import cam72cam.mod.gui_v2.core.ILayoutable;
import cam72cam.mod.gui_v2.control.AbstractPanel;

public class VBox extends AbstractPanel<VBox> {
    public VBox(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    @Override
    public void layout(int x, int y) {
        this.setX(x);
        this.setY(y);
        for (ILayoutable<?> widget : children) {
            widget.setX(0);
            widget.setY(y);
            widget.layout(x, y);
            y += widget.height();
        }
    }

    @Override
    public int height() {
        return children.stream().mapToInt(ILayoutable::height).sum();
    }
}
