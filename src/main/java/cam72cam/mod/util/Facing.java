package cam72cam.mod.util;

import cam72cam.mod.math.Rotation;
import net.minecraft.core.Direction;

/**
 * Wrap MC's EnumFacing enum, provide some helpers
 */
public enum Facing {
    DOWN(Direction.DOWN),
    UP(Direction.UP),
    NORTH(Direction.NORTH),
    SOUTH(Direction.SOUTH),
    WEST(Direction.WEST),
    EAST(Direction.EAST),
    ;

    // Note to self: Do not call Arrays.randomize on this, weird shit happens (as would be expected)
    public static final Facing[] HORIZONTALS = {
            NORTH, SOUTH, EAST, WEST
    };
    public final Direction internal;

    Facing(Direction internal) {
        this.internal = internal;
    }

    public static Facing from(Direction facing) {
        if (facing == null) {
            return null;
        }
        switch (facing) {
            case DOWN:
                return DOWN;
            case UP:
                return UP;
            case NORTH:
                return NORTH;
            case SOUTH:
                return SOUTH;
            case WEST:
                return WEST;
            case EAST:
                return EAST;
            default:
                return null;
        }
    }

    /** Older versions of MC used a single byte to represent facing */
    @Deprecated
    public static Facing from(byte facing) {
        return from(net.minecraft.core.Direction.from3DDataValue(facing));
    }

    /** 0 is SOUTH, 90 is WEST */
    public static Facing fromAngle(float v) {
        return from(Direction.fromYRot(v));
    }

    public Facing getOpposite() {
        switch (this) {
            case DOWN:
                return UP;
            case UP:
                return DOWN;
            case NORTH:
                return SOUTH;
            case SOUTH:
                return NORTH;
            case WEST:
                return EAST;
            case EAST:
                return WEST;
            default:
                return null;
        }
    }

    public Facing rotate(Rotation rot) {
        return Facing.from(rot.internal.rotate(this.internal));
    }

    public float getAngle() {
        return internal.toYRot();
    }

    /** Axis that this facing lies upon */
    public Axis getAxis() {
        return Axis.from(internal.getAxis());
    }

    /** @see cam72cam.mod.math.Vec3i#offset */
    public int getXMultiplier() {
        return internal.getStepX();
    }

    /** @see cam72cam.mod.math.Vec3i#offset */
    public int getYMultiplier() {
        return internal.getStepY();
    }

    /** @see cam72cam.mod.math.Vec3i#offset */
    public int getZMultiplier() {
        return internal.getStepZ();
    }
}
