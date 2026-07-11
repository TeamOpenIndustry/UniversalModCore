package cam72cam.mod.render.cutter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Polygon {

    public final List<ClipVertex> vertices = new ArrayList<>();//max = 6 for now?

    public Polygon() {}

    public Polygon(Collection<ClipVertex> vertices) {
        this.vertices.addAll(vertices);
    }

    public Polygon copy() {

        Polygon polygon = new Polygon();

        for (ClipVertex vertex : vertices) {
            polygon.vertices.add(vertex.copy());
        }

        return polygon;
    }
}
