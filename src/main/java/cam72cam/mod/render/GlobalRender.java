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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.culling.ClippingHelperImpl;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

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
            if (Minecraft.getInstance().gameSettings.showDebugInfo && GPUInfo.hasGPUInfo()) {
                int i;
                for (i = 0; i < event.getRight().size(); i++) {
                    if (event.getRight().get(i).startsWith("Display: ")) {
                        i++;
                        break;
                    }
                }
                event.getRight().add(i, GPUInfo.debug());
            }
        });
    }

    /** Register a function that is called (with partial ticks) during the Block Entity render phase */
    public static void registerRender(RenderFunction func) {
        renderFuncs.add(func);
    }

    /** Register a function that is called (with partial ticks) during the UI render phase */
    public static void registerOverlay(RenderFunction func) {
        ClientEvents.RENDER_OVERLAY.subscribe(event -> {
            if (event.getType() == RenderGameOverlayEvent.ElementType.ALL) {
                func.render(new RenderState().stage(RenderContext.Stage.GUI), event.getPartialTicks());
            }
        });
    }

    /** Register a function that is called to render during the mouse over phase (only if a block is moused over) */
    public static void registerItemMouseover(CustomItem item, MouseoverEvent fn) {
        ClientEvents.RENDER_MOUSEOVER.subscribe(partialTicks -> {
            if (MinecraftClient.getBlockMouseOver() != null) {
                Player player = MinecraftClient.getPlayer();
                if (item.internal == player.getHeldItem(Player.Hand.PRIMARY).internal.getItem()) {
                    fn.render(player, player.getHeldItem(Player.Hand.PRIMARY), MinecraftClient.getBlockMouseOver(), MinecraftClient.getPosMouseOver(),
                              new RenderState().stage(RenderContext.Stage.OVERLAY), partialTicks);
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
        return new Vec3d(Minecraft.getInstance().gameRenderer.getActiveRenderInfo().getProjectedView());
    }

    /** Internal camera helper */
    static ICamera getCamera(float partialTicks) {
        ClippingHelperImpl ch = new ClippingHelperImpl();
        ch.init();
        ICamera camera = new Frustum(ch); // Must be new instance per Johni0702 otherwise will be affected by weird global state!
        Vec3d cameraPos = getCameraPos(partialTicks);
        camera.setPosition(cameraPos.x, cameraPos.y, cameraPos.z);
        return camera;
    }

    /** Return the render distance in meters */
    public static int getRenderDistance() {
        return Minecraft.getInstance().gameSettings.renderDistanceChunks * 16;
    }


    /** Similar to drawNameplate */
    public static void drawText(String str, RenderState state, Vec3d pos, float scale, float rotate)
    {
        EntityRendererManager renderManager = Minecraft.getInstance().getRenderManager();
        float viewerYaw = renderManager.playerViewY + rotate;
        float viewerPitch = renderManager.playerViewX;
        boolean isThirdPersonFrontal = renderManager.options.thirdPersonView == 2;

        FontRenderer fontRendererIn = Minecraft.getInstance().fontRenderer;

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
            fontRendererIn.drawString(str, -fontRendererIn.getStringWidth(str) / 2, 0, -1);
        }
    }

    /** Draws centered text (does not rotate towards player) */
    public static void drawRawCenteredText(String str, RenderState state, int color)
    {
        FontRenderer fontRendererIn = Minecraft.getInstance().fontRenderer;

        state.color(1,1,1,1).alpha_test(true).stage(RenderContext.Stage.OVERLAY_TEXT);

        try (With ignored = RenderContext.apply(state)) {
            fontRendererIn.drawString(str, -fontRendererIn.getStringWidth(str) / 2, 0, color);
        }
    }

    /** Draws left-oriented text (does not rotate towards player) */
    public static void drawRawLeftOrientedText(String str, RenderState state, int color)
    {
        FontRenderer fontRendererIn = Minecraft.getInstance().fontRenderer;

        state.color(1,1,1,1).alpha_test(true);
        state.stage(RenderContext.Stage.OVERLAY_TEXT);

        try (With ignored = RenderContext.apply(state)) {
            fontRendererIn.drawString(str, 0, 0, color);
        }
    }

    /** Draws right-oriented text (does not rotate towards player) */
    public static void drawRawRightOrientedText(String str, RenderState state, int color)
    {
        FontRenderer fontRendererIn = Minecraft.getInstance().fontRenderer;

        state.color(1,1,1,1).alpha_test(true).stage(RenderContext.Stage.OVERLAY_TEXT);

        try (With ignored = RenderContext.apply(state)) {
            fontRendererIn.drawString(str, -fontRendererIn.getStringWidth(str), 0, color);
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
