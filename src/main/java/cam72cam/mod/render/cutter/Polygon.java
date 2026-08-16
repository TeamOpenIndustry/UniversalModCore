package cam72cam.mod.render.cutter;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.cutter.adapter.QuadTemplate;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class Polygon {
    private static final double EPS = 1E-6;
    private final List<ClipVertex> vertices;
    private final Vec3d normal; // Could be null

    public Polygon(Collection<ClipVertex> vertices) {
        this(vertices, null);
    }

    public Polygon(Collection<ClipVertex> vertices, Vec3d normal) {
        this.vertices = Collections.unmodifiableList(new ArrayList<>(vertices));
        this.normal = normal;
    }

    public static Polygon generateUV(Polygon polygon, QuadTemplate template) {

        Vec3d p0 = template.sourcePos[0];
        Vec3d p1 = template.sourcePos[1];
        Vec3d p3 = template.sourcePos[3];

        // position space basis
        Vec3d e1 = p1.subtract(p0);
        Vec3d e2 = p3.subtract(p0);

        double a00 = e1.dotProduct(e1);
        double a01 = e1.dotProduct(e2);
        double a11 = e2.dotProduct(e2);

        // 2x2 inverse determinant
        double det = a00 * a11 - a01 * a01;

        if (Math.abs(det) < 1E-8) {
            return polygon.copy();
        }

        List<ClipVertex> vertices = new ArrayList<>(polygon.getVertices());

        for (ClipVertex vertex : vertices) {
            Vec3d d = vertex.pos.subtract(p0);

            double b0 = d.dotProduct(e1);
            double b1 = d.dotProduct(e2);

            double x = (b0 * a11 - b1 * a01) / det;
            double y = (b1 * a00 - b0 * a01) / det;

            vertex.u = (float) (
                    template.sourceU[0]
                            + x * (template.sourceU[1] - template.sourceU[0])
                            + y * (template.sourceU[3] - template.sourceU[0])
            );

            vertex.v = (float) (
                    template.sourceV[0]
                            + x * (template.sourceV[1] - template.sourceV[0])
                            + y * (template.sourceV[3] - template.sourceV[0])
            );
        }

        Collections.reverse(vertices);
        return new Polygon(vertices, polygon.getNormal());
    }

    /**
     * Splits a convex polygon into quads using fan triangulation from the first vertex.
     * Triangles are represented as degenerate quads (last vertex duplicated).
     * Assumes input polygon is convex; for concave polygons, use ear clipping first.
     *
     * @param polygon the convex polygon to split
     * @return a list of quads (each as a Polygon with 4 vertices)
     */
    public static List<Polygon> convexToQuads(Polygon polygon) {
        List<Polygon> result = new ArrayList<>();
        List<ClipVertex> verts = polygon.getVertices();
        int n = verts.size();
        if (n < 3) return result;

        ClipVertex first = verts.get(0);
        for (int i = 1; i < n - 1; i++) {
            ClipVertex a = first;
            ClipVertex b = verts.get(i);
            ClipVertex c = verts.get(i + 1);
            List<ClipVertex> quadVerts = new ArrayList<>(4);
            quadVerts.add(a);
            quadVerts.add(b);
            quadVerts.add(c);
            quadVerts.add(c);
            result.add(new Polygon(quadVerts, polygon.getNormal()));
        }
        return result;
    }

    static ClipVertex intersection(
            ClipVertex a,
            ClipVertex b,
            double da,
            double db) {

        double t = da / (da - db);
        return a.lerp(b, t);
    }

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