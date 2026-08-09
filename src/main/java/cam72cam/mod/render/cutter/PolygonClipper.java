package cam72cam.mod.render.cutter;

import java.util.ArrayList;
import java.util.List;

public final class PolygonClipper {

    private static final double EPS = 1E-6;

    private PolygonClipper() {}

    /**
     * Keep the positive side of the plane.
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
        List<ClipVertex> intersections = new ArrayList<>();
        int size = vertices.size();

        for (int i = 0; i < size; i++) {
            ClipVertex current = vertices.get(i);
            ClipVertex next = vertices.get((i + 1) % size);

            double dc = plane.distance(current.pos);
            double dn = plane.distance(next.pos);

            boolean currentInside = dc >= -EPS;
            boolean nextInside = dn >= -EPS;

            if (currentInside && nextInside) {
                // inside → inside
                clippedVerts.add(next);

            } else if (currentInside) {
                // inside → outside
                ClipVertex inter = intersection(current, next, dc, dn);
                clippedVerts.add(inter);
                intersections.add(inter.copy());

            } else if (nextInside) {
                // outside → inside
                ClipVertex inter = intersection(current, next, dc, dn);
                clippedVerts.add(inter);
                intersections.add(inter.copy());
                clippedVerts.add(next);
            }
            // outside → outside → nothing
        }

        Polygon clippedPolygon = new Polygon(clippedVerts, polygon.getNormal());
        return new ClipResult(clippedPolygon, intersections);
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