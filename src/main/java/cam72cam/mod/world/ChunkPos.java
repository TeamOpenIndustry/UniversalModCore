package cam72cam.mod.world;

import cam72cam.mod.math.Vec3i;
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