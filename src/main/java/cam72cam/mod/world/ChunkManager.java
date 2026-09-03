package cam72cam.mod.world;

import cam72cam.mod.math.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

/**
 * Internal, do not use directly<br>
 * No more need to save by ourselves since 1.21.5
 */
public class ChunkManager {
    private static final TicketType UMCTICKET = new TicketType(20, true, TicketType.TicketUse.LOADING_AND_SIMULATION);

    static void flagEntityPos(cam72cam.mod.world.World world, Vec3i inPos) {
        if (world.isClient) {
            return;
        }

        ChunkPos chunkpos = new ChunkPos(inPos.internal());
        ServerLevel server = (ServerLevel) world.internal;
        server.getChunkSource().addTicketWithRadius(UMCTICKET, chunkpos, 3);
    }
}
