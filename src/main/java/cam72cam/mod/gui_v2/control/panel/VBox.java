package cam72cam.mod.gui_v2.control.panel;

import cam72cam.mod.gui_v2.GuiUtils;
import cam72cam.mod.gui_v2.core.ILayoutable;
import cam72cam.mod.gui_v2.control.AbstractPanel;

public class VBox extends AbstractPanel<VBox> {
    private int spacing;

    public VBox(int spacing) {
        //Left empty for auto width/height
        super(0, 0);
        this.spacing = spacing;
    }

    public int getSpacing() {
        return spacing;
    }

    public void setSpacing(int spacing) {
        this.spacing = spacing;
        GuiUtils.requestLayout();
    }

    @Override
    public void layout(int x, int y) {
        this.setX(x);
        this.setY(y);
        for (ILayoutable<?> widget : children) {
            widget.setX(0);
            widget.setY(y);
            widget.layout(x, y);
            y += widget.height() + spacing;
        }
    }

    @Override
    public int width() {
        return children.stream().mapToInt(ILayoutable::width).max().orElse(0);
    }

    @Override
    public int height() {
        int total = 0;
        for (ILayoutable<?> child : children) {
            total += child.height() + spacing;
        }
        total -= spacing;
        return total;
    }

    @Override
    public void setWidth(int width) {
        //NO-OP
    }

    @Override
    public void setHeight(int height) {
        //NO-OP
    }
}
