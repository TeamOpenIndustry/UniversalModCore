package cam72cam.mod.model.common.mesh;

import java.util.List;

public class Model {
    private final VAOLayout layout;
    private final float[] vboData;
    private final List<ModelGroup> groups;

    public Model(VAOLayout layout, float[] vboData, List<ModelGroup> groups) {
        this.layout = layout;
        this.vboData = vboData;
        this.groups = groups;
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
