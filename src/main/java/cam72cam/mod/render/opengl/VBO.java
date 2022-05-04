package cam72cam.mod.render.opengl;

import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.model.obj.VertexBuffer;
import cam72cam.mod.util.With;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
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

    public VBO(Supplier<VertexBuffer> buffer, Consumer<RenderState> settings) {
        this.buffer = buffer;
        this.vbo = -1;
        this.settings = settings;

        synchronized (vbos) {
            vbos.add(this);
        }
    }

    private void init() {
        VertexBuffer vb = buffer.get();
        this.length = vb.data.length / (vb.stride);
        this.vbInfo = new VertexBuffer(0, vb.hasNormals);
        FloatBuffer buffer = BufferUtils.createFloatBuffer(vb.data.length);
        buffer.put(vb.data);
        buffer.position(0);

        int oldVbo = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);

        vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, oldVbo);
    }

    public Binding bind(RenderState state) {
        return new Binding(state);
    }

    public class Binding implements With {
        private final With restore;

        protected Binding(RenderState state) {
            if (vbo == -1) {
                init();
            }

            lastUsed = System.currentTimeMillis();

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
            RenderState state = new RenderState();
            mod.accept(state);
            return RenderContext.apply(state);
        }

        /**
         * Draw the entire VB
         */
        public void draw() {
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
