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
import cam72cam.mod.render.opengl.RenderContext;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.render.opengl.VBO;
import cam72cam.mod.sound.Audio;
import cam72cam.mod.world.World;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.event.sound.SoundEngineLoadEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

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
    public static final Event<Consumer<ModelEvent.ModifyBakingResult>> MODEL_BAKE = new Event<>();
    public static final Event<Consumer<TextureStitchEvent>> TEXTURE_STITCH = new Event<>();
    public static final Event<Runnable> HACKS = new Event<>();
    public static final Event<Runnable> REGISTER_ENTITY = new Event<>();
    public static final Event<Consumer<CustomizeGuiOverlayEvent.DebugText>> RENDER_DEBUG = new Event<>();
    public static final Event<Consumer<RenderGuiOverlayEvent.Pre>> RENDER_OVERLAY = new Event<>();
    public static final Event<Consumer<RenderHighlightEvent.Block>> RENDER_MOUSEOVER = new Event<>();
    public static final Event<Consumer<SoundEngineLoadEvent>> SOUND_LOAD = new Event<>();
    public static final Event<Runnable> RELOAD = new Event<>();
    public static final Event<Consumer<RenderLevelStageEvent>> OPTIFINE_SUCKS = new Event<>();

    @Mod.EventBusSubscriber(modid = ModCore.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientEventBusForge {
        private static Vec3d dragPos = null;
        private static boolean skipNextMouseInputEvent = false;

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            TICK.execute(Runnable::run);
        }

        private static void onGuiMouse(ScreenEvent event, int x, int y, int btn, MouseAction action) {
            MouseGuiEvent mevt = new MouseGuiEvent(action, x, y, btn);
            if (!MOUSE_GUI.executeCancellable(h -> h.apply(mevt))) {
                event.setCanceled(true);
                // Apparently cancelling this input event only cancels it for the *GUI* handlers, not all input handlers
                // Therefore we need to track that ourselves.  Thanks for changing that from 1.12.2-forge
                skipNextMouseInputEvent = true;
            }
        }

        @SubscribeEvent
        public static void onGuiClick(ScreenEvent.MouseButtonPressed.Pre event) {
            onGuiMouse(event, (int) event.getMouseX(), (int) event.getMouseY(), event.getButton(), MouseAction.CLICK);
        }
        @SubscribeEvent
        public static void onGuiDrag(ScreenEvent.MouseDragged.Pre event) {
            onGuiMouse(event, (int) event.getMouseX(), (int) event.getMouseY(), event.getMouseButton(), MouseAction.RELEASE);
        }
        @SubscribeEvent
        public static void onGuiRelease(ScreenEvent.MouseButtonReleased.Pre event) {
            onGuiMouse(event, (int) event.getMouseX(), (int) event.getMouseY(), event.getButton(), MouseAction.RELEASE);
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
        public static void onClick(InputEvent.MouseButton event) {
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
        public static void onFrame(TickEvent.RenderTickEvent event) {
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
        public static void onOverlayEvent(RenderGuiOverlayEvent.Pre event) {
            RENDER_OVERLAY.execute(x -> x.accept(event));
        }

        @SubscribeEvent
        public static void onRenderMouseover(RenderHighlightEvent.Block event) {
            RenderType.cutout().setupRenderState();
            // TODO 1.15+ do we need to set lightmap coords here?
            RENDER_MOUSEOVER.execute(x -> x.accept(event));
            RenderType.cutout().clearRenderState();
        }

        @SubscribeEvent
        public static void onSoundLoad(SoundEngineLoadEvent event) {
            SOUND_LOAD.execute(x -> x.accept(event));
        }

        @SubscribeEvent
        public static void optifineSucksEvent(RenderLevelStageEvent event) {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
                OPTIFINE_SUCKS.execute(x -> x.accept(event));
            }
        }

        static boolean hasHacked = false;
        @SubscribeEvent
        public static void onHackShaders(TickEvent.RenderTickEvent event) {
            if (!hasHacked && event.phase == TickEvent.Phase.START) {
                if (GameRenderer.getRendertypeCutoutShader() != null) {
                    hasHacked = true;
                    HACKS.execute(Runnable::run);
                }
            }
        }
    }

    @Mod.EventBusSubscriber(modid = ModCore.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientEventBusMod {
        static {
            registerClientEvents();
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
        public static void onTextureStitchEvent(TextureStitchEvent event) {
            TEXTURE_STITCH.execute(x -> x.accept(event));
        }

        /*@SubscribeEvent(priority = EventPriority.LOW)
        public static void registerEntities(RegistryEvent.Register<EntityType<?>> event) {
            //REGISTER_ENTITY.execute(Runnable::run);
        }*/
    }
}
