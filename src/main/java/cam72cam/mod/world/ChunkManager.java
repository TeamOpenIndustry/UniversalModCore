package cam72cam.mod.world;

import cam72cam.mod.ModCore;
import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.math.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Internal, do not use directly
 */
public class ChunkManager {
    private static final TicketType<ChunkPos> UMCTICKET = TicketType.create("universalmodcore", Comparator.comparingLong(ChunkPos::toLong), 20);

    static void flagEntityPos(cam72cam.mod.world.World world, Vec3i inPos) {
        if (world.isClient) {
            return;
        }

        ChunkPos chunkpos = new ChunkPos(inPos.internal());
        ServerLevel server = (ServerLevel) world.internal;
        server.getChunkSource().addRegionTicket(UMCTICKET, chunkpos, 3, chunkpos, true);
    }

    private static Path chunkData(ServerLevel world) {
        File dir = new File(world.getServer().storageSource.getDimensionPath(world.dimension()).toString(), "umc");
        dir.mkdirs();
        return new File(dir, "chunks.raw").toPath();
    }

    public static void setup() {
        // Forces UMCTICKET registration
        CommonEvents.World.SAVE.subscribe(world -> {
            List<String> currentlyForced = new ArrayList<>();
            world.getChunkSource().distanceManager.tickets.forEach((pos, tickets) -> {
                for (Ticket<?> ticket : tickets) {
                    if (ticket.getType() == UMCTICKET) {
                        ModCore.debug("Remembering to keep chunk %s loaded", new ChunkPos(pos));
                        currentlyForced.add(pos.toString());
                    }
                }
            });
            try {
                Files.write(chunkData(world), currentlyForced, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                ModCore.catching(e);
            }
        });
        CommonEvents.World.LOAD.subscribe(world -> {
            if (!world.isClientSide) {
                try {
                    ServerLevel server = (ServerLevel) world;
                    Path dataFile = chunkData(server);
                    if (dataFile.toFile().exists()) {
                        for (String line : Files.readAllLines(dataFile)) {
                            ChunkPos chunkpos = new ChunkPos(Long.parseLong(line));
                            ModCore.debug("Remembered to keep chunk %s loaded", chunkpos);
                            server.getChunkSource().addRegionTicket(UMCTICKET, chunkpos, 3, chunkpos, true);
                        }
                    }
                } catch (IOException e) {
                    ModCore.catching(e);
                }
            }
        });
    }
}
