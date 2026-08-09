package cam72cam.mod.render.cutter;

import cam72cam.mod.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public final class PolygonQuadBuilder {

    private PolygonQuadBuilder() {}

    public static List<Polygon> build(Polygon polygon) {
        List<Polygon> result = new ArrayList<>();
        List<ClipVertex> verts = new ArrayList<>(polygon.vertices);

        // 三角剖分（耳剪裁）
        List<List<ClipVertex>> triangles = earClip(verts);

        for (List<ClipVertex> tri : triangles) {
            Polygon quad = new Polygon();
            quad.vertices.add(tri.get(0));
            quad.vertices.add(tri.get(1));
            quad.vertices.add(tri.get(2));
            quad.vertices.add(tri.get(2)); // 复制最后一个顶点形成四边形
            result.add(quad);
        }
        return result;
    }

    // 简单耳剪裁三角剖分（假设多边形简单且凸/凹均可）
    private static List<List<ClipVertex>> earClip(List<ClipVertex> polygon) {
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

                // 检查是否为凸顶点（ear）
                if (isConvex(a, b, c) && !hasVerticesInside(a, b, c, verts)) {
                    triangles.add(List.of(a, b, c));
                    verts.remove(i);
                    found = true;
                    break;
                }
            }
            if (!found) {
                // 退化情况：强制取前三个
                triangles.add(List.of(verts.get(0), verts.get(1), verts.get(2)));
                verts.remove(1);
            }
        }
        if (verts.size() == 3) {
            triangles.add(List.of(verts.get(0), verts.get(1), verts.get(2)));
        }
        return triangles;
    }

    private static boolean isConvex(ClipVertex a, ClipVertex b, ClipVertex c) {
        // 二维叉积（假设多边形在XY平面，忽略Z）
        double cross = (b.pos.x - a.pos.x) * (c.pos.y - b.pos.y) -
                (b.pos.y - a.pos.y) * (c.pos.x - b.pos.x);
        return cross > 0; // 假设顶点顺序为逆时针
    }

    private static boolean hasVerticesInside(ClipVertex a, ClipVertex b, ClipVertex c,
                                             List<ClipVertex> verts) {
        for (ClipVertex v : verts) {
            if (v == a || v == b || v == c) continue;
            if (pointInTriangle(v.pos, a.pos, b.pos, c.pos)) {
                return true;
            }
        }
        return false;
    }

    private static boolean pointInTriangle(Vec3d p, Vec3d a, Vec3d b, Vec3d c) {
        // 重心法判断点是否在三角形内
        double d1 = sign(p, a, b);
        double d2 = sign(p, b, c);
        double d3 = sign(p, c, a);
        boolean hasNeg = (d1 < 0) || (d2 < 0) || (d3 < 0);
        boolean hasPos = (d1 > 0) || (d2 > 0) || (d3 > 0);
        return !(hasNeg && hasPos);
    }

    private static double sign(Vec3d p1, Vec3d p2, Vec3d p3) {
        return (p1.x - p3.x) * (p2.y - p3.y) - (p2.x - p3.x) * (p1.y - p3.y);
    }
}