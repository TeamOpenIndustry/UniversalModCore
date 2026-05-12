package cam72cam.mod.gui_v2.core;

import cam72cam.mod.render.opengl.RenderContext;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.util.With;

import java.awt.geom.Rectangle2D;
import java.util.ArrayDeque;
import java.util.Deque;

public class ScissorStack {
    Deque<Rectangle2D> stack;

    public ScissorStack() {
        stack = new ArrayDeque<>();
    }

    public ScissorStack push(ILayoutable<?> widget) {
        return push(new Rectangle2D.Double(widget.x(), widget.y(), widget.width(), widget.height()));
    }

    public ScissorStack push(int x, int y, int width, int height) {
        return push(new Rectangle2D.Double(x, y, width, height));
    }

    public ScissorStack push(Rectangle2D r) {
        if (!stack.isEmpty()) {
            r = r.createIntersection(stack.peek());
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
