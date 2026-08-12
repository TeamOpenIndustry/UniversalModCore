package cam72cam.mod.model.common.mesh;

import cam72cam.mod.render.obj.OBJTextureSheet;
import cam72cam.mod.resource.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Model {
    private final Identifier location;
    private final VAOLayout layout;
    private final float[] vboData;
    private final List<ModelGroup> groups;

    private final Map<String, Map<Integer, OBJTextureSheet>> texture = new HashMap<>();
    private final Map<String, Map<Integer, OBJTextureSheet>> specular = new HashMap<>();
    private final Map<String, Map<Integer, OBJTextureSheet>> normal = new HashMap<>();

    public Model(Identifier location, VAOLayout layout, float[] vboData, List<ModelGroup> groups) {
        this.location = location;
        this.layout = layout;
        this.vboData = vboData;
        this.groups = groups;
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

    public float[] getVboData() {
        return vboData;
    }

    public List<ModelGroup> getGroups() {
        return groups;
    }
}
