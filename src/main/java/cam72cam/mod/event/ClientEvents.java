package cam72cam.mod.event;

import cam72cam.mod.ModCore;
import cam72cam.mod.entity.EntityRegistry;
import cam72cam.mod.gui.GuiRegistry;
import cam72cam.mod.entity.Player;
import cam72cam.mod.input.Mouse;
import cam72cam.mod.render.BlockRender;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.EntityRenderer;
import cam72cam.mod.render.GlobalRender;
import cam72cam.mod.render.opengl.CustomTexture;
import cam72cam.mod.render.opengl.VBO;
import cam72cam.mod.world.World;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.event.sound.SoundLoadEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.function.Consumer;
import java.util.function.Function;

/** Registry of events that fire off client side only.  Do not use directly! */
public class ClientEvents {

    private static net.minecraft.world.World clientLast = null;
    private static void registerClientEvents() {
        EntityRegistry.registerClientEvents();
        EntityRenderer.registerClientEvents();
        Mouse.registerClientEvents();
        GlobalRender.registerClientEvents();
        GuiRegistry.registerClientEvents();
        World.registerClientEvnets();

        VBO.registerClientEvents();
        CustomTexture.registerClientEvents();

        // Forge does not fire world unloaded client side
        TICK.subscribe(() -> {
            ClientWorld mcw = Minecraft.getInstance().world;
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
    public static final Event<Consumer<ModelBakeEvent>> MODEL_BAKE = new Event<>();
    public static final Event<Consumer<TextureStitchEvent.Pre>> TEXTURE_STITCH = new Event<>();
    public static final Event<Runnable> REGISTER_ENTITY = new Event<>();
    public static final Event<Consumer<RenderGameOverlayEvent.Text>> RENDER_DEBUG = new Event<>();
    public static final Event<Consumer<RenderGameOverlayEvent.Pre>> RENDER_OVERLAY = new Event<>();
    public static final Event<Consumer<DrawHighlightEvent.HighlightBlock>> RENDER_MOUSEOVER = new Event<>();
    public static final Event<Consumer<SoundLoadEvent>> SOUND_LOAD = new Event<>();
    public static final Event<Runnable> RELOAD = new Event<>();
    public static final Event<Consumer<RenderWorldLastEvent>> OPTIFINE_SUCKS = new Event<>();

    @Mod.EventBusSubscriber(modid = ModCore.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientEventBusForge {
        private static Vec3d dragPos = null;
        private static boolean skipNextMouseInputEvent = false;

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            TICK.execute(Runnable::run);
        }

        private static void onGuiMouse(GuiScreenEvent.MouseInputEvent event, int btn, MouseAction action) {
            MouseGuiEvent mevt = new MouseGuiEvent(action, (int) event.getMouseX(), (int) event.getMouseY(), btn);
            if (!MOUSE_GUI.executeCancellable(h -> h.apply(mevt))) {
                event.setCanceled(true);
                // Apparently cancelling this input event only cancels it for the *GUI* handlers, not all input handlers
                // Therefore we need to track that ourselves.  Thanks for changing that from 1.12.2-forge
                skipNextMouseInputEvent = true;
            }
        }

        @SubscribeEvent
        public static void onGuiClick(GuiScreenEvent.MouseClickedEvent.Pre event) {
            onGuiMouse(event, event.getButton(), MouseAction.CLICK);
        }
        @SubscribeEvent
        public static void onGuiDrag(GuiScreenEvent.MouseDragEvent.Pre event) {
            onGuiMouse(event, event.getMouseButton(), MouseAction.MOVE);
        }
        @SubscribeEvent
        public static void onGuiRelease(GuiScreenEvent.MouseReleasedEvent.Pre event) {
            onGuiMouse(event, event.getButton(), MouseAction.RELEASE);
        }

        private static void hackInputState(InputEvent.MouseInputEvent event) {
            int attackID = Minecraft.getInstance().gameSettings.keyBindAttack.getKey().getKeyCode();
            int useID = Minecraft.getInstance().gameSettings.keyBindUseItem.getKey().getKeyCode();

            // This prevents the event from firing
            if (event.getButton() == attackID) {
                Minecraft.getInstance().gameSettings.keyBindAttack.isPressed();
            }
            if (event.getButton() == useID) {
                Minecraft.getInstance().gameSettings.keyBindUseItem.isPressed();
            }
        }

        @SubscribeEvent
        public static void onClick(InputEvent.MouseInputEvent event) {
            if (skipNextMouseInputEvent) {
                // This is the path from onGuiMouse
                skipNextMouseInputEvent = false;
                hackInputState(event);
                return;
            }

            int attackID = Minecraft.getInstance().gameSettings.keyBindAttack.getKey().getKeyCode();
            int useID = Minecraft.getInstance().gameSettings.keyBindUseItem.getKey().getKeyCode();

            if (event.getButton() == attackID || event.getButton() == useID) {
                if(event.getAction() == 1) {
                    Player.Hand button = attackID == event.getButton() ? Player.Hand.SECONDARY : Player.Hand.PRIMARY;
                    if (!DRAG.executeCancellable(x -> x.apply(button))) {
                        //event.setCanceled(true);
                        hackInputState(event);
                        dragPos = new Vec3d(0, 0, 0);
                        return;
                    }
                    if (!CLICK.executeCancellable(x -> x.apply(button))) {
                        //event.setCanceled(true);
                        hackInputState(event);
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
                dragPos = dragPos.add(Minecraft.getInstance().mouseHelper.getXVelocity(), Minecraft.getInstance().mouseHelper.getYVelocity(), 0);
            }
        }

        public static Vec3d getDragPos() {
            return dragPos;
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
        public static void onRenderMouseover(DrawHighlightEvent.HighlightBlock event) {
            RenderType.getCutout().setupRenderState();
            // TODO 1.15+ do we need to set lightmap coords here?
            RENDER_MOUSEOVER.execute(x -> x.accept(event));
            RenderType.getCutout().clearRenderState();
        }

        @SubscribeEvent
        public static void onSoundLoad(SoundLoadEvent event) {
            SOUND_LOAD.execute(x -> x.accept(event));
        }

        @SubscribeEvent
        public static void optifineSucksEvent(RenderWorldLastEvent event) {
            OPTIFINE_SUCKS.execute(x -> x.accept(event));
        }
    }

    @Mod.EventBusSubscriber(modid = ModCore.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientEventBusMod {
        static {
            registerClientEvents();
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
        public static void onColorSetup(ColorHandlerEvent.Block event) {
            BlockRender.onPostColorSetup(event.getBlockColors());
        }

        @SubscribeEvent
        public static void onTextureStitchEvent(TextureStitchEvent.Pre event) {
            TEXTURE_STITCH.execute(x -> x.accept(event));
        }

        @SubscribeEvent(priority = EventPriority.LOW)
        public static void registerEntities(RegistryEvent.Register<EntityType<?>> event) {
            REGISTER_ENTITY.execute(Runnable::run);
        }
    }
}
