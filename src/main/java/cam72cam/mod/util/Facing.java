package cam72cam.mod.util;

import cam72cam.mod.math.Rotation;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * Wrap MC's EnumFacing enum, provide some helpers
 */
public enum Facing {
    DOWN(EnumFacing.DOWN),
    UP(EnumFacing.UP),
    NORTH(EnumFacing.NORTH),
    SOUTH(EnumFacing.SOUTH),
    WEST(EnumFacing.WEST),
    EAST(EnumFacing.EAST),
    ;

    // Note to self: Do not call Arrays.randomize on this, weird shit happens (as would be expected)
    public static final Facing[] HORIZONTALS = {
            NORTH, SOUTH, EAST, WEST
    };
    public final EnumFacing internal;

    Facing(EnumFacing internal) {
        this.internal = internal;
    }

    public static Facing from(EnumFacing facing) {
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
    public static Facing from(ForgeDirection dir) {
        switch (dir) {
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

    public ForgeDirection to() {
        switch (this) {
            case DOWN:
                return ForgeDirection.DOWN;
            case UP:
                return ForgeDirection.UP;
            case NORTH:
                return ForgeDirection.NORTH;
            case SOUTH:
                return ForgeDirection.SOUTH;
            case WEST:
                return ForgeDirection.WEST;
            case EAST:
                return ForgeDirection.EAST;
            default:
                return null;
        }
    }

    /** Older versions of MC used a single byte to represent facing */
    @Deprecated
    public static Facing from(byte facing) {
        return from(net.minecraft.util.EnumFacing.getFront(facing));
    }

    /** 0 is SOUTH, 90 is WEST */
    public static Facing fromAngle(float v) {
        switch (MathHelper.floor_double(v / 90.0D + 0.5D) & 3) {
            case 0:
                return SOUTH;
            case 1:
                return WEST;
            case 2:
                return NORTH;
            case 3:
                return EAST;
            default:
                return NORTH;
        }
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
                return this;
        }
    }

    public Facing rotate(Rotation rot) {
        switch (rot) {
            case NONE:
                return this;
            case CLOCKWISE_90:
                switch (this) {
                    case NORTH:
                        return EAST;
                    case SOUTH:
                        return WEST;
                    case WEST:
                        return NORTH;
                    case EAST:
                        return SOUTH;
                }
                break;
            case CLOCKWISE_180:
                return getOpposite();
            case COUNTERCLOCKWISE_90:
                return getOpposite().rotate(Rotation.CLOCKWISE_90);
        }
        return this;
    }

    public float getAngle() {
        int horizontalIndex = -1;
        switch (this) {
            case NORTH:
                horizontalIndex = 2;
                break;
            case SOUTH:
                horizontalIndex = 0;
                break;
            case WEST:
                horizontalIndex = 1;
                break;
            case EAST:
                horizontalIndex = 3;
                break;
        }
        return (float)((horizontalIndex & 3) * 90);
    }

    /** Axis that this facing lies upon */
    public Axis getAxis() {
        switch (this) {
            case DOWN:
            case UP:
                return Axis.Y;
            case NORTH:
            case SOUTH:
                return Axis.X;
            case WEST:
            case EAST:
                return Axis.Z;
        }
        return Axis.Y;
    }

    /** @see cam72cam.mod.math.Vec3i#offset */
    public int getXMultiplier() {
        return internal.getFrontOffsetX();
    }

    /** @see cam72cam.mod.math.Vec3i#offset */
    public int getYMultiplier() {
        return internal.getFrontOffsetY();
    }

    /** @see cam72cam.mod.math.Vec3i#offset */
    public int getZMultiplier() {
        return internal.getFrontOffsetZ();
    }
}
