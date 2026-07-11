package cam72cam.mod.util;

import cam72cam.mod.math.Vec3d;

public final class BlockDirectionUtil {

    private BlockDirectionUtil() {}

    /**
     * This convert Vec3d normal to minecraft block direction
     * */
    public static Facing fromNormal(Vec3d normal) {

        double ax = Math.abs(normal.x);
        double ay = Math.abs(normal.y);
        double az = Math.abs(normal.z);

        if (ax >= ay && ax >= az) {
            return normal.x >= 0
                    ? Facing.EAST
                    : Facing.WEST;
        }

        if (ay >= ax && ay >= az) {
            return normal.y >= 0
                    ? Facing.UP
                    : Facing.DOWN;
        }

        return normal.z >= 0
                ? Facing.SOUTH
                : Facing.NORTH;
    }
}