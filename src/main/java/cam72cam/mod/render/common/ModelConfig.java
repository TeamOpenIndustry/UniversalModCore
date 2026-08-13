package cam72cam.mod.render.common;

import cam72cam.mod.model.common.mesh.Model;
import cam72cam.mod.render.obj.OBJTextureSheet;
import cam72cam.mod.render.opengl.RenderState;

import java.util.Collections;
import java.util.Map;

/** Per-draw texture/variant/LOD config. Built via of().lod().variant(), applied by ConfiguredRenderer. */
public class ModelConfig {
    private int lod;
    private String variant;

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
        OBJTextureSheet albedo = pickSheet(model.getTextures());
        if (albedo != null) {
            state.texture(albedo);
        }
        if (model.hasSpecular) {
            OBJTextureSheet spec = pickSheet(model.getSpeculars());
            if (spec != null) {
                state.specular(spec);
            }
        }
        if (model.hasNormal) {
            OBJTextureSheet norm = pickSheet(model.getNormals());
            if (norm != null) {
                state.normals(norm);
            }
        }
        state.smooth_shading(model.isSmoothShading);
    }

    private OBJTextureSheet pickSheet(Map<String, Map<Integer, OBJTextureSheet>> variants) {
        Map<Integer, OBJTextureSheet> map = variants.get(variant);
        Map<Integer, OBJTextureSheet> lodMap = map != null ? map : variants.get("");
        if (lodMap == null || lodMap.isEmpty()) {
            return null;
        }
        if (lod <= 0) {
            return lodMap.get(Collections.max(lodMap.keySet()));
        }
        Integer best = null;
        for (Integer size : lodMap.keySet()) {
            if (size <= lod && (best == null || size > best)) {
                best = size;
            }
        }
        return lodMap.get(best != null ? best : Collections.min(lodMap.keySet()));
    }
}
