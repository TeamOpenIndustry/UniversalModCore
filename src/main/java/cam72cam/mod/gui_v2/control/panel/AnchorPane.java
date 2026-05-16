package cam72cam.mod.gui_v2.control.panel;

import cam72cam.mod.gui_v2.GuiUtils;
import cam72cam.mod.gui_v2.control.PositionedPanel;
import cam72cam.mod.gui_v2.core.layout.HorizontalAlign;
import cam72cam.mod.gui_v2.core.layout.ILayoutable;
import cam72cam.mod.gui_v2.core.layout.VerticalAlign;

import java.util.HashMap;
import java.util.Map;

public class AnchorPane extends PositionedPanel<AnchorPane> {
    private final Map<ILayoutable<?>, AnchorInfo> anchorMap = new HashMap<>();

    public AnchorPane(int width, int height) {
        super(width, height);
    }

    public static AnchorPane fullScreen() {
        return new AnchorPane(GuiUtils.getScreenWidth(), GuiUtils.getScreenHeight());
    }

    @Override
    public void addChildren(ILayoutable<?> child, int relX, int relY) {
        addChildren(child, HorizontalAlign.LEFT, relX, VerticalAlign.TOP, relY);
    }

    @Override
    public void addChildren(Iterable<ILayoutable<?>> children) {
        super.addChildren(children);
        for (ILayoutable<?> child : children) {
            anchorMap.put(child, new AnchorInfo(HorizontalAlign.LEFT, 0, VerticalAlign.TOP, 0));
        }
    }

    public void addChildren(ILayoutable<?> child, HorizontalAlign hAlign, int marginX, VerticalAlign vAlign, int marginY) {
        super.addChildren(child);
        anchorMap.put(child, new AnchorInfo(hAlign, marginX, vAlign, marginY));
        requestLayout();
    }

    public void setChildAnchor(ILayoutable<?> child, HorizontalAlign hAlign, int marginX, VerticalAlign vAlign, int marginY) {
        if (!anchorMap.containsKey(child))
            throw new IllegalArgumentException("AnchorPane does not contain child");
        anchorMap.put(child, new AnchorInfo(hAlign, marginX, vAlign, marginY));
        requestLayout();
    }

    @Override
    public void layout(int x, int y) {
        this.setX(x);
        this.setY(y);

        int panelW = width();
        int panelH = height();

        for (ILayoutable<?> child : getVisibleChildren()) {
            AnchorInfo info = anchorMap.get(child);
            if (info != null) {
                int relX = 0, relY = 0;

                switch (info.hAlign) {
                    case LEFT:   relX = info.marginX; break;
                    case MIDDLE: relX = (panelW - child.width()) / 2; break;
                    case RIGHT:  relX = panelW - child.width() - info.marginX; break;
                }

                switch (info.vAlign) {
                    case TOP:    relY = info.marginY; break;
                    case MIDDLE: relY = (panelH - child.height()) / 2; break;
                    case BOTTOM: relY = panelH - child.height() - info.marginY; break;
                }

                setChildPosition(child, relX, relY);
            }

            int childX = x + getChildRelX(child);
            int childY = y + getChildRelY(child);
            child.setX(childX);
            child.setY(childY);
            child.layout(childX, childY);
        }
    }

    private static class AnchorInfo {
        final HorizontalAlign hAlign;
        final int marginX;

        final VerticalAlign vAlign;
        final int marginY;

        AnchorInfo(HorizontalAlign hAlign, int marginX, VerticalAlign vAlign, int marginY) {
            this.hAlign = hAlign;
            this.marginX = marginX;
            this.vAlign = vAlign;
            this.marginY = marginY;
        }
    }
}