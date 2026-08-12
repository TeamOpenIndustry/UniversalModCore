package cam72cam.mod.model.common.mesh;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.common.material.Material;
import cam72cam.mod.model.common.util.FaceUtils;
import cam72cam.mod.model.common.util.Buffers;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArraySet;

import java.util.*;
import java.util.stream.Collectors;

public class GlModelBuilder implements IModelBuilder {

    private final Buffers.FloatBuffer posIndices = new Buffers.FloatBuffer(1024);
    private final Buffers.FloatBuffer uvIndices = new Buffers.FloatBuffer(1024);
    private final Buffers.FloatBuffer normIndices = new Buffers.FloatBuffer(1024);
    private final float scale;
    private final Set<String> variants;

    // 3 ints per vertex, 3 verts per triangle (posIdx, uvIdx, normIdx) x3
    private final Buffers.IntBuffer faceBuffer = new Buffers.IntBuffer(1024);
    private final Buffers.IntBuffer materialByFace = new Buffers.IntBuffer(1024);

    private final List<Material> materials = new ArrayList<>();
    private final Map<String, Integer> materialIds = new HashMap<>();
    private final Set<Integer> usedMaterials = new IntArraySet();
    private int currMaterial = -1;

    private final List<String> groupNames = new ArrayList<>();
    private final List<Integer> groupStartFaces = new IntArrayList();
    private boolean smoothShading;

    private final List<ModelGroup> groups = new ArrayList<>();

    private boolean finished = false;

    public GlModelBuilder(float scale, Set<String> variants) {
        this.scale = scale;
        if (variants == null) {
            variants = Collections.singleton("");
        }
        if (variants.isEmpty()) {
            variants.add("");
        }
        this.variants = variants;
    }

    @Override
    public void newModelGroup(String name) {
        checkUnfinished();
        groupNames.add(name);
        groupStartFaces.add(faceBuffer.size() / 9);
    }

    @Override
    public void setCurrentMaterial(Material mat) {
        checkUnfinished();
        Integer id = materialIds.get(mat.name);
        if (id == null) {
            id = materials.size();
            materialIds.put(mat.name, id);
            materials.add(mat);
        }
        currMaterial = id;
    }

    @Override
    public int addIndexedVert(float x, float y, float z) {
        checkUnfinished();
        int index = posIndices.size() / 3;
        posIndices.add(x * scale);
        posIndices.add(y * scale);
        posIndices.add(z * scale);
        return index;
    }

    @Override
    public int addIndexedUv(float u, float v) {
        checkUnfinished();
        int index = uvIndices.size() / 2;
        uvIndices.add(u);
        uvIndices.add(v);
        return index;
    }

    @Override
    public int addIndexedNormal(float nx, float ny, float nz) {
        checkUnfinished();
        int index = normIndices.size() / 3;
        normIndices.add(nx);
        normIndices.add(ny);
        normIndices.add(nz);
        return index;
    }

    @Override
    public IFaceBuilder newFace() {
        checkUnfinished();
        return new GlFaceBuilder();
    }

