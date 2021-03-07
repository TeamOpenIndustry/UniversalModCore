package cam72cam.mod.render;

import cam72cam.mod.model.obj.OBJGroup;
import cam72cam.mod.model.obj.OBJModel;
import cam72cam.mod.serialization.ResourceCache;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class VBO {
    private final OBJModel model;
    private final int vbo;

    /** Create a buffer with number of verts */
    public VBO(OBJModel model) {
        this.model = model;

        // Byte buffer is faster than float buffer, but good enough for now
        ResourceCache.GenericByteBuffer tmp = model.vbo.get();
        float[] floats = tmp.floats();
        ByteBuffer buffer = ByteBuffer.allocateDirect(floats.length * Float.BYTES).order(ByteOrder.nativeOrder());
        buffer.asFloatBuffer().put(floats);

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
            GL11.glPushClientAttrib( GL11.GL_CLIENT_VERTEX_ARRAY_BIT);

            GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
            GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
            if (model.hasVertexNormals) {
                GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
            } else {
                GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
            }

            color = OpenGL.color(1, 1, 1, 1);

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

            int stride = (model.hasVertexNormals ? 12 : 9) * Float.BYTES;
            GL11.glVertexPointer(3, GL11.GL_FLOAT, stride, (0) * Float.BYTES);
            GL11.glTexCoordPointer(2, GL11.GL_FLOAT, stride, (3) * Float.BYTES);
            GL11.glColorPointer(4, GL11.GL_FLOAT, stride, (3+2) * Float.BYTES);
            if (model.hasVertexNormals) {
                GL11.glNormalPointer(GL11.GL_FLOAT, stride, (3+2+4) * Float.BYTES);
            }
        }
        @Override
        public void restore() {
            GL11.glPopClientAttrib();

            color.restore();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, prev);
        }

        /** Draw the entire VB */
        public void draw() {
                                 drawVBO(null);
                                                }

        /** Draw these groups in the VB */
        public void draw(Collection<String> groups) {
                                                        drawVBO(groups);
                                                                        }

        private void drawVBO(Collection<String> groups) {
            if (groups == null) {
                GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, new ArrayList<>(model.groups.values()).get(model.groups.size()-1).faceStop * 3 + 3);
            } else {
                List<String> sorted = new ArrayList<>(groups);
                sorted.sort(Comparator.naturalOrder());
                int start = -1;
                int stop = -1;
                for (String group : groups) {
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

    /** Clear this VB from standard and GPU memory */
    public void free() {
        GL15.glDeleteBuffers(vbo);
    }
}
