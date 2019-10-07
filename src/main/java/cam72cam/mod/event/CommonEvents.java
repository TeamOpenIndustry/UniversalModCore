package cam72cam.mod.event;

import net.fabricmc.fabric.api.event.registry.BlockConstructedCallback;
import net.fabricmc.fabric.api.event.world.WorldTickCallback;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.network.packet.BlockBreakingProgressS2CPacket;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;

import java.util.function.Consumer;

public class CommonEvents {
    private static void registerEvents() {
        cam72cam.mod.world.World.registerEvents();
        cam72cam.mod.entity.EntityRegistry.registerEvents();
        //cam72cam.mod.world.ChunkManager.registerEvents();
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
    }

    public static final class EventBus {
        static {
            registerEvents();
        }

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
