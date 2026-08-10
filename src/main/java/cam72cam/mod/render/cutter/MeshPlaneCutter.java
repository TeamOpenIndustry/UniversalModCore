package cam72cam.mod.render.cutter;

import cam72cam.mod.ModCore;
import cam72cam.mod.math.Vec3d;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

/**
 * Utility for cutting a list of primitives (e.g., BakedQuads) with a plane.
 * <p>
 * The algorithm:
 * <ol>
 *   <li>Each primitive is clipped against the plane using {@link PolygonClipper}.</li>
 *   <li>The resulting fragments are converted back to primitives via the adapter.</li>
 *   <li>Intersection points (pairs of entry/exit) are collected and used to build a graph.</li>
 *   <li>All closed loops (Eulerian cycles) are extracted using Hierholzer's algorithm.</li>
 *   <li>Loops are split into simple polygons if they contain repeated coordinates
 *       (which can happen when multiple loops share vertices or edges).</li>
 *   <li>Each simple loop is used to generate a cap polygon, which is then rendered.</li>
 * </ol>
 * <p>
 * The implementation handles:
 * <ul>
 *   <li>Convex polygons (direct cap generation).</li>
 *   <li>Concave polygons (ear‑clipping triangulation before cap generation).</li>
 *   <li>Multiple disjoint rings (e.g., stair treads).</li>
 *   <li>Rings sharing vertices (e.g., two rectangles touching at a corner).</li>
 *   <li>Rings sharing edges (e.g., "日"‑shaped two rectangles sharing a side).</li>
 * </ul>
 * <p>
 * All operations are compatible with Java 8.
 *
 * @see PrimitiveAdapter
 * @see PolygonClipper
 * @see CapBuilder
 */
public final class MeshPlaneCutter {

    private static final double EPS = 1e-4;

    private MeshPlaneCutter() {
        // Private constructor to prevent instantiation.
    }

    /**
     * Cuts the given list of primitives with the specified plane.
     *
     * @param primitives the input primitives (must be convertible to polygons)
     * @param plane      the cutting plane (positive side is kept)
     * @param adapter    the adapter for converting between primitives and polygons
     * @param <T>        the primitive type
     * @param <Template> the template type for UV generation
     * @return a list of primitives resulting from the cut (including fragments and caps)
     */
    public static <T, Template> List<T> cut(
            List<T> primitives,
            Plane plane,
            PrimitiveAdapter<T, Template> adapter) {

        List<T> result = new ArrayList<>();
        List<Pair<ClipVertex, ClipVertex>> allPairs = new ArrayList<>();

        // 1. Clip each primitive and collect intersection point pairs.
        for (T primitive : primitives) {
            Polygon polygon = adapter.toPolygon(primitive);
            ClipResult clipped = PolygonClipper.clip(polygon, plane);

            allPairs.addAll(clipped.getIntersections());

            if (clipped.getPolygon().getVertices().size() >= 3) {
                result.addAll(
                        adapter.fromPrimitive(
                                clipped.getPolygon(),
                                primitive
                        )
                );
            }
        }

        // 2. Extract all closed loops (rings) from the intersection graph.
        List<List<ClipVertex>> rings = extractRings(allPairs);
        if (rings.isEmpty()) {
            ModCore.error("Fail to get ring with ClipVertex Pairs:");
            for (Pair<ClipVertex, ClipVertex> pair : allPairs) {
                ModCore.error("%s -> %s", pair.getLeft().pos, pair.getRight().pos);
            }
        }

        // 3. Generate a cap for each ring.
        Template template = adapter.createTemplate(primitives, plane);
        if (template == null) return result;

        for (List<ClipVertex> ring : rings) {
            if (ring.size() < 3) continue;

            // Ensure counter‑clockwise winding (relative to plane normal).
            if (signedArea(ring, plane.normal) < 0) {
                Collections.reverse(ring);
            }

            boolean isConvex = isPolygonConvex(ring, plane.normal);

            if (isConvex) {
                // Convex polygon: use directly.
                Polygon capPoly = new Polygon(ring, plane.normal);
                adapter.prepareCap(capPoly, plane, template);
                result.addAll(adapter.fromTemplate(capPoly, template));
            } else {
                // Concave polygon: triangulate first.
                List<List<ClipVertex>> triangles = earClip(ring, plane.normal);
                for (List<ClipVertex> tri : triangles) {
                    // Convert triangle to quad by duplicating the last vertex.
                    List<ClipVertex> quadVerts = new ArrayList<>(4);
                    quadVerts.add(tri.get(0));
                    quadVerts.add(tri.get(1));
                    quadVerts.add(tri.get(2));
                    quadVerts.add(tri.get(2));
                    Polygon capPoly = new Polygon(quadVerts, plane.normal);
                    adapter.prepareCap(capPoly, plane, template);
                    result.addAll(adapter.fromTemplate(capPoly, template));
                }
            }
        }

        return result;
    }

