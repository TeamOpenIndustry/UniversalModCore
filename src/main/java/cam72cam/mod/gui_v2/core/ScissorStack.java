package cam72cam.mod.gui_v2.core;

import cam72cam.mod.render.opengl.RenderContext;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.util.With;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayDeque;
import java.util.Deque;

public class ScissorStack {
    private static final Rectangle2D EMPTY = new Rectangle(0, 0, 0, 0);

    Deque<Rectangle2D> stack;

    public ScissorStack() {
        stack = new ArrayDeque<>();
    }

    public ScissorStack push(ILayoutable<?> widget) {
        return push(new Rectangle(widget.x(), widget.y(), widget.width(), widget.height()));
    }

    public ScissorStack push(int x, int y, int width, int height) {
        return push(new Rectangle(x, y, width, height));
    }

    public ScissorStack push(Rectangle2D r) {
        if (!stack.isEmpty()) {
            if (r.intersects(stack.peek())) {
                r = r.createIntersection(stack.peek());
            } else {
                r = EMPTY;
            }
        }
        stack.push(r);
        return this;
    }

    public ScissorStack pop() {
        stack.pop();
        return this;
    }

    public With applyScissor() {
        if (!stack.isEmpty()) {
            return RenderContext.apply(new RenderState().scissor(true, stack.peek()));
        }
        return () -> {};
    }
}
