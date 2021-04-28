package cam72cam.mod.text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import cam72cam.mod.ModCore;
import cam72cam.mod.entity.Player;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.MessageArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;


/** API not finalized use at your own risk */
public abstract class Command implements com.mojang.brigadier.Command<CommandSource> {
    private static final List<Command> commands = new ArrayList<>();

//    private final ICommand internal;
//
//    protected Command() {
//        this.internal = new CommandBase() {
//            @Override
//            public String getName() {
//                return Command.this.getPrefix();
//            }
//
//            @Override
//            public String getUsage(ICommandSender sender) {
//                return Command.this.getUsage();
//            }
//
//            @Override
//            public int getRequiredPermissionLevel() {
//				return Command.this.getRequiredPermissionLevel();
//            }
//
//            @Override
//            public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
//				if (getRequiredPermissionLevel() == PermissionLevel.NONE)
//					return true;
//				return sender.canUseCommand(getRequiredPermissionLevel(), getName());
//            }
//            
//            @Override
//			public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
//				Optional<Player> player = Optional.empty();
//				if (sender instanceof EntityPlayer) {
//					player = Optional.of(new Player((EntityPlayer) sender));
//				}
//				if (!Command.this.execute(m -> sender.sendMessage(m.internal), player, args)) {
//                    throw new CommandException(getUsage(sender));
//                }
//            }
//        };
//    }

    public static void register(Command cmd) {
        commands.add(cmd);
    }

    public static void registration(CommandDispatcher<CommandSource> ch) {
		ModCore.debug("Registration of commands started.. (Count: %d)", commands.size());

		for (Command command : commands) {
			// @formatter:off
			
			ch.register(
				Commands.literal(command.getPrefix())
				// Check if player has permission 
				.requires(source -> source.hasPermissionLevel(command.getRequiredPermissionLevel()))
				.then(
						// Execute command with arguments
						Commands.argument("arguments", MessageArgument.message()).executes(command)
				)
				// Execute command without arguments
				.executes(command)
			);
			
			// @formatter:on

		}
	}

	@Override
	public int run(CommandContext<CommandSource> context) throws CommandSyntaxException {
		Optional<Player> player = Optional.empty();
		try {
			ServerPlayerEntity serverPlayer = context.getSource().asPlayer();
			player = Optional.of(new Player(serverPlayer));
		} catch (CommandSyntaxException e) {
			// Is the console ?
		}

		Consumer<PlayerMessage> pm = m -> context.getSource().sendFeedback(m.internal, false);
		String[] args = context.getInput().split(" ");
		// Remove command from arguments
		args = Arrays.copyOfRange(args, 1, args.length);

		boolean ok = this.execute(pm, player, args);


		if (!ok) {
			context.getSource().sendErrorMessage(new StringTextComponent(getUsage()));
			return -1;
		}

		return 1;
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