    // =========================================================================
    //  Graph‑based loop extraction (Hierholzer + splitting)
    // =========================================================================

    /**
     * Extracts all simple closed loops from a list of intersection edges.
     * <p>
     * The method:
     * <ol>
     *   <li>Merges vertices that are within {@link #EPS} of each other.</li>
     *   <li>Builds a multigraph using {@code List} adjacency to preserve parallel edges.</li>
     *   <li>For each connected component, checks that all vertex degrees are even
     *       (Eulerian condition).</li>
     *   <li>Repeatedly extracts Eulerian circuits using Hierholzer's algorithm.</li>
     *   <li>Each extracted circuit is split into simple polygons if it contains
     *       repeated coordinates (which indicate multiple loops sharing vertices or edges).</li>
     * </ol>
     *
     * @param pairs the list of directed edges (entry → exit)
     * @return a list of rings, each represented as a list of vertices in order
     */
    private static List<List<ClipVertex>> extractRings(List<Pair<ClipVertex, ClipVertex>> pairs) {
        if (pairs.isEmpty()) return Collections.emptyList();

        // 1. Merge vertices with identical coordinates.
        Map<Vec3d, ClipVertex> coordMap = new HashMap<>();
        java.util.function.Function<ClipVertex, ClipVertex> getMerged = (v) -> {
            for (Vec3d key : coordMap.keySet()) {
                if (key.distanceTo(v.pos) < EPS) {
                    return coordMap.get(key);
                }
            }
            coordMap.put(v.pos, v);
            return v;
        };

        // 2. Build adjacency list (using List to allow parallel edges).
        Map<ClipVertex, List<ClipVertex>> graph = new HashMap<>();
        for (Pair<ClipVertex, ClipVertex> p : pairs) {
            ClipVertex a = getMerged.apply(p.getLeft());
            ClipVertex b = getMerged.apply(p.getRight());
            graph.computeIfAbsent(a, k -> new ArrayList<>()).add(b);
            graph.computeIfAbsent(b, k -> new ArrayList<>()).add(a);
        }

        List<List<ClipVertex>> allRings = new ArrayList<>();
        Set<ClipVertex> globalVisited = new HashSet<>();

        for (ClipVertex start : graph.keySet()) {
            if (globalVisited.contains(start)) continue;

            // 3. Extract the connected component.
            Set<ClipVertex> component = new HashSet<>();
            Deque<ClipVertex> stack = new ArrayDeque<>();
            stack.push(start);
            component.add(start);
            while (!stack.isEmpty()) {
                ClipVertex v = stack.pop();
                for (ClipVertex neighbor : graph.get(v)) {
                    if (!component.contains(neighbor)) {
                        component.add(neighbor);
                        stack.push(neighbor);
                    }
                }
            }

            // 4. Check that all degrees are even.
            boolean allEven = true;
            for (ClipVertex v : component) {
                if (graph.get(v).size() % 2 != 0) {
                    allEven = false;
                    break;
                }
            }
            if (!allEven) {
                globalVisited.addAll(component);
                continue;
            }

            // 5. Copy adjacency for modification.
            Map<ClipVertex, List<ClipVertex>> graphCopy = new HashMap<>();
            for (ClipVertex v : component) {
                graphCopy.put(v, new ArrayList<>(graph.get(v)));
            }

            // 6. Repeatedly extract Eulerian circuits.
            while (true) {
                ClipVertex startVertex = null;
                for (ClipVertex v : component) {
                    if (!graphCopy.get(v).isEmpty()) {
                        startVertex = v;
                        break;
                    }
                }
                if (startVertex == null) break;

                List<ClipVertex> ring = new ArrayList<>();
                Deque<ClipVertex> stack2 = new ArrayDeque<>();
                ClipVertex current = startVertex;
                stack2.push(current);
                while (!stack2.isEmpty()) {
                    if (!graphCopy.get(current).isEmpty()) {
                        stack2.push(current);
                        ClipVertex next = graphCopy.get(current).remove(0); // remove first edge
                        // Remove reverse edge (parallel edges remain).
                        graphCopy.get(next).remove(current);
                        current = next;
                    } else {
                        ring.add(current);
                        current = stack2.pop();
                    }
                }
                Collections.reverse(ring);

                // Remove duplicate start point (if any).
                if (!ring.isEmpty() && ring.get(0) == ring.get(ring.size() - 1)) {
                    ring.remove(ring.size() - 1);
                }

                if (ring.size() >= 3) {
                    // Split if the circuit contains repeated coordinates.
                    allRings.addAll(splitRing(ring));
                }
            }

            globalVisited.addAll(component);
        }

        return allRings;
    }

    // =========================================================================
    //  Ring splitting (based on repeated coordinates)
    // =========================================================================

    /**
     * Recursively splits a ring that may contain repeated vertices (due to shared
     * vertices or edges) into simple polygons with no repeated coordinates.
     * <p>
     * The method finds a coordinate that appears at least twice, picks a split
     * pair that gives the largest gap (to avoid tiny rings), and cuts the ring
     * at those positions. The resulting sub‑rings are processed recursively.
     *
     * @param ring the input ring (may have repeated coordinates)
     * @return a list of simple rings (no repeated coordinates) with at least 3 vertices
     */
    private static List<List<ClipVertex>> splitRing(List<ClipVertex> ring) {
        List<List<ClipVertex>> result = new ArrayList<>();
        if (ring.size() < 3) {
            result.add(ring);
            return result;
        }

        // 1. Collect indices for each coordinate.
        Map<Vec3d, List<Integer>> posIndices = new HashMap<>();
        for (int i = 0; i < ring.size(); i++) {
            Vec3d pos = ring.get(i).pos;
            posIndices.computeIfAbsent(pos, k -> new ArrayList<>()).add(i);
        }

        // 2. Find coordinates that appear at least twice.
        List<Map.Entry<Vec3d, List<Integer>>> duplicates = new ArrayList<>();
        for (Map.Entry<Vec3d, List<Integer>> entry : posIndices.entrySet()) {
            if (entry.getValue().size() >= 2) {
                duplicates.add(entry);
            }
        }

        if (duplicates.isEmpty()) {
            // No duplicates: this ring is already simple.
            result.add(ring);
            return result;
        }

        // 3. Pick the duplicate pair with the largest gap (first occurrence to second).
        Map.Entry<Vec3d, List<Integer>> best = null;
        int bestGap = -1;
        for (Map.Entry<Vec3d, List<Integer>> entry : duplicates) {
            List<Integer> idxs = entry.getValue();
            for (int i = 0; i < idxs.size() - 1; i++) {
                int gap = idxs.get(i + 1) - idxs.get(i);
                if (gap > bestGap) {
                    bestGap = gap;
                    best = entry;
                }
            }
        }

        if (best == null) {
            result.add(ring);
            return result;
        }

        List<Integer> idxs = best.getValue();
        int first = idxs.get(0);
        int second = idxs.get(1);

        // 4. Split the ring into two sub‑rings.
        List<ClipVertex> sub1 = new ArrayList<>(ring.subList(first, second + 1));
        List<ClipVertex> sub2 = new ArrayList<>();
        sub2.addAll(ring.subList(second, ring.size()));
        sub2.addAll(ring.subList(0, first + 1));

        // Remove duplicate end vertices (since we included the start point in both).
        if (sub1.size() > 1 && sub1.get(0) == sub1.get(sub1.size() - 1)) {
            sub1.remove(sub1.size() - 1);
        }
        if (sub2.size() > 1 && sub2.get(0) == sub2.get(sub2.size() - 1)) {
            sub2.remove(sub2.size() - 1);
        }

        // Guard against infinite recursion: sub‑rings must be strictly smaller.
        if (sub1.size() >= ring.size() || sub2.size() >= ring.size()) {
            result.add(ring);
            return result;
        }

        // 5. Recurse on each sub‑ring.
        if (sub1.size() >= 3) {
            result.addAll(splitRing(sub1));
        } else {
            result.add(sub1);
        }
        if (sub2.size() >= 3) {
            result.addAll(splitRing(sub2));
        } else {
            result.add(sub2);
        }

        return result;
    }

    // =========================================================================
    //  Geometric utilities
    // =========================================================================

    /**
     * Computes the signed area of a polygon projected onto the plane normal.
     * <p>
     * The sign indicates the winding direction (positive = counter‑clockwise).
     *
     * @param poly   the polygon vertices
     * @param normal the plane normal
     * @return the signed area
     */
    private static double signedArea(List<ClipVertex> poly, Vec3d normal) {
        double area = 0;
        int n = poly.size();
        for (int i = 0; i < n; i++) {
            Vec3d p1 = poly.get(i).pos;
            Vec3d p2 = poly.get((i + 1) % n).pos;
            area += p1.crossProduct(p2).dotProduct(normal);
        }
        return area;
    }

    /**
     * Checks if a polygon is convex.
     * <p>
     * All cross products of consecutive edges must have the same sign
     * (relative to the plane normal).
     *
     * @param polygon the polygon vertices
     * @param normal  the plane normal
     * @return true if the polygon is convex
     */
    private static boolean isPolygonConvex(List<ClipVertex> polygon, Vec3d normal) {
        int n = polygon.size();
        if (n < 3) return false;

        double lastCross = 0;
        for (int i = 0; i < n; i++) {
            Vec3d p1 = polygon.get(i).pos;
            Vec3d p2 = polygon.get((i + 1) % n).pos;
            Vec3d p3 = polygon.get((i + 2) % n).pos;
            Vec3d v1 = p2.subtract(p1);
            Vec3d v2 = p3.subtract(p2);
            double cross = v1.crossProduct(v2).dotProduct(normal);
            if (i == 0) {
                lastCross = cross;
            } else if (cross * lastCross < 0) {
                return false;
            }
        }
        return true;
    }

    // =========================================================================
    //  Ear‑clipping triangulation
    // =========================================================================

    /**
     * Triangulates a concave polygon using the ear‑clipping algorithm.
     * <p>
     * The polygon must be simple, planar, and have vertices in counter‑clockwise
     * order. The result is a list of triangles, each as a list of 3 vertices.
     *
     * @param polygon the input polygon (must be CCW)
     * @param normal  the plane normal (used for convexity tests)
     * @return a list of triangles (each a list of 3 vertices)
     */
    private static List<List<ClipVertex>> earClip(List<ClipVertex> polygon, Vec3d normal) {
        List<List<ClipVertex>> triangles = new ArrayList<>();
        List<ClipVertex> verts = new ArrayList<>(polygon);
        int n = verts.size();
        if (n < 3) return triangles;

        while (verts.size() > 3) {
            boolean found = false;
            for (int i = 0; i < verts.size(); i++) {
                int prev = (i - 1 + verts.size()) % verts.size();
                int next = (i + 1) % verts.size();
                ClipVertex a = verts.get(prev);
                ClipVertex b = verts.get(i);
                ClipVertex c = verts.get(next);

                if (isConvexEar(a, b, c, normal) && !hasVerticesInside(a, b, c, verts, normal)) {
                    List<ClipVertex> tri = new ArrayList<>(Arrays.asList(a, b, c));
                    triangles.add(tri);
                    verts.remove(i);
                    found = true;
                    break;
                }
            }
            if (!found) {
                // Fallback: take the first three vertices.
                List<ClipVertex> fallback = new ArrayList<>(Arrays.asList(verts.get(0), verts.get(1), verts.get(2)));
                if (signedArea(fallback, normal) < 0) {
                    Collections.reverse(fallback);
                }
                triangles.add(fallback);
                verts.remove(1);
            }
        }
        if (verts.size() == 3) {
            List<ClipVertex> tri = new ArrayList<>(verts);
            if (signedArea(tri, normal) < 0) {
                Collections.reverse(tri);
            }
            triangles.add(tri);
        }

        return triangles;
    }

    /**
     * Checks if the angle at vertex b is convex (ear condition).
     * Assumes vertices are in counter‑clockwise order.
     *
     * @param a      previous vertex
     * @param b      current vertex
     * @param c      next vertex
     * @param normal plane normal
     * @return true if the ear is convex (cross product > 0)
     */
    private static boolean isConvexEar(ClipVertex a, ClipVertex b, ClipVertex c, Vec3d normal) {
        Vec3d ab = b.pos.subtract(a.pos);
        Vec3d bc = c.pos.subtract(b.pos);
        double cross = ab.crossProduct(bc).dotProduct(normal);
        return cross > 0;
    }

    /**
     * Tests whether any vertex from the given list lies inside the triangle (a,b,c).
     *
     * @param a      triangle vertex 1
     * @param b      triangle vertex 2
     * @param c      triangle vertex 3
     * @param verts  list of vertices to test (excluding a,b,c)
     * @param normal plane normal (used for projection)
     * @return true if at least one vertex is inside the triangle
     */
    private static boolean hasVerticesInside(ClipVertex a, ClipVertex b, ClipVertex c,
                                             List<ClipVertex> verts, Vec3d normal) {
        // Project vertices onto the plane's 2D coordinate system.
        Vec3d u = normal.crossProduct(new Vec3d(1, 0, 0));
        if (u.length() < 1e-8) u = normal.crossProduct(new Vec3d(0, 1, 0));
        u = u.normalize();
        Vec3d v = normal.crossProduct(u).normalize();

        double ax = a.pos.dotProduct(u), ay = a.pos.dotProduct(v);
        double bx = b.pos.dotProduct(u), by = b.pos.dotProduct(v);
        double cx = c.pos.dotProduct(u), cy = c.pos.dotProduct(v);

        for (ClipVertex p : verts) {
            if (p == a || p == b || p == c) continue;
            double px = p.pos.dotProduct(u), py = p.pos.dotProduct(v);
            if (pointInTriangle2D(px, py, ax, ay, bx, by, cx, cy)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 2D point‑in‑triangle test using barycentric coordinates.
     *
     * @param px point x
     * @param py point y
     * @param ax triangle vertex 1 x
     * @param ay triangle vertex 1 y
     * @param bx triangle vertex 2 x
     * @param by triangle vertex 2 y
     * @param cx triangle vertex 3 x
     * @param cy triangle vertex 3 y
     * @return true if the point is inside the triangle (including edges)
     */
    private static boolean pointInTriangle2D(double px, double py,
                                             double ax, double ay,
                                             double bx, double by,
                                             double cx, double cy) {
        double d1 = sign2D(px, py, ax, ay, bx, by);
        double d2 = sign2D(px, py, bx, by, cx, cy);
        double d3 = sign2D(px, py, cx, cy, ax, ay);
        boolean hasNeg = (d1 < 0) || (d2 < 0) || (d3 < 0);
        boolean hasPos = (d1 > 0) || (d2 > 0) || (d3 > 0);
        return !(hasNeg && hasPos);
    }

    /**
     * Computes the signed area of the triangle (p1,p2,p3) in the XY plane.
     *
     * @param p1x point 1 x
     * @param p1y point 1 y
     * @param p2x point 2 x
     * @param p2y point 2 y
     * @param p3x point 3 x
     * @param p3y point 3 y
     * @return the signed area (2× area)
     */
    private static double sign2D(double p1x, double p1y,
                                 double p2x, double p2y,
                                 double p3x, double p3y) {
        return (p1x - p3x) * (p2y - p3y) - (p2x - p3x) * (p1y - p3y);
    }
}