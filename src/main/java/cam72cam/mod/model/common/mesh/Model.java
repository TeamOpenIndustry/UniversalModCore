package cam72cam.mod.model.common.mesh;

import cam72cam.mod.render.obj.OBJTextureSheet;
import cam72cam.mod.resource.Identifier;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

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

    public Identifier location() {
        return location;
    }

    public VAOLayout getLayout() {
        return layout;
    }

    public LinkedHashMap<String, ModelGroup> getGroups() {
        return groups;
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
}
