package cam72cam.mod.render.cutter;

import java.util.ArrayList;
import java.util.List;

public final class VertexDeduplicator {

    private static final double EPS = 1E-5;

    private VertexDeduplicator() {}

    public static List<ClipVertex> deduplicate(
            List<ClipVertex> input) {

        List<ClipVertex> result = new ArrayList<>();

        for (ClipVertex v : input) {

            boolean exists = false;

            for (ClipVertex old : result) {

                if (samePosition(v, old)) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                result.add(v.copy());
            }
        }

        return result;
    }

    private static boolean samePosition(
            ClipVertex a,
            ClipVertex b) {

        return Math.abs(a.pos.x - b.pos.x) < EPS
                && Math.abs(a.pos.y - b.pos.y) < EPS
                && Math.abs(a.pos.z - b.pos.z) < EPS;
    }
}