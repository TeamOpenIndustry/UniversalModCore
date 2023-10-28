package cam72cam.mod.render.opengl;

import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.model.obj.VertexBuffer;
import cam72cam.mod.util.With;
import net.minecraft.client.renderer.GLAllocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class VBO {
    private static final List<VBO> vbos = new ArrayList<>();
    public static void registerClientEvents() {
        // free unused textures
        ClientEvents.TICK.subscribe(() -> {
            synchronized (vbos) {
                for (VBO vbo : vbos) {
                    if (vbo.vbo != -1 && System.currentTimeMillis() - vbo.lastUsed > 30 * 1000) {
                        vbo.free();
                    }
                }
            }
        });
    }

    private final Supplier<VertexBuffer> buffer;
    private final Consumer<RenderState> settings;

    private int vbo;
    private int length;
    private long lastUsed;
    private VertexBuffer vbInfo;

    private static final ExecutorService pool = new ThreadPoolExecutor(0, Runtime.getRuntime().availableProcessors(),
            5L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            runnable -> {
                Thread thread = new Thread(runnable);
                thread.setName("UMC-VertexBufferLoader");
                thread.setPriority(Thread.MIN_PRIORITY);
                return thread;
            });
    private Future<FloatBuffer> loader = null;

    public VBO(Supplier<VertexBuffer> buffer, Consumer<RenderState> settings) {
        this.buffer = buffer;
        this.vbo = -1;
        this.settings = settings;

        synchronized (vbos) {
            vbos.add(this);
        }
    }

    private void init() {
        if (loader != null) {
            if (loader.isDone()) {
                try {
                    int oldVbo = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);

                    vbo = GL15.glGenBuffers();
                    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
                    GL15.glBufferData(GL15.GL_ARRAY_BUFFER, loader.get(), GL15.GL_STATIC_DRAW);

                    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, oldVbo);
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
                loader = null;
            }
        } else {
            // Start thread
            loader = pool.submit(() -> {
                VertexBuffer vb = buffer.get();
                this.length = vb.data.length / (vb.stride);
                this.vbInfo = new VertexBuffer(0, vb.hasNormals);
                FloatBuffer buffer = GLAllocation.createFloatBuffer(vb.data.length);
                buffer.put(vb.data);
                buffer.position(0);
                return buffer;
            });
        }
    }

    public Binding bind(RenderState state) {
        return bind(state, false);
    }

    public Binding bind(RenderState state, boolean waitForLoad) {
        return new Binding(state, waitForLoad);
    }

    public class Binding implements With {
        private final With restore;

        public boolean isLoaded() {
            return vbo != -1;
        }

        protected Binding(RenderState state, boolean wait) {
            lastUsed = System.currentTimeMillis();

            if (!isLoaded()) {
                init();
            }

            if (!wait) {
                if (!isLoaded()) {
                    restore = () -> {
                    };
                    return;
                }
            } else {
                while (!isLoaded()) {
                    init();
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            settings.accept(state);

            int oldVbo = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
            GL11.glPushClientAttrib(GL11.GL_CLIENT_VERTEX_ARRAY_BIT);

            this.restore = RenderContext.apply(state).and(() -> {
                GL11.glPopClientAttrib();
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, oldVbo);
            });


            GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
            GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
            if (vbInfo.hasNormals) {
                GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
            } else {
                GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
            }

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

            int stride = vbInfo.stride * Float.BYTES;
            GL11.glVertexPointer(3, GL11.GL_FLOAT, stride, (long) vbInfo.vertexOffset * Float.BYTES);
            GL11.glTexCoordPointer(2, GL11.GL_FLOAT, stride, (long) vbInfo.textureOffset * Float.BYTES);
            GL11.glColorPointer(4, GL11.GL_FLOAT, stride, (long) vbInfo.colorOffset * Float.BYTES);
            if (vbInfo.hasNormals) {
                GL11.glNormalPointer(GL11.GL_FLOAT, stride, (long) vbInfo.normalOffset * Float.BYTES);
            }
        }

        @Override
        public void restore() {
            restore.close();
        }

        protected With push(Consumer<RenderState> mod) {
            if (!isLoaded()) {
                return () -> {};
            }
            RenderState state = new RenderState();
            mod.accept(state);
            return RenderContext.apply(state);
        }

        /**
         * Draw the entire VB
         */
        public void draw() {
            if (!isLoaded()) {
                return;
            }
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, length);
        }
    }

    /**
     * Clear this VB from standard and GPU memory
     */
    public void free() {
        synchronized (vbos) {
            if (vbo != -1) {
                GL15.glDeleteBuffers(vbo);
                vbo = -1;
            }
        }
    }
}
