package cam72cam.mod.world;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicTicket;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldServer;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraftforge.common.ForgeChunkManager;
import cam72cam.mod.ModCore;
import cam72cam.mod.math.Vec3i;
import net.minecraft.world.World;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;

public class CubicChunks {
    public static boolean isCubicWorld(World world) {
        return world instanceof ICubicWorldServer && ((ICubicWorldServer) world).isCubicWorld();
    }

    private static void handleLoaded(World world, ForgeChunkManager.Ticket ticket, Set<ChunkPos> loaded) {
        Map<net.minecraft.util.math.ChunkPos, IntSet> forced = ((ICubicTicket) ticket).getAllForcedChunkCubes();
        for (Map.Entry<net.minecraft.util.math.ChunkPos, IntSet> entry : forced.entrySet()) {
            List<Vec3i> chunks = entry.getValue().stream().map(y -> new Vec3i(entry.getKey().x, y, entry.getKey().z)).collect(Collectors.toList());

            boolean shouldChunkLoad = false;

            for (Vec3i chunk : chunks) {

                for (ChunkPos pos : loaded) {
                    if (chunk.x == pos.chunkX && chunk.y == pos.chunkY && chunk.z == pos.chunkZ) {
                        shouldChunkLoad = true;
                        loaded.remove(pos);
                        break;
                    }
                }

                if (shouldChunkLoad) {
                    // Leave chunk loaded
                    //System.out.println(String.format("NOP CHUNK %s %s", chunk.x, chunk.z));
                } else {
                    ModCore.debug("UNLOADED CHUNK %s %s", chunk.x, chunk.z);
                    try {
                        ((ICubicWorldServer)world).unforceChunk(ticket, new CubePos(chunk.x, chunk.y, chunk.z));
                    } catch (Exception ex) {
                        ModCore.catching(ex);
                    }
                }
            }
        }

        for (ChunkPos pos : loaded) {
            ModCore.debug("LOADED CHUNK %s %s", pos.chunkX, pos.chunkZ);
            try {
                ((ICubicWorldServer)world).forceChunk(ticket, new CubePos(pos.chunkX, pos.chunkY, pos.chunkZ));
            } catch (Exception ex) {
                ModCore.catching(ex);
            }
        }
    }
}
