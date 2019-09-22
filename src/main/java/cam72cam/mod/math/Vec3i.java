package cam72cam.mod.math;

import cam72cam.mod.util.Facing;

public class Vec3i {
    public static final Vec3i ZERO = new Vec3i(0,0,0);
    public final int x;
    public final int y;
    public final int z;

    public Vec3i(int posX, int posY, int  posZ) {
        this.x = posX;
        this.y = posY;
        this.z = posZ;
    }

    public Vec3i(Vec3d pos) {
        this((int)Math.floor(pos.x), (int)Math.floor(pos.y), (int)Math.floor(pos.z));
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Vec3i) {
            Vec3i p = (Vec3i) o;
            return x == p.x && y == p.y && z == p.z;
        }
        return false;
    }

    public Vec3i offset(Facing facing, int n) {
        return n == 0 ? this : new Vec3i(this.x + facing.internal.getFrontOffsetX() * n, this.y + facing.internal.getFrontOffsetY() * n, this.z + facing.internal.getFrontOffsetZ() * n);
    }

    public Vec3i offset(Facing facing) {
        return offset(facing, 1);
    }

    public Vec3i up() {
        return offset(Facing.UP);
    }

    public Vec3i down() {
        return offset(Facing.DOWN);
    }

    public Vec3i north() {
        return offset(Facing.NORTH);
    }

    public Vec3i east() {
        return offset(Facing.EAST);
    }

    public Vec3i south() {
        return offset(Facing.SOUTH);
    }

    public Vec3i west() {
        return offset(Facing.WEST);
    }

    public Vec3i up(int offset) {
        return offset(Facing.UP, offset);
    }

    public Vec3i down(int offset) {
        return offset(Facing.DOWN, offset);
    }

    public Vec3i north(int offset) {
        return offset(Facing.NORTH, offset);
    }

    public Vec3i east(int offset) {
        return offset(Facing.EAST, offset);
    }

    public Vec3i south(int offset) {
        return offset(Facing.SOUTH, offset);
    }

    public Vec3i west(int offset) {
        return offset(Facing.WEST, offset);
    }

    public Vec3i add(Vec3i other) {
        return add(other.x, other.y, other.z);
    }

    public Vec3i add(int x, int y, int z) {
        return new Vec3i(x + this.x, y + this.y, z + this.z);
    }

    public Vec3i subtract(Vec3i other) {
        return subtract(other.x, other.y, other.z);
    }

    public Vec3i subtract(int x, int y, int z) {
        return add(-x, -y, -z);
    }

    public Vec3i rotate(Rotation rotation) {
        switch (rotation)
        {
            case NONE:
            default:
                return this;
            case CLOCKWISE_90:
                return new Vec3i(-z, y, x);
            case CLOCKWISE_180:
                return new Vec3i(-x, y, -z);
            case COUNTERCLOCKWISE_90:
                return new Vec3i(z, y, -x);
        }
    }

    @Override
    public String toString() {
        return String.format("Vec3i: (%d, %d %d)", x, y, z);
    }

    @Override
    public int hashCode() {
        return (y + z * 31) * 31 + x;
    }

    public Vec3d toChunkMin() {
        return new Vec3d(x >> 4 << 4, 0, z >> 4 << 4);
    }

    public Vec3d toChunkMax() {
        return new Vec3d((x >> 4 << 4) + 16, Double.POSITIVE_INFINITY, (z >> 4 << 4) + 16);
    }
}
