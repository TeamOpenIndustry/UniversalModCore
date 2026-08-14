package cam72cam.mod.model.common.util;

import cam72cam.mod.model.common.mesh.GeneratedModel;
import cam72cam.mod.model.common.mesh.Model;
import cam72cam.mod.model.common.mesh.ModelGroup;
import cam72cam.mod.model.common.mesh.VAOLayout;
import cam72cam.mod.resource.Identifier;
import util.Matrix4;

import javax.vecmath.Matrix3f;
import javax.vecmath.SingularMatrixException;
import javax.vecmath.Vector3f;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Structured re-implementation of OBJRender.Builder for the common Model. */
public class ModelSetBuilder {
    private static final AtomicInteger nextId = new AtomicInteger(0);

    private final Model model;
    private final VAOLayout layout;
    private final boolean hasNormal;
    private final List<Action> actions = new ArrayList<>();

    public ModelSetBuilder(Model model) {
        this.model = model;
        this.layout = model.getLayout();
        this.hasNormal = layout.has(VAOLayout.Usage.NORMAL);
    }

    public ModelSetBuilder append(Matrix4 m) {
        actions.add((vbo, out) -> add(out, vbo, 0, vbo.length / layout.getStride(), m));
        return this;
    }

    public ModelSetBuilder append(Collection<String> groups, Matrix4 m) {
        actions.add((vbo, out) -> {
            for (String name : groups) {
                ModelGroup group = model.getGroups().get(name);
                add(out, vbo, group.faceStart * 3, group.faceEnd * 3, m);
            }
        });
        return this;
    }

    private void add(Buffers.FloatBuffer out, float[] vbo, int startVert, int endVert, Matrix4 m) {
        int stride = layout.getStride();
        int pos = layout.getOffset(VAOLayout.Usage.POSITION);
        int nrm = hasNormal ? layout.getOffset(VAOLayout.Usage.NORMAL) : -1;
        int start = startVert * stride;
        int stop = endVert * stride;

        if (m == null) {
            for (int i = start; i < stop; i++) {
                out.add(vbo[i]);
            }
            return;
        }

        Matrix3f normalMat = null;
        if (nrm != -1) {
            normalMat = new Matrix3f(
                    (float) m.m00, (float) m.m01, (float) m.m02,
                    (float) m.m10, (float) m.m11, (float) m.m12,
                    (float) m.m20, (float) m.m21, (float) m.m22);
            try {
                normalMat.invert();
            } catch (SingularMatrixException ignore) {
                //Nothing to do here
            }
            normalMat.transpose();
        }

        float[] vert = new float[stride];
        for (int i = start; i < stop; i += stride) {
            System.arraycopy(vbo, i, vert, 0, stride);

            Vector3f p = new Vector3f(vert[pos], vert[pos + 1], vert[pos + 2]);
            m.apply(p);
            vert[pos] = p.x;
            vert[pos + 1] = p.y;
            vert[pos + 2] = p.z;

            if (normalMat != null) {
                Vector3f n = new Vector3f(vert[nrm], vert[nrm + 1], vert[nrm + 2]);
                normalMat.transform(n);
                n.normalize();
                vert[nrm] = n.x;
                vert[nrm + 1] = n.y;
                vert[nrm + 2] = n.z;
            }

            for (int k = 0; k < stride; k++) {
                out.add(vert[k]);
            }
        }
    }

    public Model build() {
        float[] vbo = model.getVboData();
        Buffers.FloatBuffer out = new Buffers.FloatBuffer(vbo.length);
        for (Action action : actions) {
            action.add(vbo, out);
        }
        float[] data = out.array();

        Identifier loc = new Identifier(model.location().getDomain(), model.location().getPath() + "_build" + nextId.getAndIncrement());
        Model result = new GeneratedModel(model, loc, () -> data);
        result.linkTextures(model.getTextures(), model.getSpeculars(), model.getNormals());
        return result;
    }

    @FunctionalInterface
    private interface Action {
        void add(float[] vbo, Buffers.FloatBuffer out);
    }
}
