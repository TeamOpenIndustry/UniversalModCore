package cam72cam.mod.gui_v2.control.widget;

import cam72cam.mod.gui_v2.control.AbstractWidget;

/**
 * Empty widget used for padding
 */
public class Spacing extends AbstractWidget<Spacing> {
    protected Spacing(int width, int height) {
        this.setBound(0, 0, width, height);
    }

    public static Spacing of(int width, int height) {
        return new Spacing(width, height);
    }

    @Override
    public void layout(int x, int y) {
        this.setX(x);
        this.setY(y);
    }
}
