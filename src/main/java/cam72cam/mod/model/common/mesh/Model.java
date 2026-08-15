package cam72cam.mod.model.common.mesh;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.common.util.FaceAccessor;
import cam72cam.mod.render.common.ModelRenderer;
import cam72cam.mod.render.obj.OBJTextureSheet;
import cam72cam.mod.resource.Identifier;

import java.util.*;
import java.util.function.Supplier;

public class Model {
    private final Identifier location;
    private final VAOLayout layout;
    private final Supplier<float[]> vboSupplier;
    private float[] vboData = null;
    private final LinkedHashMap<String, ModelGroup> groups;

    public final boolean hasSpecular;
    public final boolean hasNormal;
    public final boolean isSmoothShading;

    public final int packedTextureWidth;
    public final int packedTextureHeight;
    private int defaultLodSize;

    private final Map<String, NavigableMap<Integer, OBJTextureSheet>> textures = new HashMap<>();
    private final Map<String, NavigableMap<Integer, OBJTextureSheet>> speculars = new HashMap<>();
    private final Map<String, NavigableMap<Integer, OBJTextureSheet>> normals = new HashMap<>();

    public String modelHash;

    public Model(Identifier location, VAOLayout layout, Supplier<float[]> vboSupplier, LinkedHashMap<String, ModelGroup> groups,
                 boolean hasSpecular, boolean hasNormal, boolean isSmoothShading,
                 int packedTextureWidth, int packedTextureHeight) {
        this.location = location;
        this.layout = layout;
        this.vboSupplier = vboSupplier;
        this.groups = groups;
        this.hasSpecular = hasSpecular;
        this.hasNormal = hasNormal;
        this.isSmoothShading = isSmoothShading;
        this.packedTextureWidth = packedTextureWidth;
        this.packedTextureHeight = packedTextureHeight;
    }

    public void linkTextures(Map<String, NavigableMap<Integer, OBJTextureSheet>> tex,
                             Map<String, NavigableMap<Integer, OBJTextureSheet>> spec,
                             Map<String, NavigableMap<Integer, OBJTextureSheet>> norm) {
        this.textures.putAll(tex);
        this.speculars.putAll(spec);
        this.normals.putAll(norm);
        defaultLodSize = textures.get("").keySet().stream().mapToInt(i -> i).max().orElse(-1);
    }

    // ModelGroup helpers

    public LinkedHashMap<String, ModelGroup> getGroups() {
        return groups;
    }

    public Set<String> groups() {
        return groups.keySet();
    }

    public Vec3d minOfGroups(Iterable<String> groupNames) {
        Vec3d min = null;
        for (String group : groupNames) {
            Vec3d gmin = groups.get(group).min;
            if (min == null) {
                min = gmin;
            } else {
                min = min.min(gmin);
            }
        }
        return min;
    }

    public Vec3d maxOfGroups(Iterable<String> groupNames) {
        Vec3d max = null;
        for (String group : groupNames) {
            Vec3d gmax = groups.get(group).max;
            if (max == null) {
                max = gmax;
            } else {
                max = max.max(gmax);
            }
        }
        return max;
    }

    public Vec3d centerOfGroups(Iterable<String> groupNames) {
        Vec3d min = minOfGroups(groupNames);
        Vec3d max = maxOfGroups(groupNames);
        return new Vec3d((min.x + max.x) / 2, (min.y + max.y) / 2, (min.z + max.z) / 2);
    }

    public double lengthOfGroups(Iterable<String> groupNames) {
        Vec3d min = minOfGroups(groupNames);
        Vec3d max = maxOfGroups(groupNames);
        return max.x - min.x;
    }

    public double widthOfGroups(Iterable<String> groupNames) {
        Vec3d min = minOfGroups(groupNames);
        Vec3d max = maxOfGroups(groupNames);
        return max.z - min.z;
    }

    public double heightOfGroups(Iterable<String> groupNames) {
        Vec3d min = minOfGroups(groupNames);
        Vec3d max = maxOfGroups(groupNames);
        return max.y - min.y;
    }

    /** WARNING This is a very slow function and should be used for debug only */
    public List<Vec3d> points(ModelGroup group) {
        getVboData(); //Populate
        List<Vec3d> points = new ArrayList<>();
        for (int face = group.faceStart; face <= group.faceEnd; face++) {
            for (int point = 0; point < 3; point++) {
                int idx = (face * 3 + point) * layout.getStride() + layout.getOffset(VAOLayout.Usage.POSITION);
                points.add(new Vec3d(vboData[idx], vboData[idx+1], vboData[idx+2]));
            }
        }
        return points;
    }

    public FaceAccessor getFaceAccessor() {
        return new FaceAccessor(this);
    }

    public Identifier location() {
        return location;
    }

    public VAOLayout getLayout() {
        return layout;
    }

    public float[] getVboData() {
        if (vboData == null) {
            vboData = vboSupplier.get();
        }
        return vboData;
    }

    public Map<String, NavigableMap<Integer, OBJTextureSheet>> getTextures() {
        return textures;
    }

    public Map<String, NavigableMap<Integer, OBJTextureSheet>> getSpeculars() {
        return speculars;
    }

    public Map<String, NavigableMap<Integer, OBJTextureSheet>> getNormals() {
        return normals;
    }

    public int getDefaultLodSize() {
        return defaultLodSize;
    }

    public void free() {
        for (Map<Integer, OBJTextureSheet> lodMap : textures.values()) {
            for (OBJTextureSheet texture : lodMap.values()) {
                texture.dealloc();
            }
        }
        for (Map<Integer, OBJTextureSheet> lodMap : speculars.values()) {
            for (OBJTextureSheet texture : lodMap.values()) {
                texture.dealloc();
            }
        }
        for (Map<Integer, OBJTextureSheet> lodMap : normals.values()) {
            for (OBJTextureSheet texture : lodMap.values()) {
                texture.dealloc();
            }
        }
        ModelRenderer.getRendererFor(this).free();
    }
}
