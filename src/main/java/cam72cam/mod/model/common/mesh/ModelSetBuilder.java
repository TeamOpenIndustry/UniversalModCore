package cam72cam.mod.model.common.mesh;

import cam72cam.mod.model.common.util.Buffers;
import cam72cam.mod.resource.Identifier;
import util.Matrix4;

import javax.vecmath.Matrix3f;
import javax.vecmath.SingularMatrixException;
import javax.vecmath.Vector3f;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Bakes a set of transformed copies of a source {@link Model} (or selected groups) into a
 * single drawable model.<br>
 *
 * The result of{@link #build()} is a {@link GeneratedModel} which shares the source model's texture sheets.
 */
public class ModelSetBuilder {
    private static final AtomicInteger nextId = new AtomicInteger(0);

    private final Model model;
    private final VAOLayout layout;
    private final List<Action> actions = new ArrayList<>();

    public static ModelSetBuilder of(Model model) {
        return new ModelSetBuilder(model);
    }

    private ModelSetBuilder(Model model) {
        this.model = model;
        this.layout = model.getLayout();
    }

    /**
     * Appends the entire model transformed by <code>m</code>.
     * @param m The transform to apply, or <code>null</code> to append untransformed
     * @return This builder
     */
    public ModelSetBuilder append(Matrix4 m) {
        actions.add((vbo, out) -> add(out, vbo, 0, vbo.length / layout.getStride(), m));
        return this;
    }

    /**
     * Appends the enumerated groups, transformed by <code>m</code>.
     * @param groups Group names to append
     * @param m      The transform to apply, or {@code null} to append untransformed
     * @return This builder
     */
    public ModelSetBuilder append(Collection<String> groups, Matrix4 m) {
        actions.add((vbo, out) -> {
            for (String name : groups) {
                ModelGroup group = model.getGroups().get(name);
                add(out, vbo, group.faceStart * 3, (group.faceEnd + 1) * 3, m);
            }
        });
        return this;
    }

    private void add(Buffers.FloatBuffer out, float[] vbo, int startVert, int endVert, Matrix4 m) {
        int stride = layout.getStride();
        int pos = layout.getOffset(VAOLayout.Usage.POSITION);
        int nrm = layout.getOffset(VAOLayout.Usage.NORMAL);
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

    /**
     * Build the transformed model into one {@link GeneratedModel}.
     * @return A {@link GeneratedModel} which has a new VBO and shares the source model's texture sheets
     */
    public GeneratedModel build() {
        float[] vbo = model.getVboData();
        Buffers.FloatBuffer out = new Buffers.FloatBuffer(vbo.length);
        for (Action action : actions) {
            action.add(vbo, out);
        }
        float[] data = out.array();

        Identifier loc = new Identifier(model.location().getDomain(), model.location().getPath() + "_build" + nextId.getAndIncrement());
        return new GeneratedModel(model, loc, () -> data);
    }

    @FunctionalInterface
    private interface Action {
        void add(float[] vbo, Buffers.FloatBuffer out);
    }
}
