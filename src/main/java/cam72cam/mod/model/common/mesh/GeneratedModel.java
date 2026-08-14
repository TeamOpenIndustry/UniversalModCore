package cam72cam.mod.model.common.mesh;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.resource.Identifier;

import java.util.LinkedHashMap;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Internal, don't use directly
 */
public final class GeneratedModel extends Model {
    public GeneratedModel(Model baseModel, Identifier location, Supplier<float[]> vboSupplier) {
        super(location, baseModel.getLayout(), vboSupplier, null, baseModel.hasSpecular, baseModel.hasNormal, baseModel.isSmoothShading);
    }

    @Override
    public LinkedHashMap<String, ModelGroup> getGroups() {
        throw new UnsupportedOperationException("Not supported in generated model");
    }

    @Override
    public Set<String> groups() {
        throw new UnsupportedOperationException("Not supported in generated model");
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
