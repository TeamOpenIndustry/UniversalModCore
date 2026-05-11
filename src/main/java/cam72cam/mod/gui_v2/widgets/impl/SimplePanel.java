package cam72cam.mod.gui_v2.widgets.impl;

import cam72cam.mod.gui_v2.core.ILayoutable;
import cam72cam.mod.gui_v2.rendering.GUIRenderer;
import cam72cam.mod.gui_v2.widgets.AbstractPanel;
import org.jline.reader.Widget;

public class SimplePanel extends AbstractPanel {
    public SimplePanel(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    @Override
    public void layout(int x, int y) {
        this.setX(x);
        this.setY(y);
        for (ILayoutable widget : children) {
            widget.setX(0);
            widget.setY(y);
            widget.layout(x, y);
            y += widget.getHeight();
        }
    }
}
