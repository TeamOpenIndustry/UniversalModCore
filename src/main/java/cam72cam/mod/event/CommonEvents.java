package cam72cam.mod.event;

import cam72cam.mod.math.Vec3i;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.WorldEvent;

import java.util.function.Consumer;

/** Registry of events that fire off on both client and server.  Do not use directly! */
public class CommonEvents {
    private static void registerEvents() {
        cam72cam.mod.world.World.registerEvents();
        cam72cam.mod.entity.EntityRegistry.registerEvents();

        Block.REGISTER.execute(Runnable::run);
        Item.REGISTER.execute(Runnable::run);
        Entity.REGISTER.execute(Runnable::run);
        Recipe.REGISTER.execute(Runnable::run);
    }

    public static final class World {
        public static final Event<Consumer<net.minecraft.world.World>> LOAD = new Event<>();
        public static final Event<Consumer<net.minecraft.world.World>> UNLOAD = new Event<>();
        public static final Event<Consumer<net.minecraft.world.World>> TICK = new Event<>();
    }

    public static final class Block {
        public static final Event<Runnable> REGISTER = new Event<>();
        public static final Event<EventBus.BlockBrokenEvent> BROKEN = new Event<>();
    }

    public static final class Item {
        public static final Event<Runnable> REGISTER = new Event<>();
    }

    public static final class Recipe {
        public static final Event<Runnable> REGISTER = new Event<>();
    }

    public static final class Entity {
        public static final Event<Runnable> REGISTER = new Event<>();
        public static final Event<EventBus.EntityJoinEvent> JOIN = new Event<>();
    }

    public static final class EventBus {
        public EventBus() {
            registerEvents();
        }

        // World
        @SubscribeEvent
        public void onWorldLoad(WorldEvent.Load event) {
            World.LOAD.execute(x -> x.accept(event.world));
        }

        @SubscribeEvent
        public void onWorldUnload(WorldEvent.Unload event) {
            World.UNLOAD.execute(x -> x.accept(event.world));
        }

        @SubscribeEvent
        public void onWorldTick(TickEvent.WorldTickEvent event) {
            if (event.phase == TickEvent.Phase.START) {
                World.TICK.execute(x -> x.accept(event.world));
            }
        }

        @FunctionalInterface
        public interface BlockBrokenEvent {
            boolean onBroken(net.minecraft.world.World world, Vec3i pos, EntityPlayer player);
        }
        @SubscribeEvent
        public void onBlockBreakEvent(BlockEvent.BreakEvent event) {
            if (!Block.BROKEN.executeCancellable(x -> x.onBroken(event.world, new Vec3i(event.x, event.y, event.z), event.getPlayer()))) {
                event.setCanceled(true);
            }
        }

        @FunctionalInterface
        public interface EntityJoinEvent {
            boolean onJoin(net.minecraft.world.World world, net.minecraft.entity.Entity entity);
        }
        @SubscribeEvent
        public void onEntityJoin(EntityJoinWorldEvent event) {
            if (!Entity.JOIN.executeCancellable(x -> x.onJoin(event.world, event.entity))) {
                event.setCanceled(true);
            }
        }
    }
}
