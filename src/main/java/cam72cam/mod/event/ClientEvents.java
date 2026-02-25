package cam72cam.mod.event;

import cam72cam.mod.ModCore;
import cam72cam.mod.entity.EntityRegistry;
import cam72cam.mod.event.platform.RegisterTextureSpriteEvent;
import cam72cam.mod.gui.GuiRegistry;
import cam72cam.mod.entity.Player;
import cam72cam.mod.input.Mouse;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.BlockRender;
import cam72cam.mod.render.EntityRenderer;
import cam72cam.mod.render.GlobalRender;
import cam72cam.mod.render.opengl.CustomTexture;
import cam72cam.mod.render.opengl.VBO;
import cam72cam.mod.world.World;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.event.sound.SoundEngineLoadEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

import java.util.function.Consumer;
import java.util.function.Function;

/** Registry of events that fire off client side only.  Do not use directly! */
public class ClientEvents {

    private static ClientLevel clientLast = null;
    private static void registerClientEvents() {
        EntityRegistry.registerClientEvents();
        EntityRenderer.registerClientEvents();
        Mouse.registerClientEvents();
        GlobalRender.registerClientEvents();
        GuiRegistry.registerClientEvents();
        World.registerClientEvnets();
        CommonEvents.Entity.REGISTER.post(() -> REGISTER_ENTITY.execute(Runnable::run));

        VBO.registerClientEvents();
        CustomTexture.registerClientEvents();

        // Forge does not fire world unloaded client side
        TICK.subscribe(() -> {
            ClientLevel mcw = Minecraft.getInstance().level;
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
        SCROLL,
    }

    public static class MouseGuiEvent {
        public final MouseAction action;
        public final int x;
        public final int y;
        public final int button;
        public final double scroll;

        public MouseGuiEvent(MouseAction action, int x, int y, int button, double scroll) {
            this.action = action;
            this.x = x;
            this.y = y;
            this.button = button;
            this.scroll = scroll;
        }
    }

    public static final Event<Runnable> TICK = new Event<>();
    @Deprecated // TODO find a better hack
    public static final Event<Runnable> TICK_POST = new Event<>();
    public static final Event<Function<Player.Hand, Boolean>> DRAG = new Event<>();
    public static final Event<Function<Double, Boolean>> SCROLL = new Event<>();
    public static final Event<Function<Player.Hand, Boolean>> CLICK = new Event<>();
    public static final Event<Function<MouseGuiEvent, Boolean>> MOUSE_GUI = new Event<>();
    public static final Event<Runnable> MODEL_CREATE = new Event<>();
    public static final Event<Consumer<ModelEvent.ModifyBakingResult>> MODEL_BAKE = new Event<>();
    public static final Event<Consumer<RegisterTextureSpriteEvent>> TEXTURE_STITCH = new Event<>();
    public static final Event<Runnable> HACKS = new Event<>();
    public static final Event<Runnable> REGISTER_ENTITY = new Event<>();
    public static final Event<Consumer<RegisterShadersEvent>> REGISTER_SHADER = new Event<>();
    public static final Event<Consumer<CustomizeGuiOverlayEvent.DebugText>> RENDER_DEBUG = new Event<>();
    public static final Event<Consumer<RenderGuiLayerEvent.Pre>> RENDER_OVERLAY = new Event<>();
    public static final Event<Consumer<RenderHighlightEvent.Block>> RENDER_MOUSEOVER = new Event<>();
    public static final Event<Consumer<SoundEngineLoadEvent>> SOUND_LOAD = new Event<>();
    public static final Event<Runnable> RELOAD = new Event<>();
//    public static final Event<Consumer<RenderLevelStageEvent>> OPTIFINE_SUCKS = new Event<>();
    public static final Event<Consumer<RegisterKeyMappingsEvent>> KEY_MAPPING_REGISTER = new Event<>();
    public static final Event<Consumer<RegisterClientExtensionsEvent>> CLIENT_EXTENSIONS_REGISTER = new Event<>();
    public static final Event<Consumer<RegisterMenuScreensEvent>> MENU_SCREENS_REGISTER = new Event<>();

    @EventBusSubscriber(modid = ModCore.MODID, value = Dist.CLIENT)
    public static class ClientEventBus {
        private static Vec3d dragPos = null;
        private static boolean skipNextMouseInputEvent = false;

        static {
            registerClientEvents();
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Pre event) {
            TICK.execute(Runnable::run);
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            TICK_POST.execute(Runnable::run);
        }

        private static void onGuiMouse(ScreenEvent event, int x, int y, int btn, MouseAction action) {
            MouseGuiEvent mevt = new MouseGuiEvent(action, x, y, btn, action == MouseAction.SCROLL ? (int) ((ScreenEvent.MouseScrolled) event).getScrollDeltaY() : 0);

            if (!MOUSE_GUI.executeCancellable(h -> h.apply(mevt))) {
                ((ICancellableEvent)event).setCanceled(true);
                if (action != MouseAction.SCROLL) {
                    // Apparently cancelling this input event only cancels it for the *GUI* handlers, not all input handlers
                    // Therefore we need to track that ourselves.  Thanks for changing that from 1.12.2-forge
                    skipNextMouseInputEvent = true;
                }
            }
        }

        @SubscribeEvent
        public static void onGuiClick(ScreenEvent.MouseButtonPressed.Pre event) {
            onGuiMouse(event, (int) event.getMouseX(), (int) event.getMouseY(), event.getButton(), MouseAction.CLICK);
        }
        @SubscribeEvent
        public static void onGuiDrag(ScreenEvent.MouseDragged.Pre event) {
            onGuiMouse(event, (int) event.getMouseX(), (int) event.getMouseY(), event.getMouseButton(), MouseAction.MOVE);
        }
        @SubscribeEvent
        public static void onGuiRelease(ScreenEvent.MouseButtonReleased.Pre event) {
            onGuiMouse(event, (int) event.getMouseX(), (int) event.getMouseY(), event.getButton(), MouseAction.RELEASE);
        }
        @SubscribeEvent
        public static void onGuiScroll(ScreenEvent.MouseScrolled.Pre event) {
            onGuiMouse(event, (int) event.getMouseX(), (int) event.getMouseY(), -1, MouseAction.SCROLL);
        }

        private static void hackInputState(int event) {
            int attackID = Minecraft.getInstance().options.keyAttack.getKey().getValue();
            int useID = Minecraft.getInstance().options.keyUse.getKey().getValue();

            // This prevents the event from firing
            if (event == attackID) {
                Minecraft.getInstance().options.keyAttack.consumeClick();
            }
            if (event == useID) {
                Minecraft.getInstance().options.keyUse.consumeClick();
            }
        }

        @SubscribeEvent
        public static void onScroll(InputEvent.MouseScrollingEvent event) {
            if (!SCROLL.executeCancellable(x -> x.apply(event.getScrollDeltaY()))) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onClick(InputEvent.MouseButton.Pre event) {
            if (skipNextMouseInputEvent) {
                // This is the path from onGuiMouse
                skipNextMouseInputEvent = false;
                hackInputState(event.getButton());
                return;
            }
            int attackID = Minecraft.getInstance().options.keyAttack.getKey().getValue();
            int useID = Minecraft.getInstance().options.keyUse.getKey().getValue();

            if (event.getButton() == attackID || event.getButton() == useID) {
                if(event.getAction() == 1) {
                    Player.Hand button = attackID == event.getButton() ? Player.Hand.SECONDARY : Player.Hand.PRIMARY;
                    if (!DRAG.executeCancellable(x -> x.apply(button))) {
                        //event.setCanceled(true);
                        hackInputState(event.getButton());
                        dragPos = new Vec3d(0, 0, 0);
                        return;
                    }
                    if (!CLICK.executeCancellable(x -> x.apply(button))) {
                        //event.setCanceled(true);
                        hackInputState(event.getButton());
                    }
                } else {
                    dragPos = null;
                }
            }
        }

        @SubscribeEvent
        public static void onFrame(RenderFrameEvent.Pre event) {
            if (dragPos != null) {
                //Minecraft.getMinecraft().mouseHelper.mouseXYChange();
                dragPos = dragPos.add(Minecraft.getInstance().mouseHandler.getXVelocity(), Minecraft.getInstance().mouseHandler.getYVelocity(), 0);
            }
        }

        public static Vec3d getDragPos() {
            return dragPos;
        }


        @SubscribeEvent
        public static void onDebugRender(CustomizeGuiOverlayEvent.DebugText event) {
            RENDER_DEBUG.execute(x -> x.accept(event));
        }

        @SubscribeEvent
        public static void onOverlayEvent(RenderGuiLayerEvent.Pre event) {
            RENDER_OVERLAY.execute(x -> x.accept(event));
        }

        @SubscribeEvent
        public static void onRenderMouseover(RenderHighlightEvent.Block event) {
            RenderType.cutout().setupRenderState();
            // TODO 1.15+ do we need to set lightmap coords here?
            RENDER_MOUSEOVER.execute(x -> x.accept(event));
            RenderType.cutout().clearRenderState();
        }

        //Call in Mod Bus
//        @SubscribeEvent
//        public static void onSoundLoad(SoundEngineLoadEvent event) {
//            SOUND_LOAD.execute(x -> x.accept(event));
//        }

//        @SubscribeEvent
//        public static void optifineSucksEvent(RenderLevelStageEvent event) {
//            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
//                OPTIFINE_SUCKS.execute(x -> x.accept(event));
//            }
//        }

        static boolean hasHacked = false;
        @SubscribeEvent
        public static void onHackShaders(RenderFrameEvent.Pre event) {
            if (!hasHacked/* && event.phase == TickEvent.Phase.START*/) {
                if (GameRenderer.getRendertypeCutoutShader() != null) {
                    hasHacked = true;
                    HACKS.execute(Runnable::run);
                }
            }
        }

        @SubscribeEvent
        public static void registerModels(ModelEvent.ModifyBakingResult event) {
            MODEL_BAKE.execute(x -> x.accept(event));
        }

        @SubscribeEvent
        public static void onModelBakeEvent(ModelEvent.RegisterAdditional event) {
            MODEL_CREATE.execute(Runnable::run);
        }

        @SubscribeEvent
        public static void onColorSetup(RegisterColorHandlersEvent.Block event) {
            BlockRender.onPostColorSetup(event.getBlockColors());
        }

        @SubscribeEvent
        public static void onTextureStitchEvent(RegisterTextureSpriteEvent event) {
            TEXTURE_STITCH.execute(x -> x.accept(event));
        }

        /*@SubscribeEvent(priority = EventPriority.LOW)
        public static void registerEntities(RegistryEvent.Register<EntityType<?>> event) {
            //REGISTER_ENTITY.execute(Runnable::run);
        }*/

        /*@SubscribeEvent
        public static void buildContents(CreativeModeTabEvent.Register event) {
            List<Object> after = new ArrayList<>();
            after.add(CreativeModeTabs.SPAWN_EGGS);
            CREATIVE_TAB.execute(x -> x.accept(event, after));
        }*/

        @SubscribeEvent
        public static void registerBindings(RegisterKeyMappingsEvent event) {
            KEY_MAPPING_REGISTER.execute(x -> x.accept(event));
        }

        @SubscribeEvent
        public static void registerVanillaShader(RegisterShadersEvent event) {
            REGISTER_SHADER.execute(x -> x.accept(event));
        }

        @SubscribeEvent
        public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
            CLIENT_EXTENSIONS_REGISTER.execute(x -> x.accept(event));
        }

        @SubscribeEvent
        public static void registerMenuScreen(RegisterMenuScreensEvent event) {
            MENU_SCREENS_REGISTER.execute(x -> x.accept(event));
        }

        @SubscribeEvent
        public static void onSoundLoad(SoundEngineLoadEvent event) {
            SOUND_LOAD.execute(x -> x.accept(event));
        }
    }
}
