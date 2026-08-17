package cam72cam.mod.model.common.mesh;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.common.material.Material;
import cam72cam.mod.model.common.material.TextureRepacker;
import cam72cam.mod.model.common.util.FaceUtils;
import cam72cam.mod.model.common.util.Buffers;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.serialization.ResourceCache;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArraySet;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public class SimpleModelBuilder implements IModelBuilder {
    private final Identifier modelLoc;
    private final Set<String> variants;

    // 3 floats per vertex (x, y, z)
    private final Buffers.FloatBuffer posIndices = new Buffers.FloatBuffer(1024);
    // 2 floats per uv (u, v)
    private Buffers.FloatBuffer uvIndices = new Buffers.FloatBuffer(1024);
    // 3 floats per normal (x, y, z)
    private final Buffers.FloatBuffer normIndices = new Buffers.FloatBuffer(1024);
    private final float scale;
    private final ResourceCache.ResourceProvider input;

    // 3 ints per vertex, 3 verts per triangle (posIdx, uvIdx, normIdx) x3
    private Buffers.IntBuffer faceBuffer = new Buffers.IntBuffer(1024);
    private Buffers.IntBuffer materialByFace = new Buffers.IntBuffer(1024);

    private final List<Material> materials = new ArrayList<>();
    private final Map<String, Integer> materialIds = new HashMap<>();
    private final Set<Integer> usedMaterials = new IntArraySet();
    private int currMaterial;
    private TextureRepacker repacker;

    private final List<String> groupNames = new ArrayList<>();
    private final List<Integer> groupStartFaces = new IntArrayList();
    private boolean smoothShading;

    private final LinkedHashMap<String, ModelGroup> groups = new LinkedHashMap<>();

    private boolean hasNormal = true;
    private boolean finished = false;

    public SimpleModelBuilder(Identifier modelLoc, float scale, Collection<String> variants, ResourceCache.ResourceProvider input) {
        this.modelLoc = modelLoc;
        this.scale = scale;
        this.input = input;
        if (variants == null || variants.isEmpty()) {
            this.variants = new HashSet<>(Collections.singleton(""));
        } else {
            this.variants = new HashSet<>(variants);
        }
        // Record the model file's hash so cache invalidation notices source edits.
        input.apply(modelLoc);
        // Faces without an explicit usemtl resolve to the default material
        materials.add(new Material(this, "undefined"));
        materialIds.put("undefined", 0);
        currMaterial = 0;
        //Add default group
        newModelGroup("defaultName");
    }

    @Override
    public void newModelGroup(String name) {
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
        return new SimpleFaceBuilder();
    }

    @Override
    public void doSmoothShading() {
        checkUnfinished();
        this.smoothShading = true;
    }

    @Override
    public Collection<ModelGroup> validGroups() {
        checkFinished();
        return groups.values();
    }

    @Override
    public boolean isSmoothShading() {
        checkFinished();
        return smoothShading;
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public void finish() {
        checkUnfinished();

        int triCount = faceBuffer.size() / 9;

        // Reorder faces by group name so that vertex order matches name order
        {
            Integer[] order = new Integer[groupNames.size()];
            for (int i = 0; i < order.length; i++) {
                order[i] = i;
            }
            Arrays.sort(order, (a, b) -> groupNames.get(a).compareTo(groupNames.get(b)));

            int[] srcFaces = faceBuffer.array();
            int[] srcMat = materialByFace.array();
            int[] sortedFaces = new int[srcFaces.length];
            int[] sortedMat = new int[srcMat.length];
            List<String> sortedNames = new ArrayList<>(groupNames.size());
            List<Integer> sortedStarts = new IntArrayList(groupNames.size());
            int cursor = 0;
            for (int gi : order) {
                int start = groupStartFaces.get(gi);
                int end = gi + 1 < groupStartFaces.size() ? groupStartFaces.get(gi + 1) : triCount;
                int triLen = end - start;
                System.arraycopy(srcFaces, start * 9, sortedFaces, cursor * 9, triLen * 9);
                System.arraycopy(srcMat, start, sortedMat, cursor, triLen);
                sortedNames.add(groupNames.get(gi));
                sortedStarts.add(cursor);
                cursor += triLen;
            }
            faceBuffer = new Buffers.IntBuffer(sortedFaces);
            materialByFace = new Buffers.IntBuffer(sortedMat);
            groupNames.clear();
            groupNames.addAll(sortedNames);
            groupStartFaces.clear();
            groupStartFaces.addAll(sortedStarts);
        }

        // Build groups data
        for (int i = 0; i < groupNames.size(); i++) {
            int start = groupStartFaces.get(i);
            int end = (i + 1 < groupStartFaces.size() ? groupStartFaces.get(i + 1) : triCount) - 1;

            if (start > end) {
                //Skip empty groups
                continue;
            }

            boolean[] usedVerts = new boolean[posIndices.size() / 3];
            List<Vec3d> points = new ArrayList<>();
            for (int tri = start; tri <= end; tri++) {
                int idx = tri * 9;

                // De-duplicated vertex data for group building
                for (int k = 0; k < 3; k++) {
                    int posIdx = faceBuffer.get(idx + k * 3);
                    if (!usedVerts[posIdx]) {
                        usedVerts[posIdx] = true;
                        points.add(new Vec3d(posIndices.get(posIdx * 3), posIndices.get(posIdx * 3 + 1), posIndices.get(posIdx * 3 + 2)));
                    }
                }

                // Build UV repacking data. Untextured materials are included too: their
                // UV tiling still determines the atlas tile count for the solid fallback.
                {
                    Material mat = materials.get(materialByFace.get(tri));
                    int u0 = faceBuffer.get(idx + 1);
                    int u1 = faceBuffer.get(idx + 4);
                    int u2 = faceBuffer.get(idx + 7);
                    if (u0 < 0 || u1 < 0 || u2 < 0) {
                        continue;
                    }
                    float vminU = Math.min(uvIndices.get(u0 * 2), Math.min(uvIndices.get(u1 * 2), uvIndices.get(u2 * 2)));
                    float vmaxU = Math.max(uvIndices.get(u0 * 2), Math.max(uvIndices.get(u1 * 2), uvIndices.get(u2 * 2)));
                    float vminV = Math.min(uvIndices.get(u0 * 2 + 1), Math.min(uvIndices.get(u1 * 2 + 1), uvIndices.get(u2 * 2 + 1)));
                    float vmaxV = Math.max(uvIndices.get(u0 * 2 + 1), Math.max(uvIndices.get(u1 * 2 + 1), uvIndices.get(u2 * 2 + 1)));
                    int offU = (int) Math.floor(vminU);
                    int offV = (int) Math.floor(vminV);
                    mat.copiesOnU = Math.max(mat.copiesOnU, (int) Math.ceil(vmaxU - offU));
                    mat.copiesOnV = Math.max(mat.copiesOnV, (int) Math.ceil(vmaxV - offV));
                }
            }
            groups.put(groupNames.get(i), ModelGroup.buildGroup(groupNames.get(i), start, end, points));
        }

        // Repack textures and rebuild the uv index space with the converted coordinates
        Set<Material> used = usedMaterials.stream().map(materials::get).collect(Collectors.toSet());
        repacker = new TextureRepacker(this, used, variants);

        //Avoid empty base buffers
        Buffers.IntBuffer repackedFaces = new Buffers.IntBuffer(faceBuffer.size() + 1);
        Buffers.FloatBuffer repackedUv =  new Buffers.FloatBuffer(uvIndices.size() + 1);
        for (int tri = 0; tri < triCount; tri++) {
            int b = tri * 9;
            Material mat = materials.get(materialByFace.get(tri));
            TextureRepacker.UVConverter converter = repacker.converters.get(mat.name);
            int offU = 0;
            int offV = 0;
            int u0 = faceBuffer.get(b + 1);
            int u1 = faceBuffer.get(b + 4);
            int u2 = faceBuffer.get(b + 7);
            if (u0 >= 0 && u1 >= 0 && u2 >= 0) {
                offU = (int) Math.floor(Math.min(uvIndices.get(u0 * 2), Math.min(uvIndices.get(u1 * 2), uvIndices.get(u2 * 2))));
                offV = (int) Math.floor(Math.min(uvIndices.get(u0 * 2 + 1), Math.min(uvIndices.get(u1 * 2 + 1), uvIndices.get(u2 * 2 + 1))));
            }
            for (int k = 0; k < 3; k++) {
                int posIdx = faceBuffer.get(b + k * 3);
                int uvIdx = faceBuffer.get(b + k * 3 + 1);
                int nrmIdx = faceBuffer.get(b + k * 3 + 2);
                // Always re-emit the uv into the repacked buffer so the index stays valid
                // For vertices without UV use 0.5 as fallback
                float u = uvIdx >= 0 ? uvIndices.get(uvIdx * 2) : 0.5f;
                float v = uvIdx >= 0 ? uvIndices.get(uvIdx * 2 + 1) : 0.5f;
                // We have materials without texture drawn as white blocks so also repack them here
                u = converter.convertU(u - offU);
                v = converter.convertV(v - offV);
                uvIdx = repackedUv.size() / 2;
                repackedUv.add(u);
                repackedUv.add(v);
                repackedFaces.add(posIdx);
                repackedFaces.add(uvIdx);
                repackedFaces.add(nrmIdx);
            }
        }
        faceBuffer = repackedFaces;
        uvIndices = repackedUv;

        finished = true;
    }

    public Model build() {
        if (hasNormal) {
            return build(VAOLayout.POS_TEX_COLOR_NORMAL);
        }
        return build(VAOLayout.POS_TEX_COLOR);
    }

    @Override
    public Model build(VAOLayout layout) {
        checkFinished();

        int strideF = layout.getStride();
        int triCount = faceBuffer.size() / 9;
        float[] data = new float[triCount * 3 * strideF];

        int posOff = layout.getOffset(VAOLayout.Usage.POSITION);
        int uvOff = layout.getOffset(VAOLayout.Usage.UV);
        int colorOff = layout.getOffset(VAOLayout.Usage.COLOR);
        int normalOff = layout.getOffset(VAOLayout.Usage.NORMAL);

        for (int tri = 0; tri < triCount; tri++) {
            int b = tri * 9;
            int posIdx0 = faceBuffer.get(b);
            int posIdx1 = faceBuffer.get(b + 3);
            int posIdx2 = faceBuffer.get(b + 6);

            Vec3d pa = new Vec3d(posIndices.get(posIdx0 * 3), posIndices.get(posIdx0 * 3 + 1), posIndices.get(posIdx0 * 3 + 2));
            Vec3d pb = new Vec3d(posIndices.get(posIdx1 * 3), posIndices.get(posIdx1 * 3 + 1), posIndices.get(posIdx1 * 3 + 2));
            Vec3d pc = new Vec3d(posIndices.get(posIdx2 * 3), posIndices.get(posIdx2 * 3 + 1), posIndices.get(posIdx2 * 3 + 2));

            Material mat = materials.get(materialByFace.get(tri));

            for (int k = 0; k < 3; k++) {
                int v = tri * 3 + k;
                int base = v * strideF;
                Vec3d p = k == 0 ? pa : (k == 1 ? pb : pc);
                int uvIdx = faceBuffer.get(b + k * 3 + 1);
                int nrmIdx = faceBuffer.get(b + k * 3 + 2);

                data[base + posOff] = (float) p.x;
                data[base + posOff + 1] = (float) p.y;
                data[base + posOff + 2] = (float) p.z;

                if (uvOff != -1) {
                    if (uvIdx >= 0) {
                        data[base + uvOff] = uvIndices.get(uvIdx * 2);
                        data[base + uvOff + 1] = uvIndices.get(uvIdx * 2 + 1);
                    }
                }

                if (colorOff != -1) {
                    // Vertex color carries the material color; color-only materials draw a white
                    // albedo slot so the color shows through directly (no double-application)
                    data[base + colorOff] = mat.r;
                    data[base + colorOff + 1] = mat.g;
                    data[base + colorOff + 2] = mat.b;
                    data[base + colorOff + 3] = mat.a;
                }

                if (normalOff != -1) {
                    if (nrmIdx >= 0) {
                        data[base + normalOff] = normIndices.get(nrmIdx * 3);
                        data[base + normalOff + 1] = normIndices.get(nrmIdx * 3 + 1);
                        data[base + normalOff + 2] = normIndices.get(nrmIdx * 3 + 2);
                    } else {
                        Vec3d normal = pb.subtract(pa).crossProduct(pc.subtract(pa)).normalize();
                        data[base + normalOff] = (float) normal.x;
                        data[base + normalOff + 1] = (float) normal.y;
                        data[base + normalOff + 2] = (float) normal.z;
                    }
                }
            }
        }

        return new Model(modelLoc, layout, () -> data, groups, repacker.hasSpecular(), repacker.hasNormal(), smoothShading, repacker.getWidth(), repacker.getHeight());
    }

    @Override
    public Identifier getModelLoc() {
        return modelLoc;
    }

    @Override
    public TextureRepacker getRepacker() {
        checkFinished();
        return repacker;
    }

    public boolean hasNormal() {
        return hasNormal;
    }

    @Override
    public InputStream open(Identifier id) {
        // Via cache
        return new ByteArrayInputStream(input.apply(id));
    }

    public class SimpleFaceBuilder implements IFaceBuilder {
        private final Buffers.IntBuffer buffer = new Buffers.IntBuffer(16);

        @Override
        public IFaceBuilder addVert(int posIdx, int uvIdx, int normalIdx) {
            if (posIdx == -1) {
                throw new IllegalArgumentException("Unable to read model geometry data because vertex position is invalid!");
            } else if (normalIdx == -1) {
                SimpleModelBuilder.this.hasNormal = false;
            }
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

            // materialByFace is one entry per triangle, so add it once per 3 vertices
            for (int t = 0; t < indices.length; t += 3) {
                for (int k = 0; k < 3; k++) {
                    int i = indices[t + k];
                    faceBuffer.add(buffer.get(i * 3));
                    faceBuffer.add(buffer.get(i * 3 + 1));
                    faceBuffer.add(buffer.get(i * 3 + 2));
                }
                materialByFace.add(currMaterial);
                usedMaterials.add(currMaterial);
            }
        }
    }
}
