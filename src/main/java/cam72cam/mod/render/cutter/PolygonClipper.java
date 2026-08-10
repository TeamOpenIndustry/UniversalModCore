package cam72cam.mod.render.cutter;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public final class PolygonClipper {

    private static final double EPS = 1E-6;

    private PolygonClipper() {}

    /**
     * Keep the positive side of the plane.
     * Returns ClipResult containing clipped polygon and list of (exit, entry) intersection pairs.
     */
    public static ClipResult clip(Polygon polygon, Plane plane) {
        List<ClipVertex> vertices = polygon.getVertices();

        if (vertices.isEmpty()) {
            return new ClipResult(
                    new Polygon(new ArrayList<>(), polygon.getNormal()),
                    new ArrayList<>()
            );
        }

        List<ClipVertex> clippedVerts = new ArrayList<>();
        List<Pair<ClipVertex, ClipVertex>> intersectionPairs = new ArrayList<>();
        ClipVertex lastExit = null;
        ClipVertex firstEntry = null; // for wrap-around pairing

        int size = vertices.size();

        for (int i = 0; i < size; i++) {
            ClipVertex current = vertices.get(i);
            ClipVertex next = vertices.get((i + 1) % size);

            double dc = plane.distance(current.pos);
            double dn = plane.distance(next.pos);

            boolean currentInside = dc >= -EPS;
            boolean nextInside = dn >= -EPS;

            if (currentInside && nextInside) {
                clippedVerts.add(next);

            } else if (currentInside) {
                // inside → outside : exit point
                ClipVertex inter = intersection(current, next, dc, dn);
                clippedVerts.add(inter);
                lastExit = inter;

            } else if (nextInside) {
                // outside → inside : entry point
                ClipVertex inter = intersection(current, next, dc, dn);
                clippedVerts.add(inter);

                // Pair with last exit if exists
                if (lastExit != null) {
                    intersectionPairs.add(Pair.of(lastExit, inter));
                    lastExit = null;
                } else {
                    // No previous exit → this is the first entry (for wrap-around)
                    if (firstEntry == null) {
                        firstEntry = inter;
                    }
                }
                clippedVerts.add(next);
            }
            // outside → outside: nothing
        }

        // Handle wrap‑around: if there's an unpaired exit, pair it with the first entry
        if (lastExit != null && firstEntry != null) {
            intersectionPairs.add(Pair.of(lastExit, firstEntry));
        }

        Polygon clippedPolygon = new Polygon(clippedVerts, polygon.getNormal());
        return new ClipResult(clippedPolygon, intersectionPairs);
    }

    private static ClipVertex intersection(
            ClipVertex a,
            ClipVertex b,
            double da,
            double db) {

        double t = da / (da - db);
        return a.lerp(b, t);
    }
}