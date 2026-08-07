package cam72cam.mod.render.cutter;

import java.util.List;

public final class PolygonClipper {

    private static final double EPS = 1E-6;

    private PolygonClipper() {}

    /**
     * Keep the positive side of the plane.
     */
    public static ClipResult clip(Polygon polygon, Plane plane) {
        ClipResult result = new ClipResult();

        List<ClipVertex> vertices = polygon.vertices;

        if (vertices.isEmpty()) {
            return result;
        }

        int size = vertices.size();

        for (int i = 0; i < size; i++) {

            ClipVertex current = vertices.get(i);
            ClipVertex next = vertices.get((i + 1) % size);

            double dc = plane.distance(current.pos);
            double dn = plane.distance(next.pos);

            boolean currentInside = dc >= -EPS;
            boolean nextInside = dn >= -EPS;

            if (currentInside && nextInside) {

                // inside -> inside
                result.polygon.vertices.add(next);

            } else if (currentInside) {

                // inside -> outside
                ClipVertex inter = intersection(current, next, dc, dn);

                result.polygon.vertices.add(inter);

                result.intersections.add(inter.copy());

            } else if (nextInside) {

                // outside -> inside
                ClipVertex inter = intersection(current, next, dc, dn);

                result.polygon.vertices.add(inter);

                result.intersections.add(inter.copy());

                result.polygon.vertices.add(next);

            }
            // outside -> outside
        }

        return result;
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