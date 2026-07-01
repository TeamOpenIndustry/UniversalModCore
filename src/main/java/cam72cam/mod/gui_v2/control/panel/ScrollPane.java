package cam72cam.mod.gui_v2.control.panel;

import cam72cam.mod.gui_v2.control.AbstractPanel;
import cam72cam.mod.gui_v2.control.widget.Slider;
import cam72cam.mod.gui_v2.core.layout.ILayoutable;
import cam72cam.mod.gui_v2.core.actions.IScrollable;
import cam72cam.mod.text.PlayerMessage;

public abstract class ScrollPane extends AbstractPanel<ScrollPane>
        implements IScrollable {

    protected Slider controller;
    protected double scrolled;           // 0 = 顶部/左侧, 1 = 底部/右侧
    protected int contentLength;         // 所有子控件的总长度（垂直为高度，水平为宽度）

    protected ScrollPane(int width, int height) {
        super(width, height);
    }

    // ================= 公共工厂 =================
    public static ScrollPane vertical(int width, int height) {
        return new Vertical(width, height);
    }

    public static ScrollPane horizontal(int width, int height) {
        return new Horizontal(width, height);
    }

    // ================= 滚动条可见性控制 =================
    public void showScrollBar() {
        setBarVisible(true);
    }

    public void hideScrollBar() {
        setBarVisible(false);
    }

    public void setBarVisible(boolean visible) {
        if (controller.isVisible() == visible) return;
        controller.setVisible(visible);
        if (visible) {
            addController(controller);
        } else {
            removeController(controller);
        }
        requestLayout();
    }

    public boolean isBarVisible() {
        return controller.isVisible();
    }

    // ================= 抽象：视口可用长度 =================
    protected abstract int viewportLength();

    // ================= 滚动事件处理 =================
    @Override
    public boolean onScroll(int mouseX, int mouseY, double deltaScroll) {
        if (!isHovering()) return false;

        // 优先交给子控件处理（如内部的 ScrollPane 或文本区）
        if (isHoveringPanel() && castedStream(getVisibleChildrenReverse(), IScrollable.class)
                .anyMatch(c -> c.onScroll(mouseX, mouseY, deltaScroll))) {
            return true;
        }

        double maxScroll = Math.max(0, contentLength - viewportLength());
        if (maxScroll <= 0) return false;

        double step = 20.0 / maxScroll;
        double prev = scrolled;
        if (deltaScroll > 0) {
            scrolled = Math.max(0, scrolled - step);
        } else if (deltaScroll < 0) {
            scrolled = Math.min(1.0, scrolled + step);
        }
        if (prev != scrolled) {
            controller.setValue(scrolled);
            return true;
        }
        return false;
    }

    // 滑块回调
    void onControllerChange(Slider ctrl) {
        scrolled = ctrl.getValue();
        requestLayout();
    }

    // ================= 垂直滚动实现 =================
    public static class Vertical extends ScrollPane {
        private Vertical(int width, int height) {
            super(width, height);
            this.controller = Slider.vertical(10, height, PlayerMessage.direct(""))
                                    .bound(0, 1, 0)
                                    .setDisplayPrecision(0)
                                    .callback(this::onControllerChange);
            addController(controller);
        }

        @Override
        protected int viewportLength() {
            return panelHeight();   // 内容区高度
        }

        @Override
        public int panelWidth() {
            int barWidth = controller.isVisible() ? controller.width() : 0;
            return super.width() - barWidth;
        }

        @Override
        public void layout(int x, int y) {
            setX(x);
            setY(y);

            // 计算内容总高度
            contentLength = getVisibleChildren().stream().mapToInt(ILayoutable::height).sum();

            // 滚动边界
            double maxScroll = Math.max(0, contentLength - viewportLength());
            if (maxScroll <= 0) {
                scrolled = 0;
                controller.setHandleSize(controller.height()); // 滑块占满轨道
            } else {
                // 滑块大小 = 视口比例 * 轨道长度
                int handleH = controller.height() * viewportLength() / contentLength;
                controller.setHandleSize(Math.max(8, handleH)); // 最小高度防止滑块消失
            }

            // 滚动偏移
            double scrollOffset = scrolled * maxScroll;

            // 内容区宽度
            int contentWidth = panelWidth();
            int currentY = y() - (int) scrollOffset;
            for (ILayoutable<?> child : getVisibleChildren()) {
                child.setX(x());
                child.setY(currentY);
                child.setWidth(contentWidth);          // 使用内容区宽度
                child.layout(x(), currentY);
                currentY += child.height();
            }

            // 滚动条放在右侧内部
            int barX = x() + panelWidth();
            controller.layout(barX, y());
        }
    }

    // ================= 水平滚动实现 =================
    public static class Horizontal extends ScrollPane {
        private Horizontal(int width, int height) {
            super(width, height);
            this.controller = Slider.horizontal(width, 10, PlayerMessage.direct(""))
                                    .bound(0, 1, 0)
                                    .setDisplayPrecision(0)
                                    .callback(this::onControllerChange);
            addController(controller);
        }

        @Override
        protected int viewportLength() {
            return panelWidth();   // 内容区宽度
        }

        @Override
        public int panelHeight() {
            int barHeight = controller.isVisible() ? controller.height() : 0;
            return super.height() - barHeight;
        }

        @Override
        public void layout(int x, int y) {
            setX(x);
            setY(y);

            // 计算内容总宽度
            contentLength = getVisibleChildren().stream().mapToInt(ILayoutable::width).sum();

            // 滚动边界
            double maxScroll = Math.max(0, contentLength - viewportLength());
            if (maxScroll <= 0) {
                scrolled = 0;
                controller.setHandleSize(controller.width()); // 滑块占满轨道
            } else {
                int handleW = controller.width() * viewportLength() / contentLength;
                controller.setHandleSize(Math.max(8, handleW));
            }

            // 滚动偏移
            double scrollOffset = scrolled * maxScroll;

            // 内容区高度
            int contentHeight = panelHeight();
            int currentX = x() - (int) scrollOffset;
            for (ILayoutable<?> child : getVisibleChildren()) {
                child.setX(currentX);
                child.setY(y());
                child.setHeight(contentHeight);        // 使用内容区高度
                child.layout(currentX, y());
                currentX += child.width();
            }

            // 滚动条放在底部内部
            int barY = y() + panelHeight();
            controller.layout(x(), barY);
        }
    }
}