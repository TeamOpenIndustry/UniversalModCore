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
}
