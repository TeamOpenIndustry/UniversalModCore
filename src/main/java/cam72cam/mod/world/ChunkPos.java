package cam72cam.mod.world;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

public class ChunkPos {
    public final int dim;
    public final int chunkX;
    public final int chunkZ;

    public ChunkPos(World world, Vec3i pos) {
        dim = world.provider.dimensionId;
        Chunk chunk = world.getChunkFromBlockCoords(pos.x, pos.z);
        chunkX = chunk.xPosition;
        chunkZ = chunk.zPosition;
    }

    public ChunkPos(Entity entity) {
        this(entity.worldObj, new Vec3i(entity.posX, entity.posY, entity.posZ));
    }

    public ChunkPos(World world, Integer cx, Integer cz) {
        dim = world.provider.dimensionId;
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

    public static long asLong(AxisAlignedBB aabb) {
        return asLong(new Vec3d((aabb.maxX + aabb.minX) / 2, (aabb.maxY + aabb.minY) / 2, (aabb.maxZ + aabb.minZ) / 2));
    }

    public static long asLong(Vec3d pos) {
        return asLong(MathHelper.floor_double(pos.x/16d), MathHelper.floor_double(pos.y/16d), MathHelper.floor_double(pos.z/16d));
    }

    public static long asLong(double x, double y, double z) {
        return asLong(MathHelper.floor_double(x/16d), MathHelper.floor_double(y/16d), MathHelper.floor_double(z/16d));
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