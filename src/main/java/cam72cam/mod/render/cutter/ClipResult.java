package cam72cam.mod.render.cutter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ClipResult {
    private final Polygon polygon;
    private final List<ClipVertex> intersections;

    public ClipResult(Polygon polygon, List<ClipVertex> intersections) {
        this.polygon = polygon;
        this.intersections = Collections.unmodifiableList(new ArrayList<>(intersections));
    }

    public Polygon getPolygon() {
        return polygon;
    }

    public List<ClipVertex> getIntersections() {
        return intersections;
    }
}