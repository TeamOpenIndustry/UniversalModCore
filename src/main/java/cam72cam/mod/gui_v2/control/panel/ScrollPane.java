package cam72cam.mod.gui_v2.control.panel;

import cam72cam.mod.gui_v2.control.AbstractPanel;
import cam72cam.mod.gui_v2.control.widget.Slider;
import cam72cam.mod.gui_v2.core.layout.ILayoutable;
import cam72cam.mod.gui_v2.core.actions.IScrollable;
import cam72cam.mod.text.PlayerMessage;

//TODO Controller position and visibility
public class ScrollPane extends AbstractPanel<ScrollPane> implements IScrollable {
    private final Slider controller;
    // 0 stands for top
    private double scrolled;
    private int contentHeight;

    public ScrollPane(int width, int height) {
        super(width, height);
        this.controller = Slider.vertical(10, height, PlayerMessage.direct(""),
                                          0, 1, 0, 0, this::onControllerChange);
        addController(controller);
    }

    public static ScrollPane vertical(int width, int height) {
        return new ScrollPane(width, height);
    }

    @Override
    public void layout(int x, int y) {
        this.setX(x);
        this.setY(y);

        contentHeight = getVisibleChildren().stream().mapToInt(ILayoutable::height).sum();

        double maxScroll = contentHeight - height();
        if (maxScroll <= 0) {
            maxScroll = 0;
            scrolled = 0;
            controller.setHandleSize(controller.height());
        } else {
            int handleSize = controller.height();
            handleSize *= height();
            handleSize /= contentHeight;
            controller.setHandleSize(handleSize);
        }

        double scrollOffset = scrolled * maxScroll;

        int currentY = y() - (int) scrollOffset;
        for (ILayoutable<?> widget : getVisibleChildren()) {
            widget.setX(x());
            widget.setY(currentY);
            widget.layout(x(), currentY);
            currentY += widget.height();
        }

        this.controller.layout(x() + width() - controller.width(), y());
    }

    @Override
    public int panelWidth() {
        return super.width() - controller.width();
    }

    @Override
    public boolean onScroll(int mouseX, int mouseY, double deltaScroll) {
        if (!isHovering()) return false;

        double maxScroll = Math.max(0, contentHeight - height());
        if (maxScroll <= 0) return false;

        //Fixed at 20px
        double step = 20.0 / maxScroll;
        if (deltaScroll > 0) {
            scrolled = Math.max(0, scrolled - step);
        } else if (deltaScroll < 0) {
            scrolled = Math.min(1.0, scrolled + step);
        }

        this.controller.setValue(scrolled);
        return true;
    }

    private void onControllerChange(Slider ctrl) {
        scrolled = ctrl.getValue();
        requestLayout();
    }
}