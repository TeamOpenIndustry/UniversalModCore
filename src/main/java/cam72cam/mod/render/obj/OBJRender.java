package cam72cam.mod.render.obj;

import cam72cam.mod.model.obj.OBJGroup;
import cam72cam.mod.model.obj.OBJModel;
import cam72cam.mod.model.obj.VertexBuffer;
import cam72cam.mod.util.With;
import cam72cam.mod.render.opengl.VBO;
import cam72cam.mod.render.opengl.RenderState;
import org.lwjgl.opengl.GL11;
import util.Matrix4;

import javax.vecmath.Matrix3f;
import javax.vecmath.SingularMatrixException;
import javax.vecmath.Vector3f;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class OBJRender extends VBO {
    public final OBJModel model;
    public final Supplier<VertexBuffer> buffer;

    public OBJRender(OBJModel model, Supplier<VertexBuffer> buffer) {
        super(buffer, s -> {});
        this.model = model;
        this.buffer = buffer;
    }

    public Binding bind(RenderState state) {
        return bind(state, false);
    }
    public Binding bind(RenderState state, boolean waitForLoad) {
        return new Binding(state, waitForLoad);
    }

    public class Binding extends VBO.Binding {
        protected Binding(RenderState state, boolean wait) {
            super(state, wait);
        }

        public void draw(Collection<String> groups, Consumer<RenderState> mod) {
            if (!isLoaded()) {
                return;
            }
            try (With pus = super.push(mod)) {
                draw(groups);
            }
        }

        /**
         * Draw these groups in the VB
         */
        public void draw(Collection<String> groups) {
            if (!isLoaded()) {
                return;
            }

            List<OBJGroup> ordered = new ArrayList<>(groups.size());
            for (String name : groups) {
                ordered.add(model.groups.get(name));
            }
            ordered.sort(Comparator.comparingInt(g -> g.faceStart));

            int start = -1;
            int stop = -1;
            for (OBJGroup info : ordered) {
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

    public class Builder {
        private final Consumer<RenderState> settings;
        private final List<Consumer<Buffer>> actions = new ArrayList<>();

        private Builder(Consumer<RenderState> settings) {
            this.settings = settings;
        }

        private class Buffer {
            private VertexBuffer vb;
            private float[] built;
            private int builtIdx;

            private Buffer() {
                this.vb = buffer.get();
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
                    Matrix3f normalMat = new Matrix3f(
                            (float) m.m00, (float) m.m01, (float) m.m02,
                            (float) m.m10, (float) m.m11, (float) m.m12,
                            (float) m.m20, (float) m.m21, (float) m.m22
                    );
                    try {
                        normalMat.invert();
                    } catch (SingularMatrixException ignore) {
                        //Nothing to do here
                    }
                    normalMat.transpose();
                    for (int i = 0; i < buff.length; i += vb.stride) {
                        float x = buff[i+0];
                        float y = buff[i+1];
                        float z = buff[i+2];
                        Vector3f v = new Vector3f(x, y, z);
                        m.apply(v);
                        buff[i+0] = v.x;
                        buff[i+1] = v.y;
                        buff[i+2] = v.z;

                        if (vb.hasNormals) {
                            float nx = buff[i+0+vb.normalOffset];
                            float ny = buff[i+1+vb.normalOffset];
                            float nz = buff[i+2+vb.normalOffset];
                            Vector3f n = new Vector3f(nx, ny, nz);
                            normalMat.transform(n);
                            n.normalize();
                            buff[i+0+vb.normalOffset] = n.x;
                            buff[i+1+vb.normalOffset] = n.y;
                            buff[i+2+vb.normalOffset] = n.z;
                        }
                    }
                }

                System.arraycopy(buff, 0, built, builtIdx, buff.length);
                builtIdx += buff.length;
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

            public VertexBuffer build() {
                float[] out = new float[builtIdx];
                System.arraycopy(built, 0, out, 0, builtIdx);
                boolean hasNormals = vb.hasNormals;
                vb = null;
                built = null;
                return new VertexBuffer(out, hasNormals);
            }
        }

        public void draw() {
            draw((Matrix4) null);
        }

        public void draw(Matrix4 m) {
            actions.add(b -> b.draw(m));
        }

        public void draw(Collection<String> groups) {
            draw(groups, null);
        }

        public void draw(Collection<String> groups, Matrix4 m) {
            actions.add(b -> b.draw(groups, m));
        }

        public VBO build() {
            List<Consumer<Buffer>> actions = new ArrayList<>(this.actions); // Snapshot
            return new VBO(() -> {
                Buffer buff = new Buffer();
                actions.forEach(c -> c.accept(buff));
                return buff.build();
            }, settings);
        }
    }

    public Builder subModel(Consumer<RenderState> settings) {
        return new Builder(settings);
    }
}
