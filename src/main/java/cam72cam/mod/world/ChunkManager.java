package cam72cam.mod.world;

import cam72cam.mod.ModCore;
import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.math.Vec3i;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.Ticket;

import java.util.*;

/** Internal, do not use directly */
public class ChunkManager implements ForgeChunkManager.LoadingCallback, ForgeChunkManager.OrderedLoadingCallback {
    /*
     * This takes a similar approach to FTBUtilities
     * One massive ticket for each dim
     *
     * CHUNK_MAP is a TLRU like structure keeping track of chunks in use from
     * server entities point of view.
     *
     * This is used in internal onTick to force/unforce chunks
     */

    private static final Map<Integer, Ticket> TICKETS = new HashMap<>();
    private static final Map<ChunkPos, Integer> CHUNK_MAP = new HashMap<>();


    private static ChunkManager instance;


    public static void setup() {
        if (instance == null) {
            instance = new ChunkManager();
            instance.init();
        }
        CommonEvents.World.TICK.subscribe(ChunkManager::onWorldTick);
    }

    private static Ticket ticketForWorld(World world) {
        int dim = world.provider.dimensionId;
        if (!TICKETS.containsKey(dim)) {
            TICKETS.put(dim, ForgeChunkManager.requestTicket(ModCore.instance, world, ForgeChunkManager.Type.NORMAL));
        }
        return TICKETS.get(dim);
    }

    static void flagEntityPos(cam72cam.mod.world.World world, Vec3i inPos) {
        if (world.isClient) {
            return;
        }

        ChunkPos pos = new ChunkPos(world.internal, inPos);

        int currTicks = 0;

        if (CHUNK_MAP.containsKey(pos)) {
            currTicks = CHUNK_MAP.get(pos) + 1;
        } else {
            ModCore.debug("NEW CHUNK %s %s", pos.chunkX, pos.chunkZ);
        }
        // max 5s before unload
        CHUNK_MAP.put(pos, Math.max(100, Math.min(10, currTicks)));
    }

    private static void onWorldTick(World world) {
        Ticket ticket;
        try {
            ticket = ticketForWorld(world);
        } catch (Exception ex) {
            ModCore.error("Something broke inside ticketForWorld!");
            return;
        }

            int dim = world.provider.dimensionId;
            Set<ChunkPos> keys = CHUNK_MAP.keySet();

            Set<ChunkPos> loaded = new HashSet<ChunkPos>();
            Set<ChunkPos> unload = new HashSet<ChunkPos>();

            for (ChunkPos pos : keys) {
                if (pos.dim != dim) {
                    continue;
                }

                int ticks = CHUNK_MAP.get(pos);

                if (ticks > 0) {
                    loaded.add(pos);
                    CHUNK_MAP.put(pos, ticks - 1);
                } else {
                    unload.add(pos);
                }
            }

            for (ChunkPos pos : unload) {
                CHUNK_MAP.remove(pos);
            }

            for (ChunkCoordIntPair chunk : ticket.getChunkList()) {
                boolean shouldChunkLoad = false;

                for (ChunkPos pos : loaded) {
                    if (chunk.chunkXPos == pos.chunkX && chunk.chunkZPos == pos.chunkZ) {
                        shouldChunkLoad = true;
                        loaded.remove(pos);
                        break;
                    }
                }

                if (shouldChunkLoad) {
                    // Leave chunk loaded
                    //System.out.println(String.format("NOP CHUNK %s %s", chunk.x, chunk.z));
                } else {
                    ModCore.debug("UNLOADED CHUNK %s %s", chunk.chunkXPos, chunk.chunkZPos);
                    try {
                        ForgeChunkManager.unforceChunk(ticket, chunk);
                    } catch (Exception ex) {
                        ModCore.catching(ex);
                    }
                }
            }

            for (ChunkPos pos : loaded) {
                ModCore.debug("LOADED CHUNK %s %s", pos.chunkX, pos.chunkZ);
                try {
                    ForgeChunkManager.forceChunk(ticket, new ChunkCoordIntPair(pos.chunkX, pos.chunkZ));
                } catch (Exception ex) {
                    ModCore.catching(ex);
                }
            }
    }

    private void init() {
        if (!ForgeChunkManager.getConfig().hasCategory(ModCore.MODID)) {
            ForgeChunkManager.getConfig().get(ModCore.MODID, "maximumChunksPerTicket", 1000000).setMinValue(0);
            ForgeChunkManager.getConfig().save();
        }

        ForgeChunkManager.setForcedChunkLoadingCallback(ModCore.instance, this);
    }

    @Override
    public List<Ticket> ticketsLoaded(List<Ticket> loaded_tickets, World world, int maxTicketCount) {
        return Collections.emptyList();
    }

    @Override
    public void ticketsLoaded(List<Ticket> tickets, World world) {
        int dim = world.provider.dimensionId;
        TICKETS.remove(dim);

        if (tickets.size() == 1) {
            TICKETS.put(dim, tickets.get(0));
        }
    }
}
