package cam72cam.mod.render;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Player;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.item.CustomItem;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.render.opengl.RenderContext;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.util.With;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/** Global Render Registry and helper functions */
public class GlobalRender {
    // Fire these off every tick
    private static final List<RenderFunction> renderFuncs = new ArrayList<>();

    /** Internal, hooked into event system directly */
    public static void registerClientEvents() {
        // Nice to have GPU info in F3
        ClientEvents.RENDER_DEBUG.subscribe(event -> {
            if (Minecraft.getInstance().getDebugOverlay().showDebugScreen()
                    && event.getSide() == CustomizeGuiOverlayEvent.DebugText.Side.Right && GPUInfo.hasGPUInfo()) {
                int i;
                for (i = 0; i < event.getText().size(); i++) {
                    if (event.getText().get(i).startsWith("Display: ")) {
                        i++;
                        break;
                    }
                }
                event.getText().add(i, GPUInfo.debug());
            }
        });
    }

    /** Register a function that is called (with partial ticks) during the Block Entity render phase */
    public static void registerRender(RenderFunction func) {
        renderFuncs.add(func);
    }

    /** Register a function that is called (with partial ticks) during the UI render phase */
    public static void registerOverlay(RenderFunction func) { // TODO
        //ClientEvents.RENDER_OVERLAY.subscribe(event -> {
        //    if (event.getName().equals(VanillaGuiLayers.HOTBAR) && !Minecraft.getInstance().options.hideGui) {
        //        func.render(new RenderState(event.getGuiGraphics().pose()).stage(RenderContext.Stage.GUI), event.getPartialTick().getRealtimeDeltaTicks());
        //    }
        //});
    }

    /** Register a function that is called to render during the mouse over phase (only if a block is moused over) */
    public static void registerItemMouseover(CustomItem item, MouseoverEvent fn) {
        ClientEvents.RENDER_MOUSEOVER.subscribe((event) -> {
            if (MinecraftClient.getBlockMouseOver() != null) {
                Player player = MinecraftClient.getPlayer();
                if (item.internal == player.getHeldItem(Player.Hand.PRIMARY).internal().getItem()) {
                    fn.render(player, player.getHeldItem(Player.Hand.PRIMARY), MinecraftClient.getBlockMouseOver().down(),
                              MinecraftClient.getPosMouseOver(), new RenderState(event.getPoseStack()).stage(RenderContext.Stage.OVERLAY), event.getPartialTick());
                }
            }
        });
    }

    /** Is MC in the Transparent Render Pass? */
    public static boolean isTransparentPass() {
        return false;//MinecraftForgeClient.getRenderPass() != 0;
    }

    /** Get global position of the player's eyes (with partialTicks taken into account) */
    public static Vec3d getCameraPos(float partialTicks) {
        return new Vec3d(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition());
    }

    /** Internal camera helper */
    static Camera getCamera(float partialTicks) {
        return new Camera() {
            {
                setPosition(getCameraPos(partialTicks).internal());
            }
        };
    }

    /** Return the render distance in meters */
    public static int getRenderDistance() {
        return Minecraft.getInstance().options.renderDistance().get() * 16;
    }

    /** Similar to drawNameplate */
    public static void drawText(String str, RenderState state, Vec3d pos, float scale, float rotate)
    {
        EntityRenderDispatcher renderManager = Minecraft.getInstance().getEntityRenderDispatcher();
        float viewerYaw = renderManager.camera.getYRot() + rotate;
        float viewerPitch = renderManager.camera.getXRot();
        boolean isThirdPersonFrontal = renderManager.options.getCameraType() == CameraType.THIRD_PERSON_FRONT;

        Font fontRendererIn = Minecraft.getInstance().font;

        state = state.clone()
                     .lighting(false)
                     .depth_test(false)
                     .color(1, 1, 1, 1)
                     .translate(pos.x, pos.y, pos.z)
                     .rotate(-viewerYaw, 0.0F, 1.0F, 0.0F)
                     .rotate((float) (isThirdPersonFrontal ? -1 : 1) * viewerPitch, 1.0F, 0.0F, 0.0F)
                     .scale(scale, scale, scale)
                     .scale(-0.025F, -0.025F, 0.025F)
                     .stage(RenderContext.Stage.OVERLAY_TEXT);

        try (With ctx = RenderContext.apply(state)) {
            fontRendererIn.drawInBatch(str, -fontRendererIn.width(str) / 2, 0, -1, false, new Matrix4f(), RenderContext.IMMEDIATE, Font.DisplayMode.SEE_THROUGH, 0, 15728880, fontRendererIn.isBidirectional());
            RenderContext.IMMEDIATE.endBatch();
        }
    }

    /** Draws centered text (does not rotate towards player) */
    public static void drawRawCenteredText(String str, RenderState state, int color)
    {
        Font fontRendererIn = Minecraft.getInstance().font;

        state.color(1,1,1,1).alpha_test(true).stage(RenderContext.Stage.OVERLAY_TEXT);

        try (With ignored = RenderContext.apply(state)) {
            fontRendererIn.drawInBatch(str, -fontRendererIn.width(str) / 2f, 0, color, false, new Matrix4f(),
                                       RenderContext.IMMEDIATE, Font.DisplayMode.NORMAL, 0, 15728880,
                                       fontRendererIn.isBidirectional());
            RenderContext.IMMEDIATE.endBatch();
        }
    }

    /** Draws left-oriented text (does not rotate towards player) */
    public static void drawRawLeftOrientedText(String str, RenderState state, int color)
    {
        Font fontRendererIn = Minecraft.getInstance().font;

        state.color(1,1,1,1).alpha_test(true);
        state.stage(RenderContext.Stage.OVERLAY_TEXT);

        try (With ignored = RenderContext.apply(state)) {
            fontRendererIn.drawInBatch(str, 0, 0, color, false, new Matrix4f(),
                                       RenderContext.IMMEDIATE, Font.DisplayMode.NORMAL, 0, 15728880,
                                       fontRendererIn.isBidirectional());
            RenderContext.IMMEDIATE.endBatch();
        }
    }

    /** Draws right-oriented text (does not rotate towards player) */
    public static void drawRawRightOrientedText(String str, RenderState state, int color)
    {
        Font fontRendererIn = Minecraft.getInstance().font;

        state.color(1,1,1,1).alpha_test(true).stage(RenderContext.Stage.OVERLAY_TEXT);

        try (With ignored = RenderContext.apply(state)) {
            fontRendererIn.drawInBatch(str, -fontRendererIn.width(str), 0, color, false, new Matrix4f(),
                                       RenderContext.IMMEDIATE, Font.DisplayMode.NORMAL, 0, 15728880,
                                       fontRendererIn.isBidirectional());
            RenderContext.IMMEDIATE.endBatch();
        }
    }

    /**
     * Converts rgb to single 24bit-integer (can be used for methods using 'int color')
     * Values should be between 0 and 255, there is no validation.
     */
    public static int rgbToInt(int red, int green, int blue){

        return 255 << 24 | red << 16 | green << 8 | blue;
    }

    @FunctionalInterface
    public interface MouseoverEvent {
        void render(Player player, ItemStack stack, Vec3i pos, Vec3d offset, RenderState state, float partialTicks);
    }

    /** Internal, don't use*/
    public static void renderGlobalFuncs(RenderState state, float partialTicks) {
        renderFuncs.forEach(r -> r.render(state, partialTicks));
    }
}
