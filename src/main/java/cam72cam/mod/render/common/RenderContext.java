package cam72cam.mod.render.common;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.common.mesh.VAOLayout;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.util.With;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

@ApiStatus.Internal
public abstract class RenderContext {
    private static RenderContext INSTANCE;
    private DrawBatcher BATCHER;

    public static RenderContext get() {
        return INSTANCE;
    }

    public static void set(RenderContext renderContext) {
        if(INSTANCE != null) {
            throw new IllegalStateException("RenderContext is already initialized!");
        }
        renderContext.BATCHER = new DrawBatcher();
        INSTANCE = renderContext;
    }

    // Draw call encoder
    private Stage enqueueTo;

    // Callers are responsible and are only responsible to the following operations! No need to restore!
    public void startStage(@NotNull Stage stage) {
        enqueueTo = stage;
    }
    public void enqueueOpaque(IGeomDrawable drawable, RenderState state) {
        BATCHER.enqueueOpaque(enqueueTo, drawable, state);
    }
    public void enqueueTransparent(IGeomDrawable drawable, RenderState state) {
        BATCHER.enqueueTransparent(enqueueTo, drawable, state);
    }
    public void flushEnqueued() {
        BATCHER.flushEnqueued(null);
    }
    public void flushEnqueued(Stage stage) {
        BATCHER.flushEnqueued(stage);
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
     * Bind our geom data and setup essential fields for rendering<br>
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
     * Query the vertex count of an uploaded geometry
     * @param handle The geometry handle
     * @return Total vertices count
     */
    public abstract int getVertexCount(String handle);

    /**
     * Remove an uploaded geometry from GPU side
     * @param handle The geometry handle
     */
    public abstract void freeGeometry(String handle);

    /**
     * Completely remove an uploaded geometry and delete the handle
     * @param handle The geometry handle, which is not usable anymore after this operation
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

    public enum Stage {
        BLOCK,

        ENTITY, // Normal entity and particles

        ITEM_SPRITE_TEX,
        ITEM_IN_WORLD,
        ITEM_IN_GUI,

        GUI,

        OVERLAY,      //Mouseover...
        OVERLAY_TEXT, //Name plates...

        NONE
    }
}
