package cam72cam.mod.util;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import cam72cam.mod.ModCore;
import cam72cam.mod.block.BlockEntity;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.ModdedEntity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.text.Command;
import cam72cam.mod.text.PlayerMessage;
import cam72cam.mod.world.World;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.ForgeChunkManager;

public class ModCoreCommand extends Command {
    @Override
    public String getPrefix() {
        return ModCore.MODID;
    }

    @Override
    public String getUsage() {
        return "Usage: " + ModCore.MODID + " entity list [server dim] | chunk [[list|debug] [all|cx cz]] [server dim] | ticket [list|debug] [server dim]";
    }


    @Override
	public int getRequiredPermissionLevel() {
		return PermissionLevel.LEVEL4;
	}

	@Override
	public boolean execute(Consumer<PlayerMessage> sender, Optional<Player> player, String[] rawArgs) {
		List<String> args = new ArrayList<>(Arrays.asList(rawArgs));

		World world;
		if (player.isPresent()) {
			world = player.get().getWorld();
		} else {
			try {
				int dimId = Integer.parseInt(args.remove(args.size()-1));
				world = World.get(dimId, false);
				if (world == null) {
					sender.accept(PlayerMessage.direct(String.format("Dimension '%d' is not loaded or does not exist.", dimId)));
					return false;
				}
			} catch (IndexOutOfBoundsException | NumberFormatException ex) {
				sender.accept(PlayerMessage.direct("Dimension must be a number!"));
				return false;
			}
		}

		if (args.isEmpty()) {
			return false;
		}

		String cmd = args.remove(0);
		switch (cmd) {
			case "entity":
				if (args.isEmpty()) {
					return false;
				}
				String list = args.remove(0);
				if (list.equals("list")) {
					sendWorldEntities(world, sender);
					return true;
				}
				return false;
			case "chunk":
				return sendChunkInfo(world, sender, player, args);
			case "ticket":
				return sendTicketInfo(world, sender, player, args);
			default:
				return false;
		}
	}

	private String ticketIdentifier(ForgeChunkManager.Ticket ticket) {
		if (ticket.isPlayerTicket()) {
			return String.format("%s:%s", ticket.getModId(), ticket.getPlayerName());
		} else {
			return ticket.getModId();
		}
	}

	private boolean sendTicketInfo(World world, Consumer<PlayerMessage> sender, Optional<Player> player, List<String> args) {
		boolean list = false;
		boolean debug = false;
		if (args.size() > 0) {
			switch (args.remove(0)) {
				case "list":
					list = true;
					break;
				case "debug":
					debug = true;
					break;
				default:
					return false;
			}
		}

		sender.accept(PlayerMessage.direct(String.format(
				"%s forced chunks in %s",
				ForgeChunkManager.getPersistentChunksFor(world.internal).size(), world.getId()
		)));

		List<ForgeChunkManager.Ticket> tickets = new ArrayList<>(new HashSet<>(ForgeChunkManager.getPersistentChunksFor(world.internal).values()));
		tickets.sort(Comparator.comparing(this::ticketIdentifier));

		if (list || debug) {
			for (ForgeChunkManager.Ticket ticket : tickets) {
				sender.accept(PlayerMessage.direct(String.format("%s : %s forced", ticketIdentifier(ticket), ticket.getChunkList().size())));
				if (debug) {
					for (ChunkPos chunkPos : ticket.getChunkList()) {
						sender.accept(PlayerMessage.direct(String.format("  x=%s y=%s", chunkPos.x, chunkPos.z)));
					}
				}
			}
		}

		return true;
	}

	private boolean sendChunkInfo(World world, Consumer<PlayerMessage> sender, Optional<Player> player, List<String> args) {
		boolean list = false;
		boolean debug = false;
		boolean evict = false;
		boolean all = false;
		Integer cx = null;
		Integer cz = null;
		if (args.size() > 0) {
			switch (args.remove(0)) {
				case "list":
					list = true;
					break;
				case "debug":
					debug = true;
					break;
				case "evict":
					evict = true;
					break;
				default:
					return false;
			}

			if (!args.isEmpty()) {
				if (args.get(0).equals("all")) {
					args.remove(0);
					all = true;
				}
			}

			if (args.size() >= 2) {
				if (player.isPresent() && args.get(0).equals("~") && args.get(1).equals("~")) {
					Vec3d chunkPos = player.get().getBlockPosition().toChunkMin();
					cx = (int)chunkPos.x/16;
					cz = (int)chunkPos.z/16;
				} else {
					try {
						cx = Integer.parseInt(args.get(0));
						cz = Integer.parseInt(args.get(1));
					} catch (NumberFormatException ex) {
						sender.accept(PlayerMessage.direct("Expected integer chunk arguments"));
						return false;
					}
				}
			}
		}


		ChunkProviderServer provider = (ChunkProviderServer) world.internal.getChunkProvider();
		List<Chunk> chunks = provider.getLoadedChunks().stream().filter(Chunk::isLoaded).sorted(Comparator.comparingInt(a -> a.x * 1000000 + a.z)).collect(Collectors.toList());
		int evicted = 0;
		if (evict) {
			for (Chunk chunk : chunks) {
				if (!((WorldServer)world.internal).getPlayerChunkMap().contains(chunk.x, chunk.z)) {
					((WorldServer) world.internal).getChunkProvider().queueUnload(chunk);
					evicted++;
				}
			}
		}
		long totalTeCount = 0;
		long totalUmcCount = 0;
		long totalEntityCount = 0;

		boolean hasChunkLocation = cx != null && cz != null;

		for (Chunk chunk : chunks) {
			int teCount = chunk.getTileEntityMap().size();
			long umcCount = chunk.getTileEntityMap().values().stream().filter(x -> x instanceof cam72cam.mod.block.tile.TileEntity).count();
			int entityCount = Arrays.stream(chunk.getEntityLists()).mapToInt(ClassInheritanceMultiMap::size).sum();

			boolean isChunkLocation = hasChunkLocation && chunk.x == cx && chunk.z == cz;
			if (all || isChunkLocation || !hasChunkLocation && (teCount > 0 || entityCount > 0)) {
				if (list || debug) {
					sender.accept(PlayerMessage.direct(String.format(
							"x=%s, z=%s: %s tiles (%s UMC), %s entities",
							chunk.x, chunk.z, teCount, umcCount, entityCount
					)));
				}
				if (debug) {
					Map<String, Integer> counts = new HashMap<>();
					for (TileEntity tile : chunk.getTileEntityMap().values()) {
						String key = tile.getClass().toString();
						if (tile instanceof cam72cam.mod.block.tile.TileEntity) {
							BlockEntity instance = ((cam72cam.mod.block.tile.TileEntity) tile).instance();
							key = instance != null ? instance.getClass().toString() : "UMC Pending";
						}
						counts.put(key, counts.getOrDefault(key, 0) + 1);
					}
					sender.accept(PlayerMessage.direct(" tiles: "));
					counts.entrySet().stream().sorted(Map.Entry.comparingByValue()).forEach((entry) -> sender.accept(PlayerMessage.direct(String.format(
							"  * %s x %s", entry.getValue(), entry.getKey()
					))));

					counts.clear();
					for (ClassInheritanceMultiMap<net.minecraft.entity.Entity> entityList : chunk.getEntityLists()) {
						for (net.minecraft.entity.Entity entity : entityList) {
							String key = entity.getClass().toString();
							if (entity instanceof ModdedEntity) {
								key = ((ModdedEntity)entity).getSelf().getClass().toString();
							}
							counts.put(key, counts.getOrDefault(key, 0) + 1);
						}
					}
					sender.accept(PlayerMessage.direct(" entities: "));
					counts.entrySet().stream().sorted(Map.Entry.comparingByValue()).forEach((entry) -> sender.accept(PlayerMessage.direct(String.format(
							"  * %s x %s", entry.getValue(), entry.getKey()
					))));
				}
			}
			totalTeCount += teCount;
			totalUmcCount += umcCount;
			totalEntityCount += entityCount;
		}
		if (!hasChunkLocation) {
			sender.accept(PlayerMessage.direct(String.format(
					"%s loaded chunks in %s: %s tiles (%s UMC), %s entities",
					chunks.size(), world.getId(),
					totalTeCount, totalUmcCount, totalEntityCount)
			));
			if (evict) {
				sender.accept(PlayerMessage.direct(String.format("Evicted %s chunks", evicted)));
			}
		}
		return true;
	}

	private void sendWorldEntities(World world, Consumer<PlayerMessage> sender) {
		Map<String, Integer> counts = new HashMap<>();
		for (Entity entity : world.getEntities(Entity.class)) {
			String id = entity.internal.getName();
			if (!counts.containsKey(id)) {
				counts.put(id, 0);
			}
			counts.put(id, counts.get(id) + 1);
		}

		counts.entrySet().stream().sorted(Map.Entry.comparingByValue())
				.forEach(entry -> sender.accept(PlayerMessage.direct(entry.getValue() + " x " + entry.getKey())));
	}
}
