package cam72cam.mod.render.common;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.common.mesh.VAOLayout;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.util.With;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

@ApiStatus.Internal
public abstract class RenderContext {
    private static RenderContext INSTANCE;

    protected List<DrawCall> opaqueCalls = new ArrayList<>();
    protected List<DrawCall> transCalls = new ArrayList<>();

    public static RenderContext get() {
        return INSTANCE;
    }

    public static void set(RenderContext renderContext) {
        if(INSTANCE != null) {
            throw new IllegalStateException("RenderContext is already initialized!");
        }
        INSTANCE = renderContext;
    }

    //Draw call encoder

    public void enqueueOpaque(IGeomDrawable drawable, RenderState state) {
        opaqueCalls.add(new DrawCall(drawable, state));
    }
    public void enqueueTransparent(IGeomDrawable drawable, RenderState state) {
        transCalls.add(new DrawCall(drawable, state));
    }
    public void flushEnqueued() {
        List<DrawCall> opaqueView = new ArrayList<>(opaqueCalls);
        opaqueCalls.clear();
        for (DrawCall call : opaqueView) {
            call.target().draw(call.state());
        }

        ToDoubleFunction<Vec3d> depth = getDepthSorter();
        if (depth != null) {
            transCalls.sort((a, b) -> {
                Vec3d va = a.center();
                Vec3d vb = b.center();
                if(va == null && vb == null) {
                    return 0;
                }
                if(vb == null) {
                    return -1;
                }
                if(va == null) {
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

    //Geometry part

    /**
     * Set up the geometry provider when using
     * @param vboData The data provider
     * @param layout Data's vertex layout
     * @return The handle used for future bindings
     */
    public abstract String setGeometryUploader(Supplier<float[]> vboData, VAOLayout layout);

    /**
     * Trigger geometry data loading if not loaded yet
     * @param handle The geometry handle
     * @param waitForLoad Should the loader block until the geometry is full loaded to GPU
     */
    public abstract void uploadGeometry(String handle, boolean waitForLoad);

    /**
     * Check if a handle's corresponding data is uploaded
     * @param handle The geometry handle
     */
    public abstract boolean isUploaded(String handle);

    /**
     * Bind our geom data for rendering<br>
     * Must be called after {@code isLoaded} check or uploading
     * @param handle The geometry handle
     * @return The restorer to restore geometry binding
     */
    public abstract With bindGeometry(String handle);

    /**
     * Draw an uploaded geometry instantly, or enqueue to backing rendering engine
     * @param handle The geometry handle
     * @param first The first vertex offset
     * @param count Total vertices count
     */
    public abstract void drawGeometry(String handle, int first, int count);

    /**
     * Remove an uploaded geometry from GPU side
     * @param handle The geometry handle
     */
    public abstract void freeGeometry(String handle);

    /**
     * Completely remove an uploaded geometry and delete the handle
     * @param handle The geometry handle
     */
    public abstract void deleteGeometry(String handle);

    /**
     * Internal helper to set up vertex pointer/VAO
     */
    protected abstract With setupVertexFormat(VAOLayout layout);

    //State setter

    /**
     * Setter to apply overall RenderState
     * @param state RenderState being used
     * @return The restorer to restore rendering sets
     */
    public abstract With apply(RenderState state);

    /**
     * @return A function mapping position to distance from camera/in the direction camera views, useful for transparency sorting
     */
    protected abstract @Nullable ToDoubleFunction<Vec3d> getDepthSorter();
}
