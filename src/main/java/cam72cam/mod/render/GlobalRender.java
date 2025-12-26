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
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.math.Matrix4f;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.settings.PointOfView;
import net.minecraft.util.math.vector.Matrix4f;
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
            if (Minecraft.getInstance().options.renderDebug && GPUInfo.hasGPUInfo()) {
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
                func.render(new RenderState(event.getMatrixStack()).stage(RenderContext.Stage.GUI), event.getPartialTicks());
            }
        });
    }

    /** Register a function that is called to render during the mouse over phase (only if a block is moused over) */
    public static void registerItemMouseover(CustomItem item, MouseoverEvent fn) {
        ClientEvents.RENDER_MOUSEOVER.subscribe((event) -> {
            if (MinecraftClient.getBlockMouseOver() != null) {
                Player player = MinecraftClient.getPlayer();
                if (item.internal == player.getHeldItem(Player.Hand.PRIMARY).internal.getItem()) {
                    fn.render(player, player.getHeldItem(Player.Hand.PRIMARY), MinecraftClient.getBlockMouseOver().down(), MinecraftClient.getPosMouseOver(),
                              new RenderState(event.getMatrix()).stage(RenderContext.Stage.OVERLAY), event.getPartialTicks());
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
        return Minecraft.getInstance().options.renderDistance * 16;
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
                .translate(pos.x, pos.y, pos.z)
                .rotate(-viewerYaw, 0.0F, 1.0F, 0.0F)
                .rotate((float) (isThirdPersonFrontal ? -1 : 1) * viewerPitch, 1.0F, 0.0F, 0.0F)
                .scale(scale, scale, scale)
                .scale(-0.025F, -0.025F, 0.025F)
                .stage(RenderContext.Stage.OVERLAY_TEXT);

        Matrix4f matrix4f = state.model_view().convertToMoj();
        try (With ctx = RenderContext.apply(new RenderState().lighting(false).depth_test(false))) {
            MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
            fontRendererIn.drawInBatch(str, -fontRendererIn.width(str) / 2, 0, -1,
                                        false, matrix4f, buffer, true, 0, 15728640);
            buffer.endBatch();
        }
    }

    /** Draws centered text (does not rotate towards player) */
    public static void drawRawCenteredText(String str, RenderState state, int color)
    {
        Font fontRendererIn = Minecraft.getInstance().font;

        state.color(1,1,1,1).alpha_test(true).stage(RenderContext.Stage.OVERLAY_TEXT);

        try (With ignored = RenderContext.apply(state)) {
            fontRendererIn.draw(new PoseStack(), str, -fontRendererIn.width(str) / 2, 0, color);
        }
    }

    /** Draws left-oriented text (does not rotate towards player) */
    public static void drawRawLeftOrientedText(String str, RenderState state, int color)
    {
        Font fontRendererIn = Minecraft.getInstance().font;

        state.color(1,1,1,1).alpha_test(true);
        state.stage(RenderContext.Stage.OVERLAY_TEXT);

        try (With ignored = RenderContext.apply(state)) {
            fontRendererIn.draw(new PoseStack(), str, 0, 0, color);
        }
    }

    /** Draws right-oriented text (does not rotate towards player) */
    public static void drawRawRightOrientedText(String str, RenderState state, int color)
    {
        Font fontRendererIn = Minecraft.getInstance().font;

        state.color(1,1,1,1).alpha_test(true).stage(RenderContext.Stage.OVERLAY_TEXT);

        try (With ignored = RenderContext.apply(state)) {
            fontRendererIn.draw(new PoseStack(), str, -fontRendererIn.width(str), 0, color);
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
