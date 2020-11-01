package cam72cam.mod.text;

import cam72cam.mod.world.World;
import net.minecraft.command.*;
import cpw.mods.fml.common.FMLCommonHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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
                return opRequired() ? 2 : 4;
            }

            @Override
            public void processCommand(ICommandSender sender, String[] args) throws CommandException {
                if (!Command.this.execute(World.get(sender.getEntityWorld()), m -> sender.addChatMessage(m.internal), args)) {
                    throw new CommandException(getCommandUsage(sender));
                }
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

    public abstract boolean opRequired();

    public abstract boolean execute(World world, Consumer<PlayerMessage> sender, String[] args);
}
