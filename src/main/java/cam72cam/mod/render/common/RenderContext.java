package cam72cam.mod.render.common;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.common.mesh.VAOLayout;
import cam72cam.mod.render.GlobalRender;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.util.With;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

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

    public abstract With apply(RenderState state);

    public void enqueueOpaque(IDrawable drawable, RenderState state) {
        opaqueCalls.add(new DrawCall(drawable, state));
    }
    public void enqueueTransparent(IDrawable drawable, RenderState state) {
        transCalls.add(new DrawCall(drawable, state));
    }
    public void flushEnqueued(float partialTicks) {
        List<DrawCall> opaqueView = new ArrayList<>(opaqueCalls);
        opaqueCalls.clear();
        for (DrawCall call : opaqueView) {
            call.target.draw(call.state);
        }

        transCalls.sort((a, b) -> {
            Vec3d va = a.center;
            Vec3d vb = b.center;
            if(va == null && vb == null) {
                return 0;
            }
            if(vb == null) {
                return -1;
            }
            if(va == null) {
                return 1;
            }
            Vec3d camera = GlobalRender.getCameraPos(partialTicks);
            double distA = va.distanceToSquared(camera);
            double distB = vb.distanceToSquared(camera);
            if(distA == distB) return 0;
            return distA > distB ? 1 : -1;
        });

        for (DrawCall call : transCalls) {
            call.target.draw(call.state);
        }
        transCalls.clear();
    }

    public abstract String setGeometryUploader(Supplier<float[]> vboData, VAOLayout layout);
    public abstract void uploadGeometry(String handle);
    //Auto restore
    public abstract With bindGeometry(String handle);
    protected abstract With setupVertexFormat(VAOLayout layout);
    public abstract boolean isLoaded(String handle);
    public abstract int vertexCount(String handle);
    public abstract void drawGeometry(String handle, int first, int count);
    public abstract void deleteGeometry(String handle);
}
