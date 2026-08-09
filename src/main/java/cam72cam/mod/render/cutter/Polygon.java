package cam72cam.mod.render.cutter;

import cam72cam.mod.math.Vec3d;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class Polygon {
    private final List<ClipVertex> vertices;
    private final Vec3d normal; // Could be null

    public Polygon(Collection<ClipVertex> vertices) {
        this(vertices, null);
    }

    public Polygon(Collection<ClipVertex> vertices, Vec3d normal) {
        this.vertices = Collections.unmodifiableList(new ArrayList<>(vertices));
        this.normal = normal;
    }

    public List<ClipVertex> getVertices() {
        return vertices;
    }

    public Vec3d getNormal() {
        return normal;
    }

    public Polygon copy() {
        return new Polygon(vertices, normal);
    }
}