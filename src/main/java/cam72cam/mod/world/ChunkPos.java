package cam72cam.mod.world;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;

public class ChunkPos {
    //Minecraft(1.17+) way of storing ChunkPos, added Y axis than 1.12 ChunkPos
    //Backported for unified storaging way
    //Don't mix this up with net.minecraft.util.math.ChunkPos
    public static long asLong(int x, int y, int z) {
        long i = 0L;
        i |= ((long)x & 4194303L) << 42;
        i |= ((long)y & 1048575L) << 0;
        i |= ((long)z & 4194303L) << 20;
        return i;
    }

    //For 1.16- It turned out processing Y will take much more unnecessary effort...
    public static long asLongExcludeY(BlockPos pos) {
        return asLong(new BlockPos(pos.getX(), 0, pos.getZ()));
    }

    public static long asLong(BlockPos pos) {
        return asLong(MathHelper.floor(pos.getX()/16d), MathHelper.floor(pos.getY()/16d), MathHelper.floor(pos.getZ()/16d));
    }

    public static long asLongExcludeY(Vector3d pos) {
        return asLong(new Vector3d(pos.x, 0, pos.z));
    }

    public static long asLong(Vector3d pos) {
        return asLong(MathHelper.floor(pos.x/16d), MathHelper.floor(pos.y/16d), MathHelper.floor(pos.z/16d));
    }

    public static int x(long packed) {
        return (int) (packed << 0 >> 42);
    }

    public static int y(long packed) {
        return (int) (packed << 44 >> 44);
    }

    public static int z(long packed) {
        return (int) (packed << 22 >> 42);
    }
}
