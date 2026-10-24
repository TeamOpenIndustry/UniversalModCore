package cam72cam.mod.render.opengl;

import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.common.mesh.VAOLayout;
import cam72cam.mod.render.common.RenderContext;
import cam72cam.mod.util.With;
import net.minecraft.client.renderer.GLAllocation;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

public class GlRenderContext extends RenderContext {
    private static final ExecutorService pool = new ThreadPoolExecutor(0, Runtime.getRuntime().availableProcessors(),
            5L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            runnable -> {
                Thread thread = new Thread(runnable);
                thread.setName("UMC-GeometryLoader");
                thread.setPriority(Thread.MIN_PRIORITY);
                return thread;
            });

    private final AtomicBoolean bound = new AtomicBoolean(false);

    private static final AtomicLong nextHandle = new AtomicLong();
    private final Map<String, GlModelBuffer> buffers = new HashMap<>();

    private final IntBuffer intBuffer = BufferUtils.createIntBuffer(16);
    private final FloatBuffer floatBuffer = BufferUtils.createFloatBuffer(16);

    public GlRenderContext() {
        // free unused geometry
        ClientEvents.TICK.subscribe(() -> {
            GlRenderContext ctx = (GlRenderContext) RenderContext.get();
            long now = System.currentTimeMillis();
            for (GlModelBuffer buffer : ctx.buffers.values()) {
                if (buffer.vbo != -1 && now - buffer.lastUsed > (long) (30 * 1000)) {
                    deleteBuffer(buffer);
                }
            }
        });
    }

    @Override
    public String setGeometryUploader(Supplier<float[]> vboData, VAOLayout layout) {
        String key = "geometry-" + nextHandle.getAndIncrement();
        GlModelBuffer buffer = new GlModelBuffer();
        buffer.data = vboData;
        buffer.layout = layout;
        buffers.put(key, buffer);
        return key;
    }

    @Override
    public With bindGeometry(String handle) {
        if (bound.get()) {
            throw new IllegalStateException("UMC Model is already bound!");
        }
        if(!isUploaded(handle)) {
            throw new IllegalStateException("VBO data is used before fully loaded!");
        }

        GlModelBuffer buffer = buffers.get(handle);
        buffer.lastUsed = System.currentTimeMillis();

        bound.set(true);
        int oldVbo = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        GL11.glPushClientAttrib(GL11.GL_CLIENT_VERTEX_ARRAY_BIT);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer.vbo);
        return ((With) () -> {
            GL11.glPopClientAttrib();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, oldVbo);
            bound.set(false);
        }).and(setupVertexFormat(buffer.layout));
    }

    @Override
    public boolean isUploaded(String key) {
        GlModelBuffer buffer = buffers.get(key);
        return buffer != null && buffer.vbo != -1;
    }

    @Override
    public void drawGeometry(String key, int first, int count) {
        GL11.glDrawArrays(GL11.GL_TRIANGLES, first, count);
    }

    @Override
    public void freeGeometry(String key) {
        deleteBuffer(buffers.get(key));
    }

    @Override
    public void deleteGeometry(String key) {
        deleteBuffer(buffers.remove(key));
    }

    private static void deleteBuffer(GlModelBuffer buffer) {
        if (buffer == null) {
            return;
        }
        if (buffer.vbo != -1) {
            GL15.glDeleteBuffers(buffer.vbo);
            buffer.vbo = -1;
            buffer.vertexCount = -1;
        }
        buffer.loader = null;
    }

    @Override
    public With apply(RenderState state) {
        return cam72cam.mod.render.opengl.RenderContext.apply(state);
    }

    @Override
    protected With setupVertexFormat(VAOLayout layout) {
        layout.setup();
        return layout::restore;
    }

    @Override
    public void uploadGeometry(String handle, boolean waitForLoad) {
        GlModelBuffer buffer = buffers.get(handle);
        if (buffer == null || buffer.vbo != -1) {
            return;
        }

        if (buffer.loader == null) {
            buffer.loader = pool.submit(() -> {
                // Baked inside the lambda; only the resulting direct buffer survives.
                float[] data = buffer.data.get();
                FloatBuffer fb = GLAllocation.createDirectFloatBuffer(data.length);
                fb.put(data);
                fb.position(0);
                return fb;
            });
        }

        if (waitForLoad) {
            try {
                buffer.loader.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while loading geometry", e);
            } catch (ExecutionException e) {
                buffer.loader = null;
                throw new RuntimeException("Cannot create geometry buffer", e);
            }
        } else if (!buffer.loader.isDone()) {
            return;
        }

        try {
            FloatBuffer data = buffer.loader.get();
            int oldVbo = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
            buffer.vbo = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer.vbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data, GL15.GL_STATIC_DRAW);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, oldVbo);
            buffer.vertexCount = data.limit() / buffer.layout.getStride();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Cannot create geometry buffer", e);
        }
        buffer.loader = null;
    }

    @Override
    protected @Nullable ToDoubleFunction<Vec3d> getDepthSorter() {
        GL11.glGetInteger(GL11.GL_VIEWPORT, intBuffer);
        if (intBuffer.get(2) <= 0 || intBuffer.get(3) <= 0) {
            return null;
        }

        // Snapshot the matrix
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, floatBuffer);
        float[] m = new float[16];
        floatBuffer.rewind();
        floatBuffer.get(m);

        return pos -> -(m[2] * pos.x + m[6] * pos.y + m[10] * pos.z);
    }

    private static final class GlModelBuffer {
        int vbo = -1;
        int vertexCount = -1;
        long lastUsed = System.currentTimeMillis();
        Supplier<float[]> data;
        VAOLayout layout;
        Future<FloatBuffer> loader;
    }
}
