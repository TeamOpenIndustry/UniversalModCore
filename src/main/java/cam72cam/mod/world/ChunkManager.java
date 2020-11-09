package cam72cam.mod.world;

import cam72cam.mod.math.Vec3i;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.TicketType;

import java.util.Comparator;

/** Internal, do not use directly */
public class ChunkManager {
    private static final TicketType<ChunkPos> UMCTICKET = TicketType.create("universalmodcore", Comparator.comparingLong(ChunkPos::asLong), 20);

    static void flagEntityPos(cam72cam.mod.world.World world, Vec3i inPos) {
        if (world.isClient) {
            return;
        }

        ChunkPos chunkpos = new ChunkPos(inPos.internal());
        ServerWorld server = (ServerWorld) world.internal;
        server.getChunkProvider().registerTicket(UMCTICKET, chunkpos, 1, chunkpos);
    }

    public static void setup() {
        // Forces UMCTICKET registration
    }
}
