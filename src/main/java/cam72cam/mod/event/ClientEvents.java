package cam72cam.mod.event;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.EntityRegistry;
import cam72cam.mod.entity.Player;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.input.Mouse;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.EntityRenderer;
import cam72cam.mod.render.GlobalRender;
import cam72cam.mod.render.opengl.CustomTexture;
import cam72cam.mod.render.opengl.VBO;
import cam72cam.mod.sound.Audio;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cam72cam.mod.world.World;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.event.sound.SoundLoadEvent;

import java.util.function.Consumer;
import java.util.function.Function;

/** Registry of events that fire off client side only.  Do not use directly! */
public class ClientEvents {

    private static net.minecraft.world.World clientLast = null;
    private static void registerClientEvents() {
        EntityRegistry.registerClientEvents();
        EntityRenderer.registerClientEvents();
        Mouse.registerClientEvents();
        Player.registerClientEvents();
        GlobalRender.registerClientEvents();
        World.registerClientEvnets();

        MODEL_CREATE.execute(Runnable::run);
        REGISTER_ENTITY.execute(Runnable::run);
        VBO.registerClientEvents();
        CustomTexture.registerClientEvents();

        // Forge does not fire world unloaded client side
        TICK.subscribe(() -> {
            WorldClient mcw = Minecraft.getMinecraft().theWorld;
            if (clientLast != mcw && clientLast != null) {
                CommonEvents.World.UNLOAD.execute(worldConsumer -> worldConsumer.accept(clientLast));
            }
            clientLast = mcw;
        });
    }

    /** Fires off a client resource reload event (UMC only).  Do not use directly */
    public static void fireReload() {
        RELOAD.execute(Runnable::run);
    }

    public enum MouseAction {
        CLICK,
        RELEASE,
        MOVE,
    }

    public static class MouseGuiEvent {
        public final MouseAction action;
        public final int x;
        public final int y;
        public final int button;

        public MouseGuiEvent(MouseAction action, int x, int y, int button) {
            this.action = action;
            this.x = x;
            this.y = y;
            this.button = button;
        }
    }

    public static final Event<Runnable> TICK = new Event<>();
    public static final Event<Function<Player.Hand, Boolean>> DRAG = new Event<>();
    public static final Event<Function<Player.Hand, Boolean>> CLICK = new Event<>();
    public static final Event<Function<MouseGuiEvent, Boolean>> MOUSE_GUI = new Event<>();
    public static final Event<Runnable> MODEL_CREATE = new Event<>();
    //public static final Event<Consumer<ModelBakeEvent>> MODEL_BAKE = new Event<>();
    public static final Event<Runnable> TEXTURE_STITCH = new Event<>();
    public static final Event<Runnable> REGISTER_ENTITY = new Event<>();
    public static final Event<Consumer<RenderGameOverlayEvent.Text>> RENDER_DEBUG = new Event<>();
    public static final Event<Consumer<RenderGameOverlayEvent.Pre>> RENDER_OVERLAY = new Event<>();
    public static final Event<Consumer<Float>> RENDER_MOUSEOVER = new Event<>();
    public static final Event<Consumer<SoundLoadEvent>> SOUND_LOAD = new Event<>();
    public static final Event<Runnable> RELOAD = new Event<>();

    public static class ClientEventBus {
        private static Vec3d dragPos = null;
        public ClientEventBus() {
            registerClientEvents();
        }

        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent event) {
            if (MinecraftClient.isReady()) {
                TICK.execute(Runnable::run);
            }
        }

        @SubscribeEvent
        public void onGuiClick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.START) {
                return;
            }
            int x = org.lwjgl.input.Mouse.getEventX() * GUIHelpers.getScreenWidth() / Minecraft.getMinecraft().displayWidth;
            int y = org.lwjgl.input.Mouse.getEventY() * GUIHelpers.getScreenHeight() / Minecraft.getMinecraft().displayHeight;
            int btn = org.lwjgl.input.Mouse.getEventButton();
            MouseAction action;
            if (org.lwjgl.input.Mouse.getEventButtonState()) {
                // click
                action = MouseAction.CLICK;
            } else if (btn != -1) {
                // release
                action = MouseAction.RELEASE;
            } else {
                // move
                action = MouseAction.MOVE;
            }
            MouseGuiEvent mevt = new MouseGuiEvent(action, x, GUIHelpers.getScreenHeight() - y, btn);
            if (!MOUSE_GUI.executeCancellable(h -> h.apply(mevt))) {
                //event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public void onClick(MouseEvent event) {
            int attackID = Minecraft.getMinecraft().gameSettings.keyBindAttack.getKeyCode() + 100;
            int useID = Minecraft.getMinecraft().gameSettings.keyBindUseItem.getKeyCode() + 100;

            if ((event.button == attackID || event.button == useID)) {
                if (event.buttonstate) {
                    Player.Hand button = attackID == event.button ? Player.Hand.SECONDARY : Player.Hand.PRIMARY;
                    if (!DRAG.executeCancellable(x -> x.apply(button))) {
                        event.setCanceled(true);
                        dragPos = new Vec3d(0, 0, 0);
                        return;
                    }
                    if (!CLICK.executeCancellable(x -> x.apply(button))) {
                        event.setCanceled(true);
                    }
                } else {
                    dragPos = null;
                }
            }
        }

        @SubscribeEvent
        public void onFrame(TickEvent.RenderTickEvent event) {
            if (dragPos != null) {
                //Minecraft.getMinecraft().mouseHelper.mouseXYChange();
                dragPos = dragPos.add(Minecraft.getMinecraft().mouseHelper.deltaX, Minecraft.getMinecraft().mouseHelper.deltaY, 0);
            }
        }

        public static Vec3d getDragPos() {
            return dragPos;
        }

        @SubscribeEvent
        public void onTextureStitchEvent(TextureStitchEvent.Pre event) {
            TEXTURE_STITCH.execute(Runnable::run);
        }


        @SubscribeEvent
        public void onDebugRender(RenderGameOverlayEvent.Text event) {
            RENDER_DEBUG.execute(x -> x.accept(event));
        }

        @SubscribeEvent
        public void onOverlayEvent(RenderGameOverlayEvent.Pre event) {
            RENDER_OVERLAY.execute(x -> x.accept(event));
        }

        @SubscribeEvent
        public void onRenderMouseover(DrawBlockHighlightEvent event) {
            RENDER_MOUSEOVER.execute(x -> x.accept(event.partialTicks));
        }

        @SubscribeEvent
        public void onSoundLoad(SoundLoadEvent event) {
            SOUND_LOAD.execute(x -> x.accept(event));
        }
    }
}
