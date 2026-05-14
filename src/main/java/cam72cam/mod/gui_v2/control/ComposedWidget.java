package cam72cam.mod.gui_v2.control;

import cam72cam.mod.gui_v2.control.panel.SimplePane;

public abstract class ComposedWidget
        extends AbstractWidget {
    protected SimplePane internal;

    public ComposedWidget(int width, int height) {
        this.internal = new SimplePane(width, height);
    }

    @Override
    public void layout(int x, int y) {
        this.internal.setX(x);
        this.internal.setY(y);
        this.internal.layout(x, y);
    }

    @Override
    public int width() {
        return internal.width();
    }

    @Override
    public int height() {
        return internal.height();
    }
}
