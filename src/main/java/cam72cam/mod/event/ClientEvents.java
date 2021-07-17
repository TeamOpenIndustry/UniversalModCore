package cam72cam.mod.event;

import cam72cam.mod.entity.EntityRegistry;
import cam72cam.mod.render.EntityRenderer;
import cam72cam.mod.render.GlobalRender;
import cam72cam.mod.sound.Audio;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import cam72cam.mod.world.World;

import java.util.List;
import java.util.function.Consumer;

/** Registry of events that fire off client side only.  Do not use directly! */
public class ClientEvents {
    public static void registerClientEvents() {
        ClientTickEvents.END_CLIENT_TICK.register(t -> TICK.execute(Runnable::run));

        EntityRegistry.registerClientEvents();
        EntityRenderer.registerClientEvents();
        GlobalRender.registerClientEvents();
        Audio.registerClientCallbacks();
        World.registerClientEvnets();
    }

    /** Fires off a client resource reload event (UMC only).  Do not use directly */
    public static void fireReload() {
        RELOAD.execute(Runnable::run);
    }

    public static final Event<Runnable> TICK = new Event<>();
    public static final Event<Runnable> MODEL_BAKE = new Event<>();
    public static final Event<Runnable> TEXTURE_STITCH = new Event<>();
    public static final Event<Runnable> REGISTER_ENTITY = new Event<>();
    public static final Event<Consumer<List<String>>> RENDER_DEBUG = new Event<>();
    public static final Event<Consumer<Float>> RENDER_OVERLAY = new Event<>();
    public static final Event<Consumer<Float>> RENDER_MOUSEOVER = new Event<>();
    public static final Event<Runnable> SOUND_LOAD = new Event<>();
    public static final Event<Runnable> RELOAD = new Event<>();

}
