package cam72cam.mod.util;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import cam72cam.mod.ModCore;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.text.Command;
import cam72cam.mod.text.PlayerMessage;
import cam72cam.mod.world.World;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;

public class ModCoreCommand extends Command {
    @Override
    public String getPrefix() {
        return ModCore.MODID;
    }

    @Override
    public String getUsage() {
        return "Usage: " + ModCore.MODID + " entity list";
    }


    @Override
	public int getRequiredPermissionLevel() {
		return PermissionLevel.LEVEL4;
	}

	@Override
	public boolean execute(Consumer<PlayerMessage> sender, Optional<Player> player, String[] args) {

		if (player.isPresent()) {
			// Executed by player

			if (args.length == 2 && "entity".equals(args[0]) && "list".equals(args[1])) {

				World world = player.get().getWorld();

				sendWorldEntities(world, sender);

				return true;
			}

			if (args.length >= 1 && args[0].equals("chunks")) {
				World world = player.get().getWorld();
				sendChunkInfo(world, sender, args.length == 2 && args[1].equals("detailed"));
			}

		} else {
			// Executed by console

			if (args.length == 3 && "entity".equals(args[0]) && "list".equals(args[1])) {

				Optional<Integer> dimId = parseInteger(args[2]);
				if (dimId.isPresent()) {
					World world = World.get(dimId.get(), false);
					if (world == null) {
						sender.accept(PlayerMessage.direct("Dimension '" + dimId.get() + "' is not loaded or does not exist."));
					} else {
						sendWorldEntities(world, sender);
					}
				} else {
					sender.accept(PlayerMessage.direct("Dimension must be a number!"));
				}


			} else if (args.length >= 2 && args[0].equals("chunks")) {
				Optional<Integer> dimId = parseInteger(args[args.length-1]);
				if (dimId.isPresent()) {
					World world = World.get(dimId.get(), false);
					if (world == null) {
						sender.accept(PlayerMessage.direct("Dimension '" + dimId.get() + "' is not loaded or does not exist."));
					} else {
						sendChunkInfo(world, sender, args.length == 3 && args[1].equals("detailed"));
					}
				} else {
					sender.accept(PlayerMessage.direct("Dimension must be a number!"));
				}
			}

			else {

				sender.accept(PlayerMessage.direct(getUsage() + " [dim]"));

			}

			return true;


		}

        return false;
    }

	private void sendChunkInfo(World world, Consumer<PlayerMessage> sender, boolean detailed) {
		ChunkProviderServer provider = (ChunkProviderServer) world.internal.getChunkProvider();
		List<Chunk> chunks = provider.getLoadedChunks().stream().filter(Chunk::isLoaded).collect(Collectors.toList());
		sender.accept(PlayerMessage.direct(String.format("%s loaded chunks in %s:", chunks.size(), world.getId())));
		if (detailed) {
			chunks.sort(Comparator.comparingInt(a -> a.x * 1000000 + a.z));
			for (Chunk chunk : chunks) {
				sender.accept(PlayerMessage.direct(String.format("x=%s, z=%s: %s tiles, %s entities", chunk.x, chunk.z, chunk.getTileEntityMap().size(), Arrays.stream(chunk.getEntityLists()).mapToInt(ClassInheritanceMultiMap::size).sum())));
			}
		}
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

	public Optional<Integer> parseInteger(String text) {

		try {
			return Optional.of(Integer.parseInt(text));
		} catch (NumberFormatException e) {
			return Optional.empty();
		}

	}

}
