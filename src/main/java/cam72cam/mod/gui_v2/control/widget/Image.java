package cam72cam.mod.gui_v2.control.widget;

import cam72cam.mod.gui_v2.control.AbstractWidget;
import cam72cam.mod.resource.Identifier;

public class Image extends AbstractWidget<Image> {
    protected Identifier tex;
    protected float uStart;
    protected float uEnd;
    protected float vStart;
    protected float vEnd;

    public Image(int width, int height, Identifier tex, float uStart, float uEnd, float vStart, float vEnd) {
        this.setWidth(width);
        this.setHeight(height);
        this.tex = tex;
        this.uStart = uStart;
        this.uEnd = uEnd;
        this.vStart = vStart;
        this.vEnd = vEnd;

        this.setRenderFunc((gui, image) -> {
            gui.drawTexturedUvRect(image.tex, image.x(), image.y(), image.width(), image.height(),
                                   image.uStart, image.uEnd, image.vStart, image.vEnd);
        });
    }

    public static Image fullTex(int width, int height, Identifier tex) {
        return new Image(width, height, tex, 0, 1, 0, 1);
    }

    @Override
    public void layout(int x, int y) {
        this.setX(x);
        this.setY(y);
    }
}
