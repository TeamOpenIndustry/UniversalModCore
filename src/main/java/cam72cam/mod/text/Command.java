package cam72cam.mod.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import cam72cam.mod.entity.Player;
import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandHandler;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;

/** API not finalized use at your own risk */
public abstract class Command {
    private static final List<Command> commands = new ArrayList<>();

    private final ICommand internal;

    protected Command() {
        this.internal = new CommandBase() {
            @Override
            public String getCommandName() {
                return Command.this.getPrefix();
            }

            @Override
            public String getCommandUsage(ICommandSender sender) {
                return Command.this.getUsage();
            }

            @Override
            public int getRequiredPermissionLevel() {
				return Command.this.getRequiredPermissionLevel();
            }

            @Override
            public void processCommand(ICommandSender sender, String[] args) throws CommandException {
				Optional<Player> player = Optional.empty();
				if (sender instanceof EntityPlayer) {
					player = Optional.of(new Player((EntityPlayer) sender));
				}
				if (!Command.this.execute(m -> sender.addChatMessage(m.internal), player, args)) {
					throw new CommandException(getUsage());
                }
            }

			@Override
			public boolean canCommandSenderUseCommand(ICommandSender sender) {
				if (getRequiredPermissionLevel() == PermissionLevel.NONE)
					return true;
				return super.canCommandSenderUseCommand(sender);
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
	 * Returns <code>PermissionLevel.LEVEL4</code> by default.
	 * opRequired() returned 4 when true 2 when false.
	 * See {@link cam72cam.mod.text.Command.PermissionLevel PermissionLevel} for possible return values.
	 * </pre>
	 */
	public int getRequiredPermissionLevel() {
		return PermissionLevel.LEVEL4;
	}

	/** Executed only on server-side */
	public abstract boolean execute(Consumer<PlayerMessage> sender, Optional<Player> player, String[] args);

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
