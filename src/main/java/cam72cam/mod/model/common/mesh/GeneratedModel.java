package cam72cam.mod.model.common.mesh;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.common.util.FaceAccessor;
import cam72cam.mod.render.common.ModelRenderer;
import cam72cam.mod.resource.Identifier;

import java.util.*;
import java.util.function.Supplier;

/**
 * Internal, don't use directly
 */
public final class GeneratedModel extends Model {
    public GeneratedModel(Model base, Identifier loc, Supplier<float[]> vboSupplier) {
        super(loc, base.getLayout(), vboSupplier, null, base.hasSpecular, base.hasNormal, base.isSmoothShading, base.packedTextureWidth, base.packedTextureHeight);
    }

    @Override
    public FaceAccessor getFaceAccessor() {
        return new FaceAccessor(this, 0, getVboData().length / getLayout().getStride() / 3, false);
    }

    @Override
    public void free() {
        // Textures are shared with and owned by the source model, so only release this baked model's VBO.
        ModelRenderer.getRendererFor(this).free();
    }

    @Override
    public Map<String, ModelGroup> getGroups() {
        return Collections.emptyMap();
    }

    @Override
    public Set<String> groups() {
        return Collections.emptySet();
    }

    @Override
    public Vec3d minOfGroups(Iterable<String> groupNames) {
        throw new UnsupportedOperationException("Not supported in generated model");
    }

    @Override
    public Vec3d maxOfGroups(Iterable<String> groupNames) {
        throw new UnsupportedOperationException("Not supported in generated model");
    }

    @Override
    public Vec3d centerOfGroups(Iterable<String> groupNames) {
        throw new UnsupportedOperationException("Not supported in generated model");
    }

    @Override
    public double lengthOfGroups(Iterable<String> groupNames) {
        throw new UnsupportedOperationException("Not supported in generated model");
    }

    @Override
    public double widthOfGroups(Iterable<String> groupNames) {
        throw new UnsupportedOperationException("Not supported in generated model");
    }

    @Override
    public double heightOfGroups(Iterable<String> groupNames) {
        throw new UnsupportedOperationException("Not supported in generated model");
    }
}
