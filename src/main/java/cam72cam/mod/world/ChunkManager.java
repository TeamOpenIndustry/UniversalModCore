package cam72cam.mod.world;

import cam72cam.mod.ModCore;
import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.serialization.TagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.Ticket;

import java.util.*;
import java.util.stream.Collectors;

/** Internal, do not use directly */
public class ChunkManager implements ForgeChunkManager.LoadingCallback {
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
        ModCore.debug("Setting up chunk loading...");
        if (instance == null) {
            instance = new ChunkManager();
            CommonEvents.World.TICK.subscribe(ChunkManager::onWorldTick);
            CommonEvents.World.UNLOAD.subscribe(ChunkManager::saveChunks);
        }
    }

    private static Ticket ticketForWorld(World world) {
        int dim = world.provider.getDimension();
        if (!TICKETS.containsKey(dim)) {
            TICKETS.put(dim, ForgeChunkManager.requestTicket(ModCore.instance, world, ForgeChunkManager.Type.NORMAL));
        }
        return TICKETS.get(dim);
    }

    static void flagEntityPos(cam72cam.mod.world.World world, Vec3i inPos) {
        if (world.isClient) {
            return;
        }

        ChunkPos pos = new ChunkPos(world.internal, inPos.internal());

        int currTicks = 0;

        if (CHUNK_MAP.containsKey(pos)) {
            currTicks = CHUNK_MAP.get(pos) + 1;
        } else {
            ModCore.debug("NEW CHUNK %s %s", pos.chunkX, pos.chunkZ);
        }
        // max 5s before unload
        CHUNK_MAP.put(pos, Math.max(10, Math.min(100, currTicks)));
    }

    private static void onWorldTick(World world) {
        Ticket ticket;
        try {
            ticket = ticketForWorld(world);
        } catch (Exception ex) {
            ModCore.error("Something broke inside ticketForWorld!");
            return;
        }

        int dim = world.provider.getDimension();
        Set<ChunkPos> keys = CHUNK_MAP.keySet();

        Set<ChunkPos> loaded = new HashSet<>();
        Set<ChunkPos> unload = new HashSet<>();

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

        for (net.minecraft.util.math.ChunkPos chunk : ticket.getChunkList()) {
            boolean shouldChunkLoad = false;

            for (ChunkPos pos : loaded) {
                if (chunk.x == pos.chunkX && chunk.z == pos.chunkZ) {
                    shouldChunkLoad = true;
                    loaded.remove(pos);
                    break;
                }
            }

            if (shouldChunkLoad) {
                // Leave chunk loaded
                //System.out.println(String.format("NOP CHUNK %s %s", chunk.x, chunk.z));
            } else {
                try {
                    ModCore.debug("UNFORCED CHUNK %s %s", chunk.x, chunk.z);
                    ForgeChunkManager.unforceChunk(ticket, chunk);
                    if (world instanceof WorldServer) {
                        if (!((WorldServer)world).getPlayerChunkMap().contains(chunk.x, chunk.z)) {
                            Chunk current = world.getChunkProvider().getLoadedChunk(chunk.x, chunk.z);
                            if (current != null) {
                                ModCore.debug("UNLOADED CHUNK %s %s", chunk.x, chunk.z);
                                ((WorldServer) world).getChunkProvider().queueUnload(current);
                            }
                        }
                    }
                } catch (Exception ex) {
                    ModCore.catching(ex);
                }
            }
        }

        for (ChunkPos pos : loaded) {
            ModCore.debug("FORCED CHUNK %s %s", pos.chunkX, pos.chunkZ);
            try {
                ForgeChunkManager.forceChunk(ticket, new net.minecraft.util.math.ChunkPos(pos.chunkX, pos.chunkZ));
            } catch (Exception ex) {
                ModCore.catching(ex);
            }
        }
        if (world.getTotalWorldTime() % 100 == 0) {
            ModCore.debug("Tracking %s loaded chunks", ticket.getChunkList().size());
            saveChunks(world);
        }
    }

    private ChunkManager() {
        if (!ForgeChunkManager.getConfig().hasCategory(ModCore.MODID)) {
            ForgeChunkManager.getConfig().get(ModCore.MODID, "maximumChunksPerTicket", 1000000).setMinValue(0);
            ForgeChunkManager.getConfig().save();
        }

        ForgeChunkManager.setForcedChunkLoadingCallback(ModCore.instance, this);
    }

    private static void saveChunks(World world) {
        Ticket ticket = ticketForWorld(world);
        int dim = world.provider.getDimension();
        TagCompound data = new TagCompound(ticket.getModData());
        data.setList("chunks", CHUNK_MAP.keySet().stream().filter(x -> x.dim == dim).collect(Collectors.toList()), cm -> {
            TagCompound chunk = new TagCompound();
            chunk.setInteger("cx", cm.chunkX);
            chunk.setInteger("cz", cm.chunkZ);
            return chunk;
        });
    }


    @Override
    public void ticketsLoaded(List<Ticket> tickets, World world) {
        int dim = world.provider.getDimension();
        ModCore.debug("Loading chunks for %s (%s tickets)", dim, tickets.size());

        CHUNK_MAP.keySet().stream().filter(x -> x.dim == dim).collect(Collectors.toList()).forEach(CHUNK_MAP::remove);

        TICKETS.remove(dim);
        if (tickets.size() == 1) {
            TICKETS.put(dim, tickets.get(0));
            TagCompound data = new TagCompound(tickets.get(0).getModData());
            if (data.hasKey("chunks")) {
                for (TagCompound chunk : data.getList("chunks", x -> x)) {
                    ModCore.debug("%s", chunk);
                    CHUNK_MAP.put(new ChunkPos(world, chunk.getInteger("cx"), chunk.getInteger("cz")), 100);
                }
            }
        } else {
            ModCore.warn("Got extra tickets!  Ignoring chunk ticket data");
        }
    }
}
