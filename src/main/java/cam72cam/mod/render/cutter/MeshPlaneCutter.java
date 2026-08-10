package cam72cam.mod.render.cutter;

import cam72cam.mod.render.cutter.adapter.PrimitiveAdapter;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public class MeshPlaneCutter {

    public static <T, Template> List<T> cut(
            List<T> primitives,
            Plane plane,
            PrimitiveAdapter<T, Template> adapter) {

        List<T> result = new ArrayList<>();
        List<ClipVertex> allIntersections = new ArrayList<>();

        for (T primitive : primitives) {
            Polygon polygon = adapter.toPolygon(primitive);
            ClipResult clipped = PolygonClipper.clip(polygon, plane);

            // Extract individual intersection points from pairs
            for (Pair<ClipVertex, ClipVertex> pair : clipped.getIntersections()) {
                allIntersections.add(pair.getLeft());
                allIntersections.add(pair.getRight());
            }

            if (clipped.getPolygon().getVertices().size() >= 3) {
                result.addAll(
                        adapter.fromPrimitive(
                                clipped.getPolygon(),
                                primitive
                        )
                );
            }
        }

        Polygon cap = CapBuilder.build(allIntersections, plane);

        if (cap != null) {
            Template template = adapter.createTemplate(primitives, plane);
            if (template != null) {
                adapter.prepareCap(cap, plane, template);
                result.addAll(adapter.fromTemplate(cap, template));
            }
        }

        return result;
    }
}