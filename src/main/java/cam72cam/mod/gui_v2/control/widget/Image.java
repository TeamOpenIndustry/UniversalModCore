package cam72cam.mod.gui_v2.control.widget;

import cam72cam.mod.gui_v2.control.AbstractWidget;
import cam72cam.mod.resource.Identifier;

public class Image extends AbstractWidget<Image> {
    protected Identifier tex;
    protected float uStart;
    protected float uEnd;
    protected float vStart;
    protected float vEnd;

    protected Image(int width, int height, Identifier tex) {
        this.setWidth(width);
        this.setHeight(height);
        this.tex = tex;

        this.setRenderer((gui, image) -> {
            gui.blitTexture(image.tex, image.x(), image.y(), image.width(), image.height(),
                                   image.uStart, image.uEnd, image.vStart, image.vEnd);
        });
    }

    public static Image of(int width, int height, Identifier tex) {
        return new Image(width, height, tex).uvBound(0, 0, 1, 1);
    }

    public Image uvBound(float uStart, float vStart, float uEnd, float vEnd) {
        this.uStart = uStart;
        this.vStart = vStart;
        this.uEnd = uEnd;
        this.vEnd = vEnd;
        return this;
    }

    @Override
    public void layout(int x, int y) {
        this.setX(x);
        this.setY(y);
    }
}
