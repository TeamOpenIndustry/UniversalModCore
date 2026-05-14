package cam72cam.mod.gui_v2.control.panel;

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
        requestLayout();
    }

    @Override
    public void layout(int x, int y) {
        this.setX(x);
        this.setY(y);
        int maxWidth = 0;
        int currentHeight = y;
        for (ILayoutable<?> widget : children) {
            widget.setX(0);
            widget.setY(currentHeight);
            widget.layout(x, currentHeight);
            currentHeight += widget.height() + spacing;
            maxWidth = Math.max(maxWidth, widget.width());
        }
        setWHInternal(maxWidth, currentHeight - y - spacing);
    }

    @Override
    public void setWidth(int width) {
        //NO-OP for VBox
    }

    @Override
    public void setHeight(int height) {
        //NO-OP for VBox
    }

    protected void setWHInternal(int width, int height) {
        super.setWidth(width);
        super.setHeight(height);
    }
}
