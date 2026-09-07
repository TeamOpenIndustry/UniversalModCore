package cam72cam.mod.model.common.mesh;

import cam72cam.mod.resource.Identifier;
import util.Matrix4;

import javax.vecmath.Matrix3f;
import javax.vecmath.SingularMatrixException;
import javax.vecmath.Vector3f;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Bakes a set of transformed copies of a source {@link Model} (or selected groups) into a
 * single drawable model.<br>
 *
 * Faces are accumulated per group name, so the resulting {@link GeneratedModel} carries real
 * group data pointing into its own VBO; group bounds (min/max) are computed lazily on first
 * access via {@link ModelGroup#lazy}. The result shares the source model's texture sheets.
 */
public class ModelSetBuilder {
    private static final AtomicInteger nextId = new AtomicInteger(0);

    private final Model base;
    private final VAOLayout layout;
    private final int stride;
    private final int pos;
    private final int norm;

    private final TreeMap<String, List<Matrix4>> actions = new TreeMap<>(Comparator.naturalOrder());
    private final IdentityHashMap<Matrix4, Matrix3f> normalMats = new IdentityHashMap<>();
    private final Vector3f positionGetter = new Vector3f();
    private final Vector3f normalGetter = new Vector3f();
    private int faceCount;

    public static ModelSetBuilder of(Model model) {
        return new ModelSetBuilder(model);
    }

    private ModelSetBuilder(Model base) {
        this.base = base;
        this.layout = base.getLayout();
        this.stride = layout.getStride();
        this.pos = layout.getOffset(VAOLayout.Usage.POSITION);
        this.norm = layout.getOffset(VAOLayout.Usage.NORMAL);
    }

    /**
     * Appends every group of the model, transformed by <code>m</code>.
     * @param m The transform to apply, or <code>null</code> to append untransformed
     * @return This builder
     */
    public ModelSetBuilder append(Matrix4 m) {
        if (m != null) {
            m = m.copy();
        }
        for (Map.Entry<String, ModelGroup> name : base.getGroups().entrySet()) {
            actions.computeIfAbsent(name.getKey(), k -> new ArrayList<>()).add(m);
            faceCount += name.getValue().faceEnd - name.getValue().faceStart + 1;
        }
        return this;
    }

    /**
     * Appends the enumerated groups, transformed by <code>m</code>.
     * @param groups Group names to append
     * @param m      The transform to apply, or {@code null} to append untransformed
     * @return This builder
     */
    public ModelSetBuilder append(Collection<String> groups, Matrix4 m) {
        if (m != null) {
            m = m.copy();
        }
        for (String name : groups) {
            ModelGroup group = base.getGroups().get(name);
            if (group == null) {
                throw new IllegalArgumentException(name + " is not a valid group for model" + base.location());
            }
            actions.computeIfAbsent(name, k -> new ArrayList<>()).add(m);
            faceCount += group.faceEnd - group.faceStart + 1;
        }
        return this;
    }

    public GeneratedModel build() {
        float[] srcData = base.getVboData();
        float[] dstData = new float[faceCount * 3 * stride];
        int faceStride = stride * 3;

        LinkedHashMap<String, ModelGroup> groups = new LinkedHashMap<>(actions.size());
        int dstCursor = 0;
        for (Map.Entry<String, List<Matrix4>> entry : actions.entrySet()) {
            String name = entry.getKey();
            ModelGroup group = base.getGroups().get(name);
            int length = (group.faceEnd - group.faceStart + 1) * 3 * stride;
            int faceStart = dstCursor / faceStride;
            for (Matrix4 m : entry.getValue()) {
                dstCursor = copyAndTransform(srcData, group.faceStart * 3 * stride, dstData, dstCursor, length, m);
            }
            int faceEnd = (dstCursor / faceStride) - 1;
            if (faceEnd - faceStart >= 0) {
                // The group is not empty
                groups.put(name, ModelGroup.lazy(name, faceStart, faceEnd, dstData, layout));
            }
        }

        Identifier loc = new Identifier(base.location().getDomain(), base.location().getPath() + "_build" + nextId.getAndIncrement());
        return new GeneratedModel(base, loc, () -> dstData, groups);
    }

    private int copyAndTransform(float[] source, int src, float[] target, int dst, int len, Matrix4 transform) {
        System.arraycopy(source, src, target, dst, len);
        if (transform != null) {
            Matrix3f normalMat = null;
            if (norm != -1) {
                normalMat = normalMats.get(transform);
                if (normalMat == null) {
                    normalMat = new Matrix3f(
                            (float) transform.m00, (float) transform.m01, (float) transform.m02,
                            (float) transform.m10, (float) transform.m11, (float) transform.m12,
                            (float) transform.m20, (float) transform.m21, (float) transform.m22);
                    try {
                        normalMat.invert();
                    } catch (SingularMatrixException ignore) {
                        //Nothing to do here
                    }
                    normalMat.transpose();
                    normalMats.put(transform, normalMat);
                }
            }

            for (int i = dst; i < dst + len; i += stride) {
                positionGetter.set(target[i + this.pos], target[i + this.pos + 1], target[i + this.pos + 2]);
                transform.apply(positionGetter);
                target[i + this.pos] = positionGetter.x;
                target[i + this.pos + 1] = positionGetter.y;
                target[i + this.pos + 2] = positionGetter.z;

                if (normalMat != null) {
                    normalGetter.set(target[i + this.norm], target[i + this.norm + 1], target[i + this.norm + 2]);
                    normalMat.transform(normalGetter);
                    normalGetter.normalize();
                    target[i + this.norm] = normalGetter.x;
                    target[i + this.norm + 1] = normalGetter.y;
                    target[i + this.norm + 2] = normalGetter.z;
                }
            }
        }
        return dst + len;
    }
}
