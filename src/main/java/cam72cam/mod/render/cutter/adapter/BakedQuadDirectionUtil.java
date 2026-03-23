package cam72cam.mod.render.cutter.adapter;

import cam72cam.mod.math.Vec3d;
import net.minecraft.core.Direction;

public final class BakedQuadDirectionUtil {

    private BakedQuadDirectionUtil() {}

    public static Direction fromNormal(Vec3d normal) {

        double ax = Math.abs(normal.x);
        double ay = Math.abs(normal.y);
        double az = Math.abs(normal.z);

        if (ax >= ay && ax >= az) {
            return normal.x >= 0
                    ? Direction.EAST
                    : Direction.WEST;
        }

        if (ay >= ax && ay >= az) {
            return normal.y >= 0
                    ? Direction.UP
                    : Direction.DOWN;
        }

        return normal.z >= 0
                ? Direction.SOUTH
                : Direction.NORTH;
    }
}