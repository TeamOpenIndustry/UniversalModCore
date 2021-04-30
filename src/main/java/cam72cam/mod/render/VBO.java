package cam72cam.mod.render;

import cam72cam.mod.model.obj.OBJGroup;
import cam72cam.mod.model.obj.OBJModel;
import cam72cam.mod.model.obj.VertexBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.util.vector.Vector3f;
import util.Matrix4;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class VBO {
    private final OBJModel model;
    private final int vbo;
    private final int length;
    private final VertexBuffer vbInfo;

    /**
     * Create a buffer with number of verts
     */
    public VBO(OBJModel model) {
        this(model, model.vbo.get());
    }

    private VBO(OBJModel model, VertexBuffer vb) {
        this.model = model;
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

    public BoundVBO bind() {
        return new BoundVBO();
    }

    public class BoundVBO implements OpenGL.With {
        private final int prev;
        private final OpenGL.With color;

        private BoundVBO() {
            prev = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
            GL11.glPushClientAttrib(GL11.GL_CLIENT_VERTEX_ARRAY_BIT);

            GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
            GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
            if (vbInfo.hasNormals) {
                GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
            } else {
                GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
            }

            color = OpenGL.color(1, 1, 1, 1);

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

            int stride = vbInfo.stride * Float.BYTES;
            GL11.glVertexPointer(3, GL11.GL_FLOAT, stride, vbInfo.vertexOffset * Float.BYTES);
            GL11.glTexCoordPointer(2, GL11.GL_FLOAT, stride, vbInfo.textureOffset * Float.BYTES);
            GL11.glColorPointer(4, GL11.GL_FLOAT, stride, vbInfo.colorOffset * Float.BYTES);
            if (vbInfo.hasNormals) {
                GL11.glNormalPointer(GL11.GL_FLOAT, stride, vbInfo.normalOffset * Float.BYTES);
            }
        }

        @Override
        public void restore() {
            GL11.glPopClientAttrib();

            color.restore();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, prev);
        }

        /**
         * Draw the entire VB
         */
        public void draw() {
            drawVBO(null);
        }

        /**
         * Draw these groups in the VB
         */
        public void draw(Collection<String> groups) {
            drawVBO(groups);
        }

        private void drawVBO(Collection<String> groups) {
            if (groups == null) {
                GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, length);
            } else {
                List<String> sorted = new ArrayList<>(groups);
                sorted.sort(Comparator.naturalOrder());
                int start = -1;
                int stop = -1;
                for (String group : sorted) {
                    OBJGroup info = model.groups.get(group);
                    if (start == stop) {
                        start = info.faceStart;
                        stop = info.faceStop + 1;
                    } else if (info.faceStart == stop) {
                        stop = info.faceStop + 1;
                    } else {
                        GL11.glDrawArrays(GL11.GL_TRIANGLES, start * 3, (stop - start) * 3);
                        start = info.faceStart;
                        stop = info.faceStop + 1;
                    }
                }
                if (start != stop) {
                    GL11.glDrawArrays(GL11.GL_TRIANGLES, start * 3, (stop - start) * 3);
                }
            }
        }
    }

    /**
     * Clear this VB from standard and GPU memory
     */
    public void free() {
        GL15.glDeleteBuffers(vbo);
    }

    public static class Builder {
        private final OBJModel model;
        private final VertexBuffer vb;
        private float[] built;
        private int builtIdx;

        public Builder(OBJModel model) {
            this.model = model;
            this.vb = model.vbo.get();
            this.built = new float[vb.data.length];
            this.builtIdx = 0;

        }

        private void require(int size) {
            while (built.length <= builtIdx + size) {
                float[] tmp = new float[built.length * 2];
                System.arraycopy(built, 0, tmp, 0, builtIdx);
                built = tmp;
            }
        }
        private void add(float[] buff, Matrix4 m) {
            require(buff.length);

            if (m != null) {
                for (int i = 0; i < buff.length; i += vb.stride) {
                    float x = buff[i+0];
                    float y = buff[i+1];
                    float z = buff[i+2];
                    Vector3f v = m.apply(new Vector3f(x, y, z));
                    buff[i+0] = v.x;
                    buff[i+1] = v.y;
                    buff[i+2] = v.z;
                }
            }

            System.arraycopy(buff, 0, built, builtIdx, buff.length);
            builtIdx += buff.length;
        }

        public void draw() {
            draw((Matrix4) null);
        }

        public void draw(Matrix4 m) {
            if (m == null) {
                add(vb.data, null);
            } else {
                float[] buff = new float[vb.data.length];
                System.arraycopy(vb.data, 0, buff, 0, vb.data.length);
                add(buff, m);
            }
        }

        public void draw(Collection<String> groups) {
            draw(groups, null);
        }

        public void draw(Collection<String> groups, Matrix4 m) {
            for (String group : groups) {
                OBJGroup info = model.groups.get(group);

                int start = info.faceStart * vb.vertsPerFace * vb.stride;
                int stop = (info.faceStop + 1) * vb.vertsPerFace * vb.stride;

                float[] buff = new float[stop - start];
                System.arraycopy(vb.data, start, buff, 0, stop - start);
                add(buff, m);
            }
        }

        public VBO build() {
            float[] out = new float[builtIdx];
            System.arraycopy(built, 0, out, 0, builtIdx);
            return new VBO(model, new VertexBuffer(out, vb.hasNormals));
        }
    }
}
