package cam72cam.mod.math;

import cam72cam.mod.util.Facing;

public enum Rotation {
    NONE(),
    CLOCKWISE_90(),
    CLOCKWISE_180(),
    COUNTERCLOCKWISE_90();

    public static Rotation from(Facing facing) {
        switch (facing) {
            case NORTH:
                return Rotation.NONE;
            case EAST:
                return Rotation.CLOCKWISE_90;
            case SOUTH:
                return Rotation.CLOCKWISE_180;
            case WEST:
                return Rotation.COUNTERCLOCKWISE_90;
            default:
                return Rotation.NONE;
        }
    }
}
