package cam72cam.mod.render.cutter;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.cutter.adapter.PrimitiveAdapter;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class MeshPlaneCutter {

    private static final double EPS = 1e-6;

    public static <T, Template> List<T> cut(
            List<T> primitives,
            Plane plane,
            PrimitiveAdapter<T, Template> adapter) {

        List<T> result = new ArrayList<>();
        List<Pair<ClipVertex, ClipVertex>> allPairs = new ArrayList<>();

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

        List<List<ClipVertex>> rings = extractRings(allPairs);

        Template template = adapter.createTemplate(primitives, plane);
        if (template == null) return result;

        for (List<ClipVertex> ring : rings) {
            if (ring.size() < 3) continue;

            if (signedArea(ring, plane.normal) < 0) {
                Collections.reverse(ring);
            }

            boolean isConvex = isPolygonConvex(ring, plane.normal);

            if (isConvex) {
                Polygon capPoly = new Polygon(ring, plane.normal);
                adapter.prepareCap(capPoly, plane, template);
                result.addAll(adapter.fromTemplate(capPoly, template));
            } else {
                List<List<ClipVertex>> triangles = earClip(ring, plane.normal);
                for (List<ClipVertex> tri : triangles) {
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

    // ========== 提取所有闭合环（Hierholzer + 拆分） ==========
    private static List<List<ClipVertex>> extractRings(List<Pair<ClipVertex, ClipVertex>> pairs) {
        if (pairs.isEmpty()) return Collections.emptyList();

        // 1. 坐标合并
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

        // 2. 构建邻接表（使用 List 保留平行边）
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

            // 检查度数偶数
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

            // 复制邻接表
            Map<ClipVertex, List<ClipVertex>> graphCopy = new HashMap<>();
            for (ClipVertex v : component) {
                graphCopy.put(v, new ArrayList<>(graph.get(v)));
            }

            // 反复提取环
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
                        ClipVertex next = graphCopy.get(current).remove(0);
                        graphCopy.get(next).remove(current);
                        current = next;
                    } else {
                        ring.add(current);
                        current = stack2.pop();
                    }
                }
                Collections.reverse(ring);
                if (!ring.isEmpty() && ring.get(0) == ring.get(ring.size() - 1)) {
                    ring.remove(ring.size() - 1);
                }

                if (ring.size() >= 3) {
                    // 拆分可能包含的多个环
                    allRings.addAll(splitRing(ring));
                }
            }

            globalVisited.addAll(component);
        }

        return allRings;
    }

    // ========== 拆分成多个简单环（基于坐标重复，递归） ==========
    private static List<List<ClipVertex>> splitRing(List<ClipVertex> ring) {
        List<List<ClipVertex>> result = new ArrayList<>();
        if (ring.size() < 3) {
            result.add(ring);
            return result;
        }

        // 1. 收集每个坐标出现的索引
        Map<Vec3d, List<Integer>> posIndices = new HashMap<>();
        for (int i = 0; i < ring.size(); i++) {
            Vec3d pos = ring.get(i).pos;
            posIndices.computeIfAbsent(pos, k -> new ArrayList<>()).add(i);
        }

        // 2. 筛选出出现次数 >=2 的坐标
        List<Map.Entry<Vec3d, List<Integer>>> duplicates = new ArrayList<>();
        for (Map.Entry<Vec3d, List<Integer>> entry : posIndices.entrySet()) {
            if (entry.getValue().size() >= 2) {
                duplicates.add(entry);
            }
        }

        if (duplicates.isEmpty()) {
            // 无重复，直接返回
            result.add(ring);
            return result;
        }

        // 3. 选择一个分割点：优先选择第一个重复点，且两个索引之间的间隔最大（避免分割出太小的环）
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

        // 4. 获取两个分割索引（第一个和第二个出现的索引）
        List<Integer> idxs = best.getValue();
        int first = idxs.get(0);
        int second = idxs.get(1);

        // 5. 分割成两个子环
        List<ClipVertex> sub1 = new ArrayList<>(ring.subList(first, second + 1));
        List<ClipVertex> sub2 = new ArrayList<>();
        sub2.addAll(ring.subList(second, ring.size()));
        sub2.addAll(ring.subList(0, first + 1));

        // 6. 去掉子环末尾的重复顶点（因为分割时包含了首尾重复）
        if (sub1.size() > 1 && sub1.get(0) == sub1.get(sub1.size() - 1)) {
            sub1.remove(sub1.size() - 1);
        }
        if (sub2.size() > 1 && sub2.get(0) == sub2.get(sub2.size() - 1)) {
            sub2.remove(sub2.size() - 1);
        }

        // 7. 确保子环大小严格小于原环，否则放弃（避免无限递归）
        if (sub1.size() >= ring.size() || sub2.size() >= ring.size()) {
            result.add(ring);
            return result;
        }

        // 8. 递归分割子环
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

    // ========== 面积和凸性判断 ==========
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

    // ========== 耳剪裁三角剖分 ==========
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

    private static boolean isConvexEar(ClipVertex a, ClipVertex b, ClipVertex c, Vec3d normal) {
        Vec3d ab = b.pos.subtract(a.pos);
        Vec3d bc = c.pos.subtract(b.pos);
        double cross = ab.crossProduct(bc).dotProduct(normal);
        return cross > 0;
    }

    private static boolean hasVerticesInside(ClipVertex a, ClipVertex b, ClipVertex c, List<ClipVertex> verts, Vec3d normal) {
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

    private static boolean pointInTriangle2D(double px, double py, double ax, double ay, double bx, double by, double cx, double cy) {
        double d1 = sign2D(px, py, ax, ay, bx, by);
        double d2 = sign2D(px, py, bx, by, cx, cy);
        double d3 = sign2D(px, py, cx, cy, ax, ay);
        boolean hasNeg = (d1 < 0) || (d2 < 0) || (d3 < 0);
        boolean hasPos = (d1 > 0) || (d2 > 0) || (d3 > 0);
        return !(hasNeg && hasPos);
    }

    private static double sign2D(double px, double py, double ax, double ay, double bx, double by) {
        return (px - bx) * (ay - by) - (ax - bx) * (py - by);
    }
}