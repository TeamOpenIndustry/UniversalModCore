package cam72cam.mod.render.cutter;

import java.util.ArrayList;
import java.util.List;

public class ClipResult {

    public final Polygon polygon = new Polygon();

    public final List<ClipVertex> intersections = new ArrayList<>();
}