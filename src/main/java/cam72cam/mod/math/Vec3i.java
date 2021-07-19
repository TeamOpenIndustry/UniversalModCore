package cam72cam.mod.math;

import cam72cam.mod.util.Facing;
import net.minecraft.util.math.BlockPos;

public class Vec3i {
    public static final Vec3i ZERO = new Vec3i(BlockPos.ZERO);
    private BlockPos internal = null;
    public final int x;
    public final int y;
    public final int z;

    public Vec3i(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vec3i(double x, double y, double z) {
        int xi = (int) x;
        int yi = (int) y;
        int zi = (int) z;
        if (xi > x) { xi -= 1; }
        if (yi > y) { yi -= 1; }
        if (zi > z) { zi -= 1; }
        this.x = xi;
        this.y = yi;
        this.z = zi;
    }

    public Vec3i(BlockPos pos) {
        this(pos.getX(), pos.getY(), pos.getZ());
        internal = pos;
    }

    public Vec3i(Vec3d pos) {
        this(pos.x, pos.y, pos.z);
    }

    @Deprecated
    public Vec3i(long serialized) {
        this(BlockPos.of(serialized));
    }

    public Vec3i offset(Facing facing, int offset) {
        if (offset == 0) {
            return this;
        }
        return new Vec3i(
                this.x + facing.getXMultiplier() * offset,
                this.y + facing.getYMultiplier() * offset,
                this.z + facing.getZMultiplier() * offset
        );
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

    public Vec3i add(int x, int y, int z) {
        return new Vec3i(this.x + x, this.y + y, this.z + z);
    }

    public Vec3i add(Vec3i other) {
        return add(other.x, other.y, other.z);
    }

    public Vec3i subtract(int x, int y, int z) {
        return add(-x, -y, -z);
    }

    public Vec3i subtract(Vec3i other) {
        return subtract(other.x, other.y, other.z);
    }

    @Deprecated
    public long toLong() {
        return internal().asLong();
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

    public BlockPos internal() {
        if (internal == null) {
            internal = new BlockPos(x, y, z);
        }
        return internal;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Vec3i) {
            Vec3i ov = (Vec3i) other;
            return ov.x == this.x && ov.y == this.y && ov.z == this.z;
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("(%s, %s, %s)", this.x, this.y, this.z);
    }

    @Override
    public int hashCode() {
        return (this.y + this.z * 31) * 31 + this.x;
    }

    public Vec3d toChunkMin() {
        return new Vec3d(x >> 4 << 4, 0, z >> 4 << 4);
    }

    public Vec3d toChunkMax() {
        return new Vec3d((x >> 4 << 4) + 16, Double.POSITIVE_INFINITY, (z >> 4 << 4) + 16);
    }
}
