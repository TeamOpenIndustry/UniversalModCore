package cam72cam.mod.render.common;

import cam72cam.mod.Config;
import cam72cam.mod.model.common.mesh.Model;
import cam72cam.mod.model.obj.OBJModel;
import cam72cam.mod.render.obj.OBJTextureSheet;
import cam72cam.mod.render.opengl.CustomTexture;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.render.opengl.Texture;
import cam72cam.mod.serialization.ResourceCache;

import java.util.Map;
import java.util.NavigableMap;

public class ModelConfig {
    private static final OBJTextureSheet defTex = OBJModel.defTex;
    private static final OBJTextureSheet defSpecTex = new OBJTextureSheet(1, 1, () -> new ResourceCache.GenericByteBuffer(new int[]{0x000000}), Integer.MAX_VALUE/2);
    private static final OBJTextureSheet defNormTex = new OBJTextureSheet(1, 1, () -> new ResourceCache.GenericByteBuffer(new int[]{0x8080FF}), Integer.MAX_VALUE/2);

    private int lod = Config.getMaxTextureSize();
    private String variant = "";

    public void lod(int lod) {
        this.lod = lod;
    }

    public void variant(String variant) {
        this.variant = variant;
    }

    void apply(RenderState state, Model model, boolean waitForLoad) {
        Texture albedo = pickLodSheet(model.getTextures(), defTex, waitForLoad);
        state.texture(albedo != null ? albedo : defTex.synchronous(true));

        Texture spec = model.hasSpecular ? pickLodSheet(model.getSpeculars(), defSpecTex, waitForLoad) : null;
        state.specular(spec != null ? spec : defSpecTex.synchronous(true));

        Texture norm = model.hasNormal ? pickLodSheet(model.getNormals(), defNormTex, waitForLoad) : null;
        state.normals(norm != null ? norm : defNormTex.synchronous(true));

        state.smooth_shading(model.isSmoothShading);
    }

    private Texture pickLodSheet(Map<String, NavigableMap<Integer, OBJTextureSheet>> variants, OBJTextureSheet fallback, boolean waitForLoad) {
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
        return tex != null ? tex : fallback.synchronous(true);
    }
}
