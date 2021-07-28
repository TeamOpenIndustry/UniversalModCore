package cam72cam.mod.event;

import cam72cam.mod.entity.EntityRegistry;
import cam72cam.mod.entity.Player;
import cam72cam.mod.input.Mouse;
import cam72cam.mod.render.EntityRenderer;
import cam72cam.mod.render.GlobalRender;
import cam72cam.mod.sound.Audio;
import cam72cam.mod.world.World;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.event.sound.SoundLoadEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.function.Consumer;
import java.util.function.Function;

/** Registry of events that fire off client side only.  Do not use directly! */
public class ClientEvents {

    private static void registerClientEvents() {
        //EntityRegistry.registerClientEvents();
        EntityRenderer.registerClientEvents();
        Mouse.registerClientEvents();
        GlobalRender.registerClientEvents();
        Audio.registerClientCallbacks();
        World.registerClientEvnets();
    }

    /** Fires off a client resource reload event (UMC only).  Do not use directly */
    public static void fireReload() {
        RELOAD.execute(Runnable::run);
    }

    public static final Event<Runnable> TICK = new Event<>();
    public static final Event<Function<Player.Hand, Boolean>> CLICK = new Event<>();
    public static final Event<Runnable> MODEL_CREATE = new Event<>();
    public static final Event<Consumer<ModelBakeEvent>> MODEL_BAKE = new Event<>();
    public static final Event<Runnable> TEXTURE_STITCH = new Event<>();
    public static final Event<Runnable> REGISTER_ENTITY = new Event<>();
    public static final Event<Consumer<RenderGameOverlayEvent.Text>> RENDER_DEBUG = new Event<>();
    public static final Event<Consumer<RenderGameOverlayEvent.Pre>> RENDER_OVERLAY = new Event<>();
    public static final Event<Consumer<Float>> RENDER_MOUSEOVER = new Event<>();
    public static final Event<Consumer<SoundLoadEvent>> SOUND_LOAD = new Event<>();
    public static final Event<Runnable> RELOAD = new Event<>();

    @Mod.EventBusSubscriber(value = Side.CLIENT)
    public static class ClientEventBus {
        static {
            registerClientEvents();
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            TICK.execute(Runnable::run);
        }

        @SubscribeEvent
        public static void onClick(MouseEvent event) {
            int attackID = Minecraft.getMinecraft().gameSettings.keyBindAttack.getKeyCode() + 100;
            int useID = Minecraft.getMinecraft().gameSettings.keyBindUseItem.getKeyCode() + 100;

            if ((event.getButton() == attackID || event.getButton() == useID) && event.isButtonstate()) {
                Player.Hand button = attackID == event.getButton() ? Player.Hand.SECONDARY : Player.Hand.PRIMARY;
                if (!CLICK.executeCancellable(x -> x.apply(button))) {
                    event.setCanceled(true);
                }
            }
        }

        @SubscribeEvent
        public static void registerModels(ModelRegistryEvent event) {
            MODEL_CREATE.execute(Runnable::run);
            REGISTER_ENTITY.execute(Runnable::run);
        }

        @SubscribeEvent
        public static void onModelBakeEvent(ModelBakeEvent event) {
            MODEL_BAKE.execute(x -> x.accept(event));
        }

        @SubscribeEvent
        public static void onTextureStitchEvent(TextureStitchEvent.Pre event) {
            TEXTURE_STITCH.execute(Runnable::run);
        }

        @SubscribeEvent
        public static void onDebugRender(RenderGameOverlayEvent.Text event) {
            RENDER_DEBUG.execute(x -> x.accept(event));
        }

        @SubscribeEvent
        public static void onOverlayEvent(RenderGameOverlayEvent.Pre event) {
            RENDER_OVERLAY.execute(x -> x.accept(event));
        }

        @SubscribeEvent
        public static void onRenderMouseover(DrawBlockHighlightEvent event) {
            RENDER_MOUSEOVER.execute(x -> x.accept(event.getPartialTicks()));
        }

        @SubscribeEvent
        public static void onSoundLoad(SoundLoadEvent event) {
            SOUND_LOAD.execute(x -> x.accept(event));
        }
    }
}
