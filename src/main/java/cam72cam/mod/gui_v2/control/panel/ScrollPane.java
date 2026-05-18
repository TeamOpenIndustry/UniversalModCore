package cam72cam.mod.gui_v2.control.panel;

import cam72cam.mod.gui_v2.control.AbstractPanel;
import cam72cam.mod.gui_v2.control.widget.Slider;
import cam72cam.mod.gui_v2.core.layout.ILayoutable;
import cam72cam.mod.gui_v2.core.actions.IScrollable;
import cam72cam.mod.text.PlayerMessage;

public abstract class ScrollPane extends AbstractPanel<ScrollPane>
        implements IScrollable {
    protected Slider controller;
    protected double scrolled;
    protected int contentLength;

    protected ScrollPane(int width, int height) {
        super(width, height);
    }

    public static ScrollPane vertical(int width, int height) {
        return new Vertical(width, height);
    }

    public static ScrollPane horizontal(int width, int height) {
        return new Horizontal(width, height);
    }

    public void showScrollBar() {
        controller.setVisible(true);
        addController(controller);
        requestLayout();
    }

    public void hideScrollBar() {
        controller.setVisible(false);
        removeController(controller);
        requestLayout();
    }

    public boolean isBarVisible() {
        return controller.isVisible();
    }

    protected abstract int viewportLength();

    @Override
    public boolean onScroll(int mouseX, int mouseY, double deltaScroll) {
        if (!isHovering()) {
            return false;
        }
        boolean canChildrenHandle = isHoveringPanel()
                && castedStream(getVisibleChildrenReverse(), IScrollable.class)
                   .anyMatch(c -> c.onScroll(mouseX, mouseY, deltaScroll));
        if (canChildrenHandle) {
            return true;
        }
        double maxScroll = Math.max(0, contentLength - viewportLength());
        if (maxScroll > 0) {
            double step = 20.0 / maxScroll;
            double prevScrolled = scrolled;
            if (deltaScroll > 0) {
                scrolled = Math.max(0, scrolled - step);
            } else if (deltaScroll < 0) {
                scrolled = Math.min(1.0, scrolled + step);
            }
            if (prevScrolled != scrolled) {
                controller.setValue(scrolled);
                return true;
            }
        }
        return false;
    }

    private void onControllerChange(Slider ctrl) {
        scrolled = ctrl.getValue();
        requestLayout();
    }

    static class Vertical extends ScrollPane {
        private Vertical(int width, int height) {
            super(width, height);
            this.controller = Slider.vertical(10, height, PlayerMessage.direct(""),
                                              0, 1, 0, 0, super::onControllerChange);
            addController(controller);
        }

        @Override
        protected int viewportLength() {
            return height();
        }

        @Override
        public int panelWidth() {
            int w = controller.isVisible() ? controller.width() : 0;
            return width() - w;
        }

        @Override
        public void layout(int x, int y) {
            setX(x);
            setY(y);
            contentLength = getVisibleChildren().stream().mapToInt(ILayoutable::height).sum();
            double maxScroll = contentLength - height();
            if (maxScroll <= 0) {
                maxScroll = 0;
                scrolled = 0;
                controller.setHandleSize(controller.height());
            } else {
                int handleSize = controller.height();
                handleSize *= height();
                handleSize /= contentLength;
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
            controller.layout(x() + width() - controller.width(), y());
        }
    }

    static class Horizontal extends ScrollPane {
        private Horizontal(int width, int height) {
            super(width, height);
            this.controller = Slider.horizontal(width, 10, PlayerMessage.direct(""),
                                                0, 1, 0, 0, super::onControllerChange);
            addController(controller);
        }

        @Override
        protected int viewportLength() {
            return width();
        }

        @Override
        public int panelHeight() {
            int h = controller.isVisible() ? controller.height() : 0;
            return height() - h;
        }

        @Override
        public void layout(int x, int y) {
            setX(x);
            setY(y);
            contentLength = getVisibleChildren().stream().mapToInt(ILayoutable::width).sum();
            double maxScroll = contentLength - width();
            if (maxScroll <= 0) {
                maxScroll = 0;
                scrolled = 0;
                controller.setHandleSize(controller.width());
            } else {
                int handleSize = controller.width();
                handleSize *= width();
                handleSize /= contentLength;
                controller.setHandleSize(handleSize);
            }
            double scrollOffset = scrolled * maxScroll;
            int currentX = x() - (int) scrollOffset;
            for (ILayoutable<?> widget : getVisibleChildren()) {
                widget.setX(currentX);
                widget.setY(y());
                widget.layout(currentX, y());
                currentX += widget.width();
            }
            controller.layout(x(), y() + height() - controller.height());
        }
    }
}