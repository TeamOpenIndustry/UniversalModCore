package cam72cam.mod.world;

import cam72cam.mod.ModCore;
import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.math.Vec3i;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.Ticket;
import net.minecraft.world.server.TicketType;

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
    private static final TicketType<ChunkPos> UMCTICKET = TicketType.create("universalmodcore", Comparator.comparingLong(ChunkPos::asLong), 20);

    static void flagEntityPos(cam72cam.mod.world.World world, Vec3i inPos) {
        if (world.isClient) {
            return;
        }

        ChunkPos chunkpos = new ChunkPos(inPos.internal());
        ServerWorld server = (ServerWorld) world.internal;
        server.getChunkProvider().registerTicket(UMCTICKET, chunkpos, 3, chunkpos);
    }

    private static Path chunkData(ServerWorld world) {
        File dir = new File(world.getDimension().getType().getDirectory(world.getSaveHandler().getWorldDirectory()), "umc");
        dir.mkdirs();
        return new File(dir, "chunks.raw").toPath();
    }

    public static void setup() {
        // Forces UMCTICKET registration
        CommonEvents.World.SAVE.subscribe(world -> {
            List<String> currentlyForced = new ArrayList<>();
            world.getChunkProvider().ticketManager.tickets.forEach((pos, tickets) -> {
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
            if (!world.isRemote) {
                try {
                    ServerWorld server = (ServerWorld) world;
                    for (String line : Files.readAllLines(chunkData(server))) {
                        ChunkPos chunkpos = new ChunkPos(Long.parseLong(line));
                        ModCore.debug("Remembered to keep chunk %s loaded", chunkpos);
                        server.getChunkProvider().registerTicket(UMCTICKET, chunkpos, 3, chunkpos);
                    }
                } catch (IOException e) {
                    ModCore.catching(e);
                }
            }
        });
    }
}
