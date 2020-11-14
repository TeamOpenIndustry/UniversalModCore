package cam72cam.mod.text;

import cam72cam.mod.world.World;
import net.fabricmc.fabric.api.registry.CommandRegistry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.LiteralText;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** API not finalized use at your own risk */
public abstract class Command {
    private static final List<Command> commands = new ArrayList<>();

    public static void register(Command cmd) {
        commands.add(cmd);
    }

    public static void registration() {
        for (Command command : commands) {
            CommandRegistry.INSTANCE.register(false, dispatcher -> {
                dispatcher.register(CommandManager
                        .literal(command.getPrefix())
                        .executes(ctx -> {
                    boolean ok = command.execute(World.get(ctx.getSource().getWorld()), msg -> ctx.getSource().getEntity().sendMessage(msg.internal), ctx.getInput().split(" "));
                    if (!ok) {
                        ctx.getSource().getEntity().sendMessage(new LiteralText(command.getUsage()));
                    }
                    return ok ? 1 : -1;
                }).requires(source -> source.hasPermissionLevel(command.opRequired() ? 4 : 0))
                );
            });
        }
    }

    public abstract String getPrefix();

    public abstract String getUsage();

    public abstract boolean opRequired();

    public abstract boolean execute(World world, Consumer<PlayerMessage> sender, String[] args);
}
