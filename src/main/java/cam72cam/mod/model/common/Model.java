package cam72cam.mod.model.common;

import cam72cam.mod.model.obj.FaceAccessor;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class Model {
    private final Geometry geometry;
    private final Set<Material> materials;
    private final Map<String, ModelGroup> groups;

    public Model(Geometry geometry, Set<Material> materials) {
        this(geometry, materials, Collections.singletonMap("model", ModelGroup.fromGeometry(geometry)));
    }

    public Model(Geometry geometry, Set<Material> materials, Map<String, ModelGroup> groups) {
        this.geometry = geometry;
        this.materials = materials;
        this.groups = groups;
    }

    public void upload() {
        geometry.upload();
//        materials.forEach(Material::upload);
    }

    public void delete() {
        geometry.destroy();
//        materials.forEach(Material::destroy);
    }

    public void render() {
        //TODO
    }

    public Geometry getGeometry() {
        return geometry;
    }
}