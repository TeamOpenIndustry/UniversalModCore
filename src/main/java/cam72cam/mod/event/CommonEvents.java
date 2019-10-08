package cam72cam.mod.event;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.function.Consumer;

public class CommonEvents {
    public static void registerEvents() {
        cam72cam.mod.world.World.registerEvents();
        cam72cam.mod.entity.EntityRegistry.registerEvents();
        //cam72cam.mod.world.ChunkManager.registerEvents();

        CommonEvents.Block.REGISTER.execute(Runnable::run);
        CommonEvents.Item.REGISTER.execute(Runnable::run);
        CommonEvents.Entity.REGISTER.execute(Runnable::run);
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
        //TODO
        public static final Event<Runnable> REGISTER = new Event<>();
    }

    public static final class Entity {
        public static final Event<Runnable> REGISTER = new Event<>();
        public static final Event<EventBus.EntityJoinEvent> JOIN = new Event<>();
        public static final Event<Consumer<net.minecraft.entity.Entity>> WORLD_JOIN = new Event<>();
        public static final Event<Consumer<net.minecraft.entity.Entity>> WORLD_LEAVE = new Event<>();
    }

    public static final class EventBus {
        @FunctionalInterface
        public interface BlockBrokenEvent {
            boolean onBroken(net.minecraft.world.World world, BlockPos pos, PlayerEntity player);
        }

        @FunctionalInterface
        public interface EntityJoinEvent {
            boolean onJoin(net.minecraft.world.World world, net.minecraft.entity.Entity entity);
        }
    }
}
