package cam72cam.mod.world;

import cam72cam.mod.math.Vec3i;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.TicketType;

import java.util.Comparator;

/** Internal, do not use directly */
public class ChunkManager {
    private static final TicketType<ChunkPos> UMCTICKET = TicketType.func_223183_a("universalmodcore", Comparator.comparingLong(ChunkPos::asLong), 20);

    static void flagEntityPos(cam72cam.mod.world.World world, Vec3i inPos) {
        if (world.isClient) {
            return;
        }

        world.internal.getChunkAt(inPos.internal());
        ChunkPos chunkpos = new ChunkPos(inPos.internal());
        ServerWorld server = (ServerWorld) world.internal;
        server.getChunkProvider().func_217228_a(UMCTICKET, chunkpos, 3, chunkpos);
    }

    public static void setup() {
        // Forces UMCTICKET registration
    }
}
