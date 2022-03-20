package cam72cam.mod.render.opengl;

import cam72cam.mod.model.obj.VertexBuffer;
import cam72cam.mod.render.GLSLShader;
import cam72cam.mod.util.With;
import cam72cam.mod.resource.Identifier;
import net.minecraft.client.renderer.GLAllocation;
import org.lwjgl.opengl.*;

import java.nio.FloatBuffer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class VBO {
    private final Supplier<VertexBuffer> buffer;
    private final Consumer<RenderState> settings;
    private GLSLShader shader = null;

    private int vbo;
    private int length;
    private VertexBuffer vbInfo;

    public VBO(Supplier<VertexBuffer> buffer, Consumer<RenderState> settings) {
        this.buffer = buffer;
        this.vbo = -1;
        this.settings = settings;
    }

    private void init() {
        if (shader == null) {
            this.shader = new GLSLShader(
                    new Identifier("universalmodcore:shaders/std.vert"),
                    new Identifier("universalmodcore:shaders/std.frag")
            );
        }

        VertexBuffer vb = buffer.get();
        this.length = vb.data.length / (vb.stride);
        this.vbInfo = new VertexBuffer(0, vb.hasNormals);
        FloatBuffer buffer = GLAllocation.createDirectFloatBuffer(vb.data.length);
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
        if (vbo != -1) {
            GL15.glDeleteBuffers(vbo);
            vbo = -1;
        }
    }
}
