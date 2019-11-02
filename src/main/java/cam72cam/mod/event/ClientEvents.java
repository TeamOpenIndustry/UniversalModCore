package cam72cam.mod.event;

import cam72cam.mod.entity.EntityRegistry;
import cam72cam.mod.input.Keyboard;
import cam72cam.mod.render.EntityRenderer;
import cam72cam.mod.render.GlobalRender;
import cam72cam.mod.sound.Audio;
import net.fabricmc.fabric.api.event.client.ClientTickCallback;

import java.util.List;
import java.util.function.Consumer;

public class ClientEvents {
    public static void registerClientEvents() {
        ClientTickCallback.EVENT.register(client -> TICK.execute(Runnable::run));
        //ClientSpriteRegistryCallback.event(SpriteAtlasTexture.BLOCK_ATLAS_TEX).register((a,b) -> TEXTURE_STITCH.execute(Runnable::run));

        EntityRegistry.registerClientEvents();
        EntityRenderer.registerClientEvents();
        Keyboard.registerClientEvents();
        GlobalRender.registerClientEvents();
        Audio.registerClientCallbacks();
    }

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
