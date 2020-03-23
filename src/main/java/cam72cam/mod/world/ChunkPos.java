package cam72cam.mod.world;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.Loader;

public class ChunkPos {
    public final int dim;
    public final int chunkX;
    public final int chunkY;
    public final int chunkZ;

    public ChunkPos(World world, BlockPos pos) {
        dim = world.provider.getDimension();
        chunkX = pos.getX() >> 4;
        chunkY = Loader.isModLoaded("CubicChunks") ? pos.getY() >> 4 : 0;
        chunkZ = pos.getZ() >> 4;
    }

    public ChunkPos(Entity entity) {
        this(entity.getEntityWorld(), entity.getPosition());
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ChunkPos) {
            ChunkPos other = (ChunkPos) o;
            return other.dim == dim && other.chunkX == chunkX && other.chunkZ == chunkZ && other.chunkY == chunkY;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return dim + chunkX + chunkZ + chunkZ;
    }
}