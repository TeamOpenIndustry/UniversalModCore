package cam72cam.mod.render.common;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.opengl.RenderState;

public class DrawCall {
    private final IGeomDrawable target;
    private final RenderState state;
    private final Vec3d center;

    public DrawCall(IGeomDrawable target, RenderState state) {
        this.target = target;
        this.state = state;
        this.center = target.getCenter() != null ? state.model_view().apply(target.getCenter()) : null;
    }

    public IGeomDrawable target() {
        return target;
    }

    public RenderState state() {
        return state;
    }

    public Vec3d center() {
        return center;
    }
}
