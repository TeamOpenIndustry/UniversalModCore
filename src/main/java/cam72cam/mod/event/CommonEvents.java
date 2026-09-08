package cam72cam.mod.event;

import org.jetbrains.annotations.ApiStatus;

/** Registry of events that fire both client and server. */
public class CommonEvents {

    public static final class World {
        public static final Event<WorldEvent> LOAD = new Event<>();
        public static final Event<WorldEvent> UNLOAD = new Event<>();
    }

    @FunctionalInterface
    public interface WorldEvent {
        void handle(cam72cam.mod.world.World world);
    }

    @ApiStatus.Internal
    public static void registerEvents() {
        cam72cam.mod.world.World.registerEvents();
        cam72cam.mod.entity.EntityRegistry.registerEvents();
    }
}
