package cam72cam.mod.render.common;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.opengl.RenderState;
import org.jetbrains.annotations.Nullable;

public interface IDrawable {
    void draw(RenderState state);
    //Used for batching
    @Nullable String getRenderHandle();
    //Used for transparent sorting
    @Nullable Vec3d getCenter();

    default void free() {
        if (getRenderHandle() != null) {
            RenderContext.get().freeGeometry(getRenderHandle());
        }
    }

    //TODO Due to historical issues UMC 1.3 doesn't call flush, use at your own risk!
    default void enqueueOpaque(RenderState state) {
        RenderContext.get().enqueueOpaque(this, state);
    }
    default void enqueueTransparent(RenderState state) {
        RenderContext.get().enqueueTransparent(this, state);
    }
}
