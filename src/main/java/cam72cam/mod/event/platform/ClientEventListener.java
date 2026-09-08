package cam72cam.mod.event.platform;

import cam72cam.mod.ModCore;
import cam72cam.mod.entity.Player;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.event.Event;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.input.Mouse;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.GlobalRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.event.sound.SoundLoadEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.relauncher.Side;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Consumer;
import java.util.function.Function;

import static cam72cam.mod.event.ClientEvents.*;

/**
 * Forge event subscriber
 */
@ApiStatus.Internal
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = ModCore.MODID)
public class ClientEventListener {
    public static final Event<Runnable> MODEL_CREATE = new Event<>();
    public static final Event<Consumer<ModelBakeEvent>> MODEL_BAKE = new Event<>();
    public static final Event<Runnable> TEXTURE_STITCH = new Event<>();
    public static final Event<Runnable> REGISTER_ENTITY = new Event<>();
    public static final Event<Consumer<RenderGameOverlayEvent.Text>> RENDER_DEBUG = new Event<>();
    public static final Event<Consumer<SoundLoadEvent>> SOUND_LOAD = new Event<>();
    /** Use {@link GlobalRender#registerItemMouseover} */
    public static final Event<Consumer<Float>> RENDER_MOUSEOVER = new Event<>();
    /** Use {@link GlobalRender#registerOverlay} */
    public static final Event<Consumer<RenderGameOverlayEvent.Pre>> RENDER_OVERLAY = new Event<>();
    /** Use {@link Mouse#registerDragHandler} */
    public static final Event<Function<Player.Hand, Boolean>> DRAG = new Event<>();

    public static net.minecraft.world.World clientLast = null;
    public static Vec3d dragPos = null;

    static {
        ClientEvents.registerClientEvents();

        // Forge does not fire world unloaded client side
        ClientEvents.TICK.subscribe(() -> {
            WorldClient currentWorld = Minecraft.getMinecraft().world;
            if (clientLast != currentWorld && clientLast != null) {
                CommonEvents.World.UNLOAD.execute(worldConsumer -> worldConsumer.accept(clientLast));
            }
            clientLast = currentWorld;
        });
    }

    public static Vec3d getDragPos() {
        return dragPos;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            TICK.execute(Runnable::run);
        }
    }

    @SubscribeEvent
    public static void onGuiClick(GuiScreenEvent.MouseInputEvent.Pre event) {
        int x = org.lwjgl.input.Mouse.getEventX() * GUIHelpers.getScreenWidth() / Minecraft.getMinecraft().displayWidth;
        int y = org.lwjgl.input.Mouse.getEventY() * GUIHelpers.getScreenHeight() / Minecraft.getMinecraft().displayHeight;
        int btn = org.lwjgl.input.Mouse.getEventButton();
        ClientEvents.MouseAction action;
        if (org.lwjgl.input.Mouse.getEventButtonState()) {
            // click
            action = ClientEvents.MouseAction.CLICK;
        } else if (btn != -1) {
            // release
            action = ClientEvents.MouseAction.RELEASE;
        } else {
            // move
            action = ClientEvents.MouseAction.MOVE;
        }

        double scroll = Math.signum(org.lwjgl.input.Mouse.getEventDWheel());
        if (scroll != 0) {
            action = ClientEvents.MouseAction.SCROLL;
        }

        ClientEvents.MouseGuiEvent mevt = new ClientEvents.MouseGuiEvent(action, x, GUIHelpers.getScreenHeight() - y, btn, scroll);
        if (!MOUSE_GUI.executeCancellable(h -> h.apply(mevt))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onClick(MouseEvent event) {
        int attackID = Minecraft.getMinecraft().gameSettings.keyBindAttack.getKeyCode() + 100;
        int useID = Minecraft.getMinecraft().gameSettings.keyBindUseItem.getKeyCode() + 100;

        if (event.getButton() == -1 && event.getDwheel() != 0) {
            if (!SCROLL.executeCancellable(x -> x.apply(Math.signum((double)event.getDwheel())))) {
                event.setCanceled(true);
                return;
            }
            return;
        }

        if ((event.getButton() == attackID || event.getButton() == useID)) {
            if (event.isButtonstate()) {
                Player.Hand button = attackID == event.getButton() ? Player.Hand.SECONDARY : Player.Hand.PRIMARY;
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
    public static void onFrame(TickEvent.RenderTickEvent event) {
        if (dragPos != null) {
            //Minecraft.getMinecraft().mouseHelper.mouseXYChange();
            dragPos = dragPos.add(Minecraft.getMinecraft().mouseHelper.deltaX, Minecraft.getMinecraft().mouseHelper.deltaY, 0);
        }
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        MODEL_CREATE.execute(Runnable::run);
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
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        REGISTER_ENTITY.execute(Runnable::run);
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
