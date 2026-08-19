package cam72cam.mod.render.cutter;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ClipResult {
    private final Polygon polygon;
    private final List<Pair<ClipVertex, ClipVertex>> intersections;

    public ClipResult(Polygon polygon, List<Pair<ClipVertex, ClipVertex>> intersections) {
        this.polygon = polygon;
        this.intersections = Collections.unmodifiableList(new ArrayList<>(intersections));
    }

    public Polygon getPolygon() {
        return polygon;
    }

    public List<Pair<ClipVertex, ClipVertex>> getIntersections() {
        return intersections;
    }
}