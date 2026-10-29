package cam72cam.mod.render.common;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.opengl.BlendMode;
import cam72cam.mod.render.opengl.RenderState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;

public class DrawBatcher {
    protected List<DrawCall> opaqueCalls = new ArrayList<>();
    protected List<DrawCall> transCalls = new ArrayList<>();

    public void enqueueOpaque(RenderContext.Stage enqueueTo, IGeomDrawable drawable, RenderState state) {
        opaqueCalls.add(new DrawCall(drawable, state.clone()));
    }
    public void enqueueTransparent(RenderContext.Stage enqueueTo, IGeomDrawable drawable, RenderState state) {
        state = state.clone().depth_mask(false);
        if (state.getBlendMode() == null){
            state = state.clone().blend(BlendMode.ALPHA_BLEND);
        }
        transCalls.add(new DrawCall(drawable, state));
    }

    public void flushEnqueued(RenderContext.Stage stage) {
        List<DrawCall> opaqueView = new ArrayList<>(opaqueCalls);
        opaqueCalls.clear();
        for (DrawCall call : opaqueView) {
            call.target().draw(call.state());
        }

        if(!transCalls.isEmpty()) {
            ToDoubleFunction<Vec3d> depth = RenderContext.get().getDepthSorter();
            if (depth != null) {
                transCalls.sort((a, b) -> {
                    Vec3d va = a.center();
                    Vec3d vb = b.center();
                    if (va == null && vb == null) {
                        return 0;
                    }
                    if (vb == null) {
                        return -1;
                    }
                    if (va == null) {
                        return 1;
                    }
                    return Double.compare(depth.applyAsDouble(vb), depth.applyAsDouble(va));
                });
            }

            for (DrawCall call : transCalls) {
                call.target().draw(call.state());
            }
            transCalls.clear();
        }
    }
}
