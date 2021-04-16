package cam72cam.mod.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import cam72cam.mod.entity.Player;
import cam72cam.mod.world.World;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandHandler;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;

/** API not finalized use at your own risk */
public abstract class Command {
    private static final List<Command> commands = new ArrayList<>();

    private final ICommand internal;
	private Player player = null;

    protected Command() {
        this.internal = new CommandBase() {
            @Override
            public String getName() {
                return Command.this.getPrefix();
            }

            @Override
            public String getUsage(ICommandSender sender) {
                return Command.this.getUsage();
            }

            @Override
            public int getRequiredPermissionLevel() {
				return Command.this.getRequiredPermissionLevel();
            }

            @Override
            public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
				if (getRequiredPermissionLevel() == PermissionLevel.NONE)
					return true;
				return sender.canUseCommand(getRequiredPermissionLevel(), getName());
            }
            
            @Override
			public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
				if (sender instanceof EntityPlayer) {
					player = new Player((EntityPlayer) sender);
				}
                if (!Command.this.execute(World.get(sender.getEntityWorld()), m -> sender.sendMessage(m.internal), args)) {
                    throw new CommandException(getUsage(sender));
                }
				player = null;
            }
        };
    }

    public static void register(Command cmd) {
        commands.add(cmd);
    }

    public static void registration() {
        CommandHandler ch = (CommandHandler) FMLCommonHandler.instance().getMinecraftServerInstance().getCommandManager();
        for (Command command : commands) {
            ch.registerCommand(command.internal);
        }
    }

    public abstract String getPrefix();

    public abstract String getUsage();

	/**
	 * <pre>
	 * Returns internally OP-Level 2 (false) and 4 (true)
	 * </pre>
	 * 
	 * @deprecated see getRequiredPermissionLevel()
	 */
	@Deprecated
	public boolean opRequired() {
		return true;
	}

	/**
	 * <pre>
	 * Executes opRequired()
	 * See <b>Command.PermissionLevel</b> for further information.
	 * </pre>
	 */
	public int getRequiredPermissionLevel() {
		return this.opRequired() ? PermissionLevel.LEVEL4 : PermissionLevel.LEVEL2;
	}

	/** Executed only on server-side */
    public abstract boolean execute(World world, Consumer<PlayerMessage> sender, String[] args);

	/**
	 * <pre>
	 * 	Returns the player that executed this command.
	 *  Returns Optional.empty() if command was executed by console.
	 *  Only available on execute();
	 * </pre>
	 */
	public Optional<Player> getPlayer() {
		return Optional.ofNullable(player);
    }

	public static class PermissionLevel {
		/** Everyone has this level */
		public static final int NONE = 0;
		/** OP-Level 1 */
		public static final int LEVEL1 = 1;
		/** OP-Level 2 */
		public static final int LEVEL2 = 2;
		/** OP-Level 3 */
		public static final int LEVEL3 = 3;
		/** OP-Level 4 */
		public static final int LEVEL4 = 4;
	}

}
