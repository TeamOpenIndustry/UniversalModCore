package cam72cam.mod.model.common.opengl;

import cam72cam.mod.util.With;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;

//Pure VBO without EBO
public class VertexBuffer {
    private final float[] data;
    private int vbo = -1;

    public VertexBuffer(float[] data) {
        this.data = data;
    }

    private VertexBuffer(long nativePtr) {
        //TODO
        data = null;
    }

    public int getVbo() {
        return vbo;
    }

    public float[] getData() {
        return data;
    }

    public void upload() {
        int oldVbo = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);

        vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(vbo, FloatBuffer.wrap(data), GL15.GL_STATIC_DRAW);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, oldVbo);
    }

    public void close() {
        if (vbo != -1) {
            GL15.glDeleteBuffers(vbo);
            vbo = -1;
        }
    }

    public With setup() {
        if (vbo == -1) return () -> {};
        int oldVbo = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        return () -> {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, oldVbo);
        };
    }
}
