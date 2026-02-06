package cam72cam.mod.world;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

public class ChunkPos {
    public final int dim;
    public final int chunkX;
    public final int chunkZ;

    public ChunkPos(World world, BlockPos pos) {
        dim = world.provider.getDimension();
        Chunk chunk = world.getChunkFromBlockCoords(pos);
        chunkX = chunk.xPosition;
        chunkZ = chunk.zPosition;
    }

    public ChunkPos(Entity entity) {
        this(entity.getEntityWorld(), entity.getPosition());
    }

    public ChunkPos(World world, Integer cx, Integer cz) {
        dim = world.provider.getDimension();
        chunkX = cx;
        chunkZ = cz;
    }

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

    public static long asLongExcludeY(Vec3d pos) {
        return asLong(new Vec3d(pos.xCoord, 0, pos.zCoord));
    }

    public static long asLong(Vec3d pos) {
        return asLong(MathHelper.floor(pos.xCoord/16d), MathHelper.floor(pos.yCoord/16d), MathHelper.floor(pos.zCoord/16d));
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

    @Override
    public boolean equals(Object o) {
        if (o instanceof ChunkPos) {
            ChunkPos other = (ChunkPos) o;
            return other.dim == dim && other.chunkX == chunkX && other.chunkZ == chunkZ;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return dim + chunkX + chunkZ;
    }
}