package cam72cam.mod.gui_v2.control.panel;

import cam72cam.mod.gui_v2.core.layout.ILayoutable;
import cam72cam.mod.gui_v2.core.layout.VerticalAlign;
import cam72cam.mod.gui_v2.control.AbstractPanel;

public class HBox extends AbstractPanel<HBox> {
    private int spacing;
    private VerticalAlign alignType;

    public HBox(int spacing) {
        this(spacing, VerticalAlign.TOP);
    }

    public HBox(int spacing, VerticalAlign alignType) {
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

    public VerticalAlign getAlignType() {
        return alignType;
    }

    public void setAlignType(VerticalAlign alignType) {
        this.alignType = alignType;
    }

    @Override
    public void layout(int x, int y) {
        this.setX(x);
        this.setY(y);

        int maxHeight = getVisibleChildren().stream()
                                            .mapToInt(ILayoutable::height)
                                            .max().orElse(0);

        int currentX = x;
        int totalWidth = 0;

        for (ILayoutable<?> widget : getVisibleChildren()) {
            int childWidth = widget.width();
            int childHeight = widget.height();

            int childYOffset;
            switch (alignType) {
                case MIDDLE:
                    childYOffset = (maxHeight - childHeight) / 2;
                    break;
                case BOTTOM:
                    childYOffset = maxHeight - childHeight;
                    break;
                case TOP:
                default:
                    childYOffset = 0;
                    break;
            }

            widget.setX(currentX);
            widget.setY(childYOffset);
            widget.layout(currentX, y + childYOffset);

            currentX += childWidth + spacing;
            totalWidth += childWidth;
        }

        if (getVisibleChildren().size() > 1) {
            totalWidth += spacing * (getVisibleChildren().size() - 1);
        }
        setWHInternal(totalWidth, maxHeight);
    }

    @Override
    public void setBound(int x, int y, int width, int height) {
        this.setX(x);
        this.setY(y);
    }

    @Override
    public void setWidth(int width) {
        //HBox auto-calculates bounds
    }

    @Override
    public void setHeight(int height) {
        //HBox auto-calculates bounds
    }

    protected void setWHInternal(int width, int height) {
        super.setWidth(width);
        super.setHeight(height);
    }
}