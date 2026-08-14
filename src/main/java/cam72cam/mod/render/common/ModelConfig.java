package cam72cam.mod.render.common;

import cam72cam.mod.model.common.mesh.Model;
import cam72cam.mod.render.obj.OBJTextureSheet;
import cam72cam.mod.render.opengl.RenderState;

import java.util.Map;
import java.util.NavigableMap;

/** Per-draw texture/variant/LOD config. Built via of().lod().variant(), applied by ConfiguredRenderer. */
public class ModelConfig {
    private int lod = -1;
    private String variant = "";

    public static ModelConfig of() {
        return new ModelConfig();
    }

    public void lod(int lod) {
        this.lod = lod;
    }

    public void variant(String variant) {
        this.variant = variant;
    }

    void apply(RenderState state, Model model) {
        OBJTextureSheet albedo = pickLodSheet(model.getTextures());
        if (albedo != null) {
            state.texture(albedo);
        }
        if (model.hasSpecular) {
            OBJTextureSheet spec = pickLodSheet(model.getSpeculars());
            if (spec != null) {
                state.specular(spec);
            }
        }
        if (model.hasNormal) {
            OBJTextureSheet norm = pickLodSheet(model.getNormals());
            if (norm != null) {
                state.normals(norm);
            }
        }
        state.smooth_shading(model.isSmoothShading);
    }

    private OBJTextureSheet pickLodSheet(Map<String, NavigableMap<Integer, OBJTextureSheet>> variants) {
        NavigableMap<Integer, OBJTextureSheet> map = variants.get(variant);
        NavigableMap<Integer, OBJTextureSheet> lodMap = map != null ? map : variants.get("");
        if (lodMap == null || lodMap.isEmpty()) {
            return null;
        }
        if (lod <= 0) {
            return lodMap.lastEntry().getValue();
        }
        Map.Entry<Integer, OBJTextureSheet> entry = lodMap.floorEntry(lod);
        if (entry == null) {
            entry = lodMap.firstEntry();
        }
        return entry.getValue();
    }
}
