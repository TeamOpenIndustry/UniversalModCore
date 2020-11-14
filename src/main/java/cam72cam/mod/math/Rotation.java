package cam72cam.mod.math;

import cam72cam.mod.util.Facing;
import net.minecraft.util.BlockRotation;

/** Represents a rotation around the Y axis */
public enum Rotation {
    NONE(BlockRotation.NONE),
    CLOCKWISE_90(BlockRotation.CLOCKWISE_90),
    CLOCKWISE_180(BlockRotation.CLOCKWISE_180),
    COUNTERCLOCKWISE_90(BlockRotation.COUNTERCLOCKWISE_90);
    public final BlockRotation internal;

    Rotation(BlockRotation internal) {
        this.internal = internal;
    }

    public static Rotation from(BlockRotation rot) {
        switch (rot) {
            case NONE:
                return NONE;
            case CLOCKWISE_90:
                return CLOCKWISE_90;
            case CLOCKWISE_180:
                return CLOCKWISE_180;
            case COUNTERCLOCKWISE_90:
                return COUNTERCLOCKWISE_90;
            default:
                return null;
        }
    }

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
