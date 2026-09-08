package cam72cam.mod.event;

import cam72cam.mod.event.platform.CommonEventListener;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Consumer;

/** Registry of events that fire both client and server. */
public class CommonEvents {

    @ApiStatus.Internal
    public static final class World {
        public static final Event<Consumer<net.minecraft.world.World>> LOAD = new Event<>();
        public static final Event<Consumer<net.minecraft.world.World>> UNLOAD = new Event<>();
        public static final Event<Consumer<net.minecraft.world.World>> TICK = new Event<>();
    }

    public static final class Block {
        public static final Event<Runnable> REGISTER = new Event<>();
        @ApiStatus.Internal
        public static final Event<CommonEventListener.BlockBrokenEvent> BROKEN = new Event<>();
    }

    public static final class Item {
        public static final Event<Runnable> REGISTER = new Event<>();
    }

    public static final class Recipe {
        public static final Event<Runnable> REGISTER = new Event<>();
    }

    public static final class Entity {
        public static final Event<Runnable> REGISTER = new Event<>();
        @ApiStatus.Internal
        public static final Event<CommonEventListener.EntityJoinEvent> JOIN = new Event<>();
    }

    @ApiStatus.Internal
    public static void registerEvents() {
        cam72cam.mod.world.World.registerEvents();
        cam72cam.mod.entity.EntityRegistry.registerEvents();
    }
}
