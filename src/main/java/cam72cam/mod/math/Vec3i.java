package cam72cam.mod.math;

import cam72cam.mod.util.Facing;

public class Vec3i {
    // Why is this all ClientOnly???
    private static final int[] multiplyDeBruijnBitPosition = new int[] {0, 1, 28, 2, 29, 14, 24, 3, 30, 22, 20, 15, 25, 17, 4, 8, 31, 27, 13, 23, 21, 19, 16, 7, 26, 12, 18, 6, 11, 5, 10, 9};
    private static boolean isPowerOfTwo(int p_151235_0_)
    {
        return p_151235_0_ != 0 && (p_151235_0_ & p_151235_0_ - 1) == 0;
    }
    public static int roundUpToPowerOfTwo(int p_151236_0_)
    {
        int j = p_151236_0_ - 1;
        j |= j >> 1;
        j |= j >> 2;
        j |= j >> 4;
        j |= j >> 8;
        j |= j >> 16;
        return j + 1;
    }
    private static int calculateLogBaseTwoDeBruijn(int p_151241_0_)
    {
        p_151241_0_ = isPowerOfTwo(p_151241_0_) ? p_151241_0_ : roundUpToPowerOfTwo(p_151241_0_);
        return multiplyDeBruijnBitPosition[(int)((long)p_151241_0_ * 125613361L >> 27) & 31];
    }
    public static int calculateLogBaseTwo(int p_151239_0_)
    {
        /**
         * Uses a B(2, 5) De Bruijn sequence and a lookup table to efficiently calculate the log-base-two of the given
         * value.  Optimized for cases where the input value is a power-of-two.  If the input value is not a power-of-
         * two, then subtract 1 from the return value.
         */
        return calculateLogBaseTwoDeBruijn(p_151239_0_) - (isPowerOfTwo(p_151239_0_) ? 0 : 1);
    }


    private static final int NUM_X_BITS = 1 + calculateLogBaseTwo(roundUpToPowerOfTwo(30000000));
    private static final int NUM_Z_BITS = NUM_X_BITS;
    private static final int NUM_Y_BITS = 64 - NUM_X_BITS - NUM_Z_BITS;
    private static final int Y_SHIFT = 0 + NUM_Z_BITS;
    private static final int X_SHIFT = Y_SHIFT + NUM_Y_BITS;
    private static final long X_MASK = (1L << NUM_X_BITS) - 1L;
    private static final long Y_MASK = (1L << NUM_Y_BITS) - 1L;
    private static final long Z_MASK = (1L << NUM_Z_BITS) - 1L;


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

    @Deprecated
    public Vec3i(long serialized) {
        x = (int)(serialized << 64 - X_SHIFT - NUM_X_BITS >> 64 - NUM_X_BITS);
        y = (int)(serialized << 64 - Y_SHIFT - NUM_Y_BITS >> 64 - NUM_Y_BITS);
        z = (int)(serialized << 64 - NUM_Z_BITS >> 64 - NUM_Z_BITS);
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

    @Deprecated
    public long toLong() {
        return ((long)x & X_MASK) << X_SHIFT | ((long)y & Y_MASK) << Y_SHIFT | ((long)z & Z_MASK) << 0;
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
