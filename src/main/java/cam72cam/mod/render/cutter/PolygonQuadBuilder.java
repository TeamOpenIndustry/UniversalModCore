package cam72cam.mod.render.cutter;

import java.util.ArrayList;
import java.util.List;

public final class PolygonQuadBuilder {

    private PolygonQuadBuilder() {}

    public static List<Polygon> build(Polygon polygon) {
        List<Polygon> result = new ArrayList<>();
        List<ClipVertex> verts = new ArrayList<>(polygon.getVertices());

        if (verts.size() > 4) {
            List<ClipVertex> quadVerts = new ArrayList<>(4);
            quadVerts.add(verts.get(0));
            quadVerts.add(verts.get(1));
            quadVerts.add(verts.get(2));
            quadVerts.add(verts.get(3));
            result.add(new Polygon(quadVerts, polygon.getNormal()));

            verts.remove(2);
            verts.remove(1);
        }

        if (verts.size() == 4) {
            result.add(new Polygon(verts, polygon.getNormal()));
        } else if (verts.size() == 3) {
            List<ClipVertex> quadVerts = new ArrayList<>(4);
            quadVerts.add(verts.get(0));
            quadVerts.add(verts.get(1));
            quadVerts.add(verts.get(2));
            quadVerts.add(verts.get(2)); // duplicate last to make quad
            result.add(new Polygon(quadVerts, polygon.getNormal()));
        }

        return result;
    }
}