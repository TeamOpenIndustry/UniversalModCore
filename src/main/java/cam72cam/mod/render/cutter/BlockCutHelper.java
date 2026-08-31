package cam72cam.mod.render.cutter;

import cam72cam.mod.math.Plane;
import cam72cam.mod.math.Vec3d;

import java.util.*;

public final class BlockCutHelper {

    private static final double EPS = 1e-6;

    private static final Map<Plane, List<Vec3d>> INTERSECTION_CACHE = new HashMap<>();
    private static final Map<Plane, List<Vec3d>> CLIPPED_CACHE = new HashMap<>();

    private static final Vec3d[] CORNERS = {
            new Vec3d(0, 0, 0),
            new Vec3d(1, 0, 0),
            new Vec3d(0, 1, 0),
            new Vec3d(1, 1, 0),
            new Vec3d(0, 0, 1),
            new Vec3d(1, 0, 1),
            new Vec3d(0, 1, 1),
            new Vec3d(1, 1, 1)
    };

    private static final int[][] EDGES = {
            {0, 1}, {0, 2}, {0, 4},
            {1, 3}, {1, 5},
            {2, 3}, {2, 6},
            {4, 5}, {4, 6},
            {3, 7}, {5, 7}, {6, 7}
    };

    public static float getCutCenterHeight(Plane plane) {
        List<Vec3d> intersections = getCachedIntersections(plane);

        if (intersections.isEmpty()) {
            return 0;
        }

        double y = 0;
        for (Vec3d v : intersections) {
            y += v.y;
        }

        return (float) (y / intersections.size());
    }

    public static float getFullHeight(Plane plane) {
        List<Vec3d> vertices = getCachedClippedVertices(plane);

        if (vertices.isEmpty()) {
            return 1;
        }

        double max = 0;
        for (Vec3d v : vertices) {
            max = Math.max(max, v.y);
        }

        return (float) max;
    }

    public static float getCutPlaneMinHeight(Plane plane) {
        List<Vec3d> intersections = getCachedIntersections(plane);

        if (intersections.isEmpty()) {
            return 0;
        }

        double min = Double.POSITIVE_INFINITY;
        for (Vec3d v : intersections) {
            min = Math.min(min, v.y);
        }

        return (float) min;
    }

    public static float getCutPlaneMaxHeight(Plane plane) {
        List<Vec3d> intersections = getCachedIntersections(plane);

        if (intersections.isEmpty()) {
            return 1;
        }

        double max = Double.NEGATIVE_INFINITY;
        for (Vec3d v : intersections) {
            max = Math.max(max, v.y);
        }

        return (float) max;
    }

    public static Plane createBottomSidePlane(Plane plane) {
        List<Vec3d> bottom = getBottomIntersections(plane);

        if (bottom.size() != 2) {
            return null;
        }

        Vec3d p0 = bottom.get(0);
        Vec3d p1 = bottom.get(1);

        Vec3d edge = p1.subtract(p0).normalize();
        Vec3d sideNormal = edge.crossProduct(new Vec3d(0, 1, 0)).normalize();
        Vec3d center = p0.add(p1).scale(0.5);

        double positive = plane.distance(center.add(sideNormal.scale(0.01)));
        double negative = plane.distance(center.subtract(sideNormal.scale(0.01)));

        if (positive < negative) {
            sideNormal = sideNormal.scale(-1);
        }

        return new Plane(center, sideNormal);
    }

    private static List<Vec3d> getClippedVertices(Plane plane) {
        List<Vec3d> result = new ArrayList<>();

        for (Vec3d corner : CORNERS) {
            if (plane.distance(corner) >= -EPS) {
                result.add(corner);
            }
        }

        for (int[] edge : EDGES) {
            Vec3d a = CORNERS[edge[0]];
            Vec3d b = CORNERS[edge[1]];

            double da = plane.distance(a);
            double db = plane.distance(b);

            if (da * db < 0) {
                double t = da / (da - db);
                addUnique(result, a.add(b.subtract(a).scale(t)));
            }
        }

        return result;
    }

    private static List<Vec3d> getCachedIntersections(Plane plane) {
        List<Vec3d> result = INTERSECTION_CACHE.get(plane);
        if (result == null) {
            result = getIntersections(plane);
            INTERSECTION_CACHE.put(plane, result);
        }
        return result;
    }

    private static List<Vec3d> getCachedClippedVertices(Plane plane) {
        List<Vec3d> result = CLIPPED_CACHE.get(plane);
        if (result == null) {
            result = getClippedVertices(plane);
            CLIPPED_CACHE.put(plane, result);
        }
        return result;
    }

    private static List<Vec3d> getIntersections(Plane plane) {
        List<Vec3d> result = new ArrayList<>();

        for (int[] edge : EDGES) {
            Vec3d a = CORNERS[edge[0]];
            Vec3d b = CORNERS[edge[1]];

            double da = plane.distance(a);
            double db = plane.distance(b);

            if (Math.abs(da - db) < EPS) {
                continue;
            }

            if (da * db <= 0) {
                double t = da / (da - db);
                addUnique(result, a.add(b.subtract(a).scale(t)));
            }
        }

        return result;
    }

    private static List<Vec3d> getBottomIntersections(Plane plane) {
        Vec3d[] bottom = {
                CORNERS[0], CORNERS[1], CORNERS[4], CORNERS[5]
        };

        int[][] edges = {
                {0, 1}, {0, 2}, {1, 3}, {2, 3}
        };

        List<Vec3d> result = new ArrayList<>();

        for (int[] edge : edges) {
            Vec3d a = bottom[edge[0]];
            Vec3d b = bottom[edge[1]];

            double da = plane.distance(a);
            double db = plane.distance(b);

            if (Math.abs(da - db) < EPS) {
                continue;
            }

            if (da * db <= 0) {
                double t = da / (da - db);
                addUnique(result, a.add(b.subtract(a).scale(t)));
            }
        }

        return result;
    }

    private static void addUnique(List<Vec3d> list, Vec3d value) {
        for (Vec3d v : list) {
            if (Math.abs(v.x - value.x) < EPS &&
                    Math.abs(v.y - value.y) < EPS &&
                    Math.abs(v.z - value.z) < EPS) {
                return;
            }
        }
        list.add(value);
    }
}