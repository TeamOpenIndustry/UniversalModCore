package cam72cam.mod.render.common;

import cam72cam.mod.model.common.mesh.Model;
import cam72cam.mod.model.obj.OBJModel;
import cam72cam.mod.render.obj.OBJTextureSheet;
import cam72cam.mod.render.opengl.CustomTexture;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.render.opengl.Texture;

import java.util.Map;
import java.util.NavigableMap;

/** Per-draw texture/variant/LOD config. Built via of().lod().variant(), applied by ConfiguredRenderer. */
public class ModelConfig {
    private static final OBJTextureSheet defTex = OBJModel.defTex;

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

    void apply(RenderState state, Model model, boolean waitForLoad) {
        Texture albedo = pickLodSheet(model.getTextures(), waitForLoad);
        if (albedo != null) {
            state.texture(albedo);
        }
        if (model.hasSpecular) {
            Texture spec = pickLodSheet(model.getSpeculars(), waitForLoad);
            if (spec != null) {
                state.specular(spec);
            }
        }
        if (model.hasNormal) {
            Texture norm = pickLodSheet(model.getNormals(), waitForLoad);
            if (norm != null) {
                state.normals(norm);
            }
        }
        state.smooth_shading(model.isSmoothShading);
    }

    private Texture pickLodSheet(Map<String, NavigableMap<Integer, OBJTextureSheet>> variants, boolean waitForLoad) {
        NavigableMap<Integer, OBJTextureSheet> map = variants.get(variant);
        NavigableMap<Integer, OBJTextureSheet> lodMap = map != null ? map : variants.get("");
        if (lodMap == null || lodMap.isEmpty()) {
            return null;
        }

        OBJTextureSheet tex;
        if (lod <= 0) {
            tex = lodMap.lastEntry().getValue();
        } else {
            Map.Entry<Integer, OBJTextureSheet> entry = lodMap.floorEntry(lod);
            tex = (entry != null ? entry : lodMap.firstEntry()).getValue();
        }

        if (waitForLoad) {
            return tex.synchronous(true);
        }

        // Start the load even if it is not ready yet
        tex.getId();
        if (!tex.isLoaded()) {
            // Try to find a loaded LOD, with a sane default
            tex = lodMap.values().stream()
                    .filter(CustomTexture::isLoaded)
                    .findAny().orElse(null);
        }
        return tex != null ? tex : defTex.synchronous(true);
    }
}
