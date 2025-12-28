package cam72cam.mod.model.obj;

import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class OBJFace {
    public List<Vec3d> vertices = new ArrayList<>(3);
    public Vec3d normal;
    public List<Vec2f> uv = new ArrayList<>(3);

    public IBoundingBox getBoundingBox() {
        Vec3d min = vertices.get(0).min(vertices.get(1).min(vertices.get(2)));
        Vec3d max = vertices.get(0).max(vertices.get(1).max(vertices.get(2)));
        return IBoundingBox.from(min, max);
    }

    public OBJFace scale(double scale) {
        OBJFace scaled = new OBJFace();
        vertices.forEach(vec3d -> scaled.vertices.add(vec3d.scale(scale)));
        scaled.normal = new Vec3d(normal.internal());
        scaled.uv = new ArrayList<>(uv);
        return scaled;
    }
}
