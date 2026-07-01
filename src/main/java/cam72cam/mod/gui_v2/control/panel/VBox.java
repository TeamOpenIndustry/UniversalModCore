package cam72cam.mod.gui_v2.control.panel;

import cam72cam.mod.gui_v2.core.layout.HorizontalAlign;
import cam72cam.mod.gui_v2.core.layout.ILayoutable;
import cam72cam.mod.gui_v2.control.AbstractPanel;

public class VBox extends AbstractPanel<VBox> {
    private int spacing;
    private HorizontalAlign alignType;

    public VBox(int spacing) {
        this(spacing, HorizontalAlign.LEFT);
    }

    public VBox(int spacing, HorizontalAlign alignType) {
        //Left empty for auto width/height
        super(0, 0);
        this.spacing = spacing;
        this.alignType = alignType;
    }

    public int getSpacing() {
        return spacing;
    }

    public void setSpacing(int spacing) {
        this.spacing = spacing;
        requestLayout();
    }

    public HorizontalAlign getAlignType() {
        return alignType;
    }

    public void setAlignType(HorizontalAlign alignType) {
        this.alignType = alignType;
    }

    @Override
    public void layout(int x, int y) {
        this.setX(x);
        this.setY(y);
        int maxWidth = getVisibleChildren().stream().mapToInt(ILayoutable::width).max().orElse(0);
        int currentHeight = y;
        for (ILayoutable<?> widget : getVisibleChildren()) {
            int childWidth = widget.width();
            int childXOffset;
            switch (alignType) {
                case MIDDLE:
                    childXOffset = (maxWidth - childWidth) / 2;
                    break;
                case RIGHT:
                    childXOffset = maxWidth - childWidth;
                    break;
                case LEFT:
                default:
                    childXOffset = 0;
                    break;
            }
            widget.setX(childXOffset);
            widget.setY(currentHeight);
            widget.layout(x + childXOffset, currentHeight);
            currentHeight += widget.height() + spacing;
        }
        int totalHeight = currentHeight - y - spacing;
        setWHInternal(maxWidth, Math.max(totalHeight, 0));
    }

    @Override
    public void setBound(int x, int y, int width, int height) {
        this.setX(x);
        this.setY(y);
    }

    @Override
    public void setWidth(int width) {
        //VBox auto-calculates bounds
    }

    @Override
    public void setHeight(int height) {
        //VBox auto-calculates bounds
    }

    protected void setWHInternal(int width, int height) {
        super.setWidth(width);
        super.setHeight(height);
    }
}
