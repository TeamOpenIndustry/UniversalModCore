package cam72cam.mod.world;

import cam72cam.mod.math.Vec3i;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;

import java.util.Comparator;

/** Internal, do not use directly */
public class ChunkManager  {
    private static final ChunkTicketType<ChunkPos> UMCTICKET = ChunkTicketType.method_20628("universalmodcore", Comparator.comparingLong(net.minecraft.util.math.ChunkPos::toLong), 20);

    static void flagEntityPos(cam72cam.mod.world.World world, Vec3i inPos) {
        if (world.isClient) {
            return;
        }

        net.minecraft.util.math.ChunkPos chunkpos = new ChunkPos(inPos.internal());
        ServerWorld server = (ServerWorld) world.internal;
        server.getChunkManager().addTicket(UMCTICKET, chunkpos, 1, chunkpos);
    }

    public static void setup() {
        // Forces UMCTICKET registration
    }
}
