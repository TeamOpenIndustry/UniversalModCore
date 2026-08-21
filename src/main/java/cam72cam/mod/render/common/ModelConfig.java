package cam72cam.mod.render.common;

import cam72cam.mod.Config;
import cam72cam.mod.model.common.mesh.Model;
import cam72cam.mod.render.obj.OBJTextureSheet;
import cam72cam.mod.render.opengl.CustomTexture;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.render.opengl.Texture;
import cam72cam.mod.serialization.ResourceCache;

import java.util.Map;
import java.util.NavigableMap;

/**
 * Per-draw configuration for {@link ModelRenderer}.<br>
 * Configures texture variant, LoD size, and whether texture loading should block synchronously.
 */
public class ModelConfig {
    private static final OBJTextureSheet defTex = new OBJTextureSheet(1, 1, () -> new ResourceCache.GenericByteBuffer(new int[] {0x0000FF}), Integer.MAX_VALUE/2);
    private static final OBJTextureSheet defSpecTex = new OBJTextureSheet(1, 1, () -> new ResourceCache.GenericByteBuffer(new int[]{0x000000}), Integer.MAX_VALUE/2);
    private static final OBJTextureSheet defNormTex = new OBJTextureSheet(1, 1, () -> new ResourceCache.GenericByteBuffer(new int[]{0x8080FF}), Integer.MAX_VALUE/2);

    private int lod = Config.getMaxTextureSize();
    private String variant = "";
    private boolean waitForRightTexLoad = false;

    /**
     * Requests the texture LOD whose max dimension is at most {@code lod}, and falls back to the
     * largest available LOD if none is small enough. A value <= 0 always selects the full-size sheet.
     *
     * @param lod maximum desired texture dimension
     * @return this config
     */
    public ModelConfig lod(int lod) {
        this.lod = lod;
        return this;
    }

    /**
     * Selects the texture variant. Falls back to the default {@code ""} variant if the named
     * one is absent.
     *
     * @param variant variant name, or {@code ""} for the default
     * @return this config
     */
    public ModelConfig variant(String variant) {
        this.variant = variant;
        return this;
    }

    /**
     * Makes binding block until the selected textures finish loading, instead of falling back
     * to whatever LOD is already available.
     *
     * @return this config
     */
    public ModelConfig synchronous() {
        waitForRightTexLoad = true;
        return this;
    }

    void apply(RenderState state, Model model) {
        Texture albedo = pickLodSheet(model.getTextures(), defTex);
        state.texture(albedo != null ? albedo : defTex.synchronous(true));

        Texture spec = model.hasSpecular ? pickLodSheet(model.getSpeculars(), defSpecTex) : null;
        state.specular(spec != null ? spec : defSpecTex.synchronous(true));

        Texture norm = model.hasNormal ? pickLodSheet(model.getNormals(), defNormTex) : null;
        state.normals(norm != null ? norm : defNormTex.synchronous(true));

        state.smooth_shading(model.isSmoothShading);
    }

    private Texture pickLodSheet(Map<String, NavigableMap<Integer, OBJTextureSheet>> variants, OBJTextureSheet fallback) {
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

        if (waitForRightTexLoad) {
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
