package cam72cam.mod.model.common.mesh;

import cam72cam.mod.resource.Identifier;

import java.util.LinkedHashMap;
import java.util.function.Supplier;

/**
 * A baked model produced by {@link ModelSetBuilder}, sharing the source model's texture sheets.
 */
public final class GeneratedModel extends Model {
    public GeneratedModel(Model base, Identifier loc, Supplier<float[]> vboSupplier, LinkedHashMap<String, ModelGroup> groups) {
        super(loc, base.getLayout(), vboSupplier, groups, base.hasSpecular, base.hasNormal, base.isSmoothShading, base.packedTextureWidth, base.packedTextureHeight);
        // Share the source model's texture lifetime
        shareTexturesWith(base);
    }
}
