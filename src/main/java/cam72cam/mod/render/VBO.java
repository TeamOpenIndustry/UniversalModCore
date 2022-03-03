package cam72cam.mod.render;

import cam72cam.mod.model.obj.VertexBuffer;
import cam72cam.mod.render.opengl.LegacyRenderContext;
import cam72cam.mod.render.opengl.RenderState;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class VBO {
    private final int vbo;
    private final int length;
    private final VertexBuffer vbInfo;

    public VBO(VertexBuffer vb) {
        this.length = vb.data.length / (vb.stride);
        this.vbInfo = new VertexBuffer(0, vb.hasNormals);
        ByteBuffer buffer = ByteBuffer.allocateDirect(vb.data.length * Float.BYTES).order(ByteOrder.nativeOrder());
        buffer.asFloatBuffer().put(vb.data);

        int prev = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, prev);
    }

    public Binding bind(RenderState state) {
        return new Binding(state);
    }

    public class Binding implements OpenGL.With {
        private final int prev;
        private final OpenGL.With restore;

        protected Binding(RenderState state) {
            this.restore = LegacyRenderContext.INSTANCE.apply(state);

            this.prev = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
            GL11.glPushClientAttrib(GL11.GL_CLIENT_VERTEX_ARRAY_BIT);

            GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
            GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
            if (vbInfo.hasNormals) {
                GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
            } else {
                GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
            }

            //GL11.glColor4f(1, 1, 1, 1);

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
            GL11.glPopClientAttrib();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, prev);
            restore.close();
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
        GL15.glDeleteBuffers(vbo);
    }
}
