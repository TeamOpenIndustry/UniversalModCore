package cam72cam.mod.gui_v2.control.panel;

import cam72cam.mod.gui_v2.control.AbstractPanel;
import cam72cam.mod.gui_v2.control.widget.Slider;
import cam72cam.mod.gui_v2.core.ILayoutable;
import cam72cam.mod.gui_v2.core.actions.IScrollable;
import cam72cam.mod.text.PlayerMessage;

public class ScrollPane extends AbstractPanel<ScrollPane>
        implements IScrollable {
    //TODO
    private final Slider controller;
    private double scrolled;
    private int lastLaidHeight;

    public ScrollPane(int x, int y, int width, int height) {
        super(x, y, width, height);
        this.controller = new Slider(width, height, PlayerMessage.direct(""), 0, 1, 0, false, this::onControllerChange);
    }

    @Override
    public void layout(int x, int y) {
        this.setX(x);
        this.setY(y);

        int h = children.stream().mapToInt(ILayoutable::height).sum();
        lastLaidHeight = (int) (Math.max(0, h - this.height()) * scrolled);

        int rel = lastLaidHeight;
        for (ILayoutable<?> widget : children) {
            widget.setX(0);
            widget.setY(rel);
            widget.layout(x, rel);
            rel += widget.height();
        }
    }

    @Override
    public boolean onScroll(int mouseX, int mouseY, double deltaScroll) {
        if (!isHovering()) {
            return false;
        }

        if (deltaScroll > 0) {
            controller.setValue(controller.getValue() + 0.05 / lastLaidHeight);
            return true;
        } else  if (deltaScroll < 0) {
            controller.setValue(controller.getValue() - 0.05 / lastLaidHeight);
            return true;
        }

        return super.onScroll(mouseX, mouseY, deltaScroll);
    }

    private void onControllerChange(Slider ctrl) {
        scrolled = ctrl.getValue();
        layout(this.x(), this.y());
    }
}