    @Override
    public void doSmoothShading() {
        checkUnfinished();
        this.smoothShading = true;
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public void finish() {
        // Build groups
        int triCount = faceBuffer.size() / 9;
        for (int i = 0; i < groupNames.size(); i++) {
            int start = groupStartFaces.get(i);
            int end = i + 1 < groupStartFaces.size() ? groupStartFaces.get(i + 1) : triCount;

            boolean[] usedVerts = new boolean[posIndices.size() / 3];
            List<Vec3d> points = new ArrayList<>();
            for (int tri = start; tri < end; tri++) {
                for (int k = 0; k < 3; k++) {
                    int posIdx = faceBuffer.get(tri * 9 + k * 3);
                    if (!usedVerts[posIdx]) {
                        usedVerts[posIdx] = true;
                        points.add(new Vec3d(posIndices.get(posIdx * 3), posIndices.get(posIdx * 3 + 1), posIndices.get(posIdx * 3 + 2)));
                    }
                }
            }

            if (!points.isEmpty()) {
                groups.add(ModelGroup.buildGroup(groupNames.get(i), start, end, points));
            }
        }
        finished = true;
    }

    public Model build(VAOLayout layout) {
        checkFinished();

        int strideF = layout.getStride() / 4;
        int triCount = faceBuffer.size() / 9;
        float[] data = new float[triCount * 3 * strideF];

        int posOff = layout.getOffset(VAOLayout.Usage.POSITION) / 4;
        boolean hasUv = layout.has(VAOLayout.Usage.UV);
        boolean hasColor = layout.has(VAOLayout.Usage.COLOR);
        boolean hasNormal = layout.has(VAOLayout.Usage.NORMAL);
        int uvOff = hasUv ? layout.getOffset(VAOLayout.Usage.UV) / 4 : 0;
        int colorOff = hasColor ? layout.getOffset(VAOLayout.Usage.COLOR) / 4 : 0;
        int nrmOff = hasNormal ? layout.getOffset(VAOLayout.Usage.NORMAL) / 4 : 0;

        for (int tri = 0; tri < triCount; tri++) {
            int b = tri * 9;
            int posIdx0 = faceBuffer.get(b);
            int posIdx1 = faceBuffer.get(b + 3);
            int posIdx2 = faceBuffer.get(b + 6);

            Vec3d pa = new Vec3d(posIndices.get(posIdx0 * 3), posIndices.get(posIdx0 * 3 + 1), posIndices.get(posIdx0 * 3 + 2));
            Vec3d pb = new Vec3d(posIndices.get(posIdx1 * 3), posIndices.get(posIdx1 * 3 + 1), posIndices.get(posIdx1 * 3 + 2));
            Vec3d pc = new Vec3d(posIndices.get(posIdx2 * 3), posIndices.get(posIdx2 * 3 + 1), posIndices.get(posIdx2 * 3 + 2));

            int matId = materialByFace.get(tri * 3);
            Material mat = matId >= 0 && matId < materials.size() ? materials.get(matId) : null;

            for (int k = 0; k < 3; k++) {
                int v = tri * 3 + k;
                int base = v * strideF;
                Vec3d p = k == 0 ? pa : (k == 1 ? pb : pc);
                int uvIdx = faceBuffer.get(b + k * 3 + 1);
                int nrmIdx = faceBuffer.get(b + k * 3 + 2);

                data[base + posOff] = (float) p.x;
                data[base + posOff + 1] = (float) p.y;
                data[base + posOff + 2] = (float) p.z;

                if (hasUv) {
                    if (uvIdx >= 0) {
                        data[base + uvOff] = uvIndices.get(uvIdx * 2);
                        data[base + uvOff + 1] = uvIndices.get(uvIdx * 2 + 1);
                    }
                }

                if (hasColor) {
                    data[base + colorOff] = mat != null ? mat.r : 1;
                    data[base + colorOff + 1] = mat != null ? mat.g : 1;
                    data[base + colorOff + 2] = mat != null ? mat.b : 1;
                    data[base + colorOff + 3] = mat != null ? mat.a : 1;
                }

                if (hasNormal) {
                    if (nrmIdx >= 0) {
                        data[base + nrmOff] = normIndices.get(nrmIdx * 3);
                        data[base + nrmOff + 1] = normIndices.get(nrmIdx * 3 + 1);
                        data[base + nrmOff + 2] = normIndices.get(nrmIdx * 3 + 2);
                    } else {
                        Vec3d normal = pb.subtract(pa).crossProduct(pc.subtract(pa)).normalize();
                        data[base + nrmOff] = (float) normal.x;
                        data[base + nrmOff + 1] = (float) normal.y;
                        data[base + nrmOff + 2] = (float) normal.z;
                    }
                }
            }
        }

        return new Model(layout, data, groups);
    }

    public class GlFaceBuilder implements IFaceBuilder {
        private final Buffers.IntBuffer buffer = new Buffers.IntBuffer(16);

        @Override
        public IFaceBuilder addVert(int posIdx, int uvIdx, int normalIdx) {
            buffer.add(posIdx);
            buffer.add(uvIdx);
            buffer.add(normalIdx);
            return this;
        }

        @Override
        public void end() {
            int vertCount = buffer.size() / 3;
            if (vertCount < 3) {
                return;
            }

            int[] indices;
            if (vertCount == 3) {
                indices = new int[]{0, 1, 2};
            } else {
                List<Vec3d> positions = new ArrayList<>(vertCount);
                for (int i = 0; i < buffer.size(); i += 3) {
                    int p = buffer.get(i);
                    positions.add(new Vec3d(posIndices.get(p * 3), posIndices.get(p * 3 + 1), posIndices.get(p * 3 + 2)));
                }
                indices = FaceUtils.triangulate(positions);
            }

            for (int i : indices) {
                faceBuffer.add(buffer.get(i * 3));
                faceBuffer.add(buffer.get(i * 3 + 1));
                faceBuffer.add(buffer.get(i * 3 + 2));
                materialByFace.add(currMaterial);
                usedMaterials.add(currMaterial);
            }
        }
    }
}
