package cam72cam.mod.render.cutter;

import cam72cam.mod.render.cutter.adapter.PrimitiveAdapter;

import java.util.ArrayList;
import java.util.List;
public class MeshPlaneCutter {

    public static <T, Template> List<T> cut(
            List<T> primitives,
            Plane plane,
            PrimitiveAdapter<T, Template> adapter) {

        List<T> result = new ArrayList<>();

        List<ClipVertex> intersections = new ArrayList<>();

        for (T primitive : primitives) {

            Polygon polygon = adapter.toPolygon(primitive);

            ClipResult clipped = PolygonClipper.clip(polygon, plane);

            intersections.addAll(clipped.intersections);

            if (clipped.polygon.vertices.size() >= 3) {

                result.addAll(
                        adapter.fromPrimitive(
                                clipped.polygon,
                                primitive
                        )
                );
            }
        }

        Polygon cap = CapBuilder.build(intersections, plane);

        if (cap != null) {

            Template template = adapter.createTemplate(primitives, plane);

            if (template != null) {

                adapter.prepareCap(
                        cap,
                        plane,
                        template
                );

                result.addAll(adapter.fromTemplate(cap, template));
            }
        }

        return result;
    }
}