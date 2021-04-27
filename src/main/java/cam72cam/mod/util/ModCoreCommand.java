package cam72cam.mod.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import cam72cam.mod.ModCore;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.ModdedEntity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.text.Command;
import cam72cam.mod.text.PlayerMessage;
import cam72cam.mod.world.World;
import net.minecraft.entity.EntityList;

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

		} else {
			// Executed by console

			if (args.length == 3 && "entity".equals(args[0]) && "list".equals(args[1])) {

				if (StringUtils.isNumeric(args[2])) {
					Integer dimId = Integer.parseInt(args[2]);
					World world = World.get(dimId, false);
					if (world == null) {
						sender.accept(PlayerMessage.direct("Dimension '" + dimId + "' is not loaded or does not exist."));
					} else {
						sendWorldEntities(world, sender);
					}
				} else {
					sender.accept(PlayerMessage.direct("Dimension must be a number!"));
				}


			} else {

				sender.accept(PlayerMessage.direct(getUsage() + " [dim]"));

			}

			return true;


		}
        return false;
    }

	private void sendWorldEntities(World world, Consumer<PlayerMessage> sender) {
        Map<String, Integer> counts = new HashMap<>();
        for (Entity entity : world.getEntities(Entity.class)) {
            String id = EntityList.getEntityString(entity.internal);
            if (entity.internal instanceof ModdedEntity) {
                id = ((ModdedEntity) entity.internal).getName();
            }
            if (!counts.containsKey(id)) {
                counts.put(id, 0);
            }
            counts.put(id, counts.get(id) + 1);
        }

        counts.entrySet().stream().sorted(Map.Entry.comparingByValue()).forEach(entry -> sender.accept(PlayerMessage.direct(entry.getValue() + " x " + entry.getKey())));
	}

}
