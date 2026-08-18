package cam72cam.mod.render.cutter;

import cam72cam.mod.math.Plane;

import java.util.List;

public interface PrimitiveAdapter<T, Template> {

    Polygon toPolygon(T primitive);

    List<T> fromPrimitive(Polygon polygon, T primitive);

    List<T> fromTemplate(Polygon polygon, Template template);

    Template createTemplate(List<T> primitives, Plane plane);

    void prepareCap(Polygon polygon, Plane plane, Template template);
}