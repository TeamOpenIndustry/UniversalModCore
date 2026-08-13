package cam72cam.mod.model.common.mesh;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.common.ModelRenderer;
import cam72cam.mod.render.obj.OBJTextureSheet;
import cam72cam.mod.resource.Identifier;

import java.util.*;

public class Model {
    private final Identifier location;
    private final VAOLayout layout;
    private final float[] vboData;
    private final LinkedHashMap<String, ModelGroup> groups;

    public final boolean hasSpecular;
    public final boolean hasNormal;
    public final boolean isSmoothShading;

    private final Map<String, Map<Integer, OBJTextureSheet>> texture = new HashMap<>();
    private final Map<String, Map<Integer, OBJTextureSheet>> specular = new HashMap<>();
    private final Map<String, Map<Integer, OBJTextureSheet>> normal = new HashMap<>();

    public Model(Identifier location, VAOLayout layout, float[] vboData, LinkedHashMap<String, ModelGroup> groups,
                 boolean hasSpecular, boolean hasNormal, boolean isSmoothShading) {
        this.location = location;
        this.layout = layout;
        this.vboData = vboData;
        this.groups = groups;
        this.hasSpecular = hasSpecular;
        this.hasNormal = hasNormal;
        this.isSmoothShading = isSmoothShading;
    }

    public void linkTextures(Map<String, Map<Integer, OBJTextureSheet>> tex, Map<String, Map<Integer, OBJTextureSheet>> spec, Map<String, Map<Integer, OBJTextureSheet>> norm) {
        this.texture.putAll(tex);
        this.specular.putAll(spec);
        this.normal.putAll(norm);
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

    public double heightOfGroups(Iterable<String> groupNames) {
        Vec3d min = minOfGroups(groupNames);
        Vec3d max = maxOfGroups(groupNames);
        return max.y - min.y;
    }

    public double widthOfGroups(Iterable<String> groupNames) {
        Vec3d min = minOfGroups(groupNames);
        Vec3d max = maxOfGroups(groupNames);
        return max.z - min.z;
    }

    /** WARNING This is a very slow function and should be used for debug only */
    public List<Vec3d> points(ModelGroup group) {
        List<Vec3d> points = new ArrayList<>();
        for (int face = group.faceStart; face <= group.faceEnd; face++) {
            for (int point = 0; point < 3; point++) {
                int idx = (face * 3 + point) * layout.getStride() + layout.getOffset(VAOLayout.Usage.POSITION);
                points.add(new Vec3d(vboData[idx], vboData[idx+1], vboData[idx+2]));
            }
        }
        return points;
    }

    public Identifier location() {
        return location;
    }

    public VAOLayout getLayout() {
        return layout;
    }

    public float[] getVboData() {
        return vboData;
    }

    public Map<String, Map<Integer, OBJTextureSheet>> getTextures() {
        return texture;
    }

    public Map<String, Map<Integer, OBJTextureSheet>> getSpeculars() {
        return specular;
    }

    public Map<String, Map<Integer, OBJTextureSheet>> getNormals() {
        return normal;
    }

    public void free() {
        for (Map<Integer, OBJTextureSheet> lodMap : texture.values()) {
            for (OBJTextureSheet texture : lodMap.values()) {
                texture.dealloc();
            }
        }
        for (Map<Integer, OBJTextureSheet> lodMap : specular.values()) {
            for (OBJTextureSheet texture : lodMap.values()) {
                texture.dealloc();
            }
        }
        for (Map<Integer, OBJTextureSheet> lodMap : normal.values()) {
            for (OBJTextureSheet texture : lodMap.values()) {
                texture.dealloc();
            }
        }
        ModelRenderer.getRendererFor(this).free();
    }
}
