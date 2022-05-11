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
import cpw.mods.fml.client.registry.ClientRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.culling.Frustrum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import java.util.ArrayList;
import java.util.List;

/** Global Render Registry and helper functions */
public class GlobalRender {
    // Fire these off every tick
    private static List<RenderFunction> renderFuncs = new ArrayList<>();

    /** Internal, hooked into event system directly */
    public static void registerClientEvents() {
        // Beacon like hack for always running a single global render during the TE render phase
        ClientEvents.REGISTER_ENTITY.subscribe(() -> {
            ClientRegistry.bindTileEntitySpecialRenderer(GlobalRenderHelper.class, new TileEntitySpecialRenderer() {
                @Override
                public void renderTileEntityAt(TileEntity te, double x, double y, double z, float partialTicks) {
                    renderFuncs.forEach(r -> r.render(new RenderState(), partialTicks));
                }
            });
        });
        GlobalRenderHelper grh = new GlobalRenderHelper();
        ClientEvents.TICK.subscribe(() -> {
            if (Minecraft.getMinecraft().thePlayer != null && Minecraft.getMinecraft().thePlayer.worldObj != null) {  // May be able to get away with running this every N ticks?
                Vec3i eyes = new Vec3i(MinecraftClient.getPlayer().getPositionEyes());
                grh.xCoord = eyes.x;
                grh.yCoord = eyes.y;
                grh.zCoord = eyes.z;
                grh.setWorldObj(Minecraft.getMinecraft().thePlayer.worldObj);
                Minecraft.getMinecraft().renderGlobal.tileEntities.remove(grh);
                Minecraft.getMinecraft().renderGlobal.tileEntities.add(grh);
            }
        });

        // Nice to have GPU info in F3
        ClientEvents.RENDER_DEBUG.subscribe(event -> {
            if (Minecraft.getMinecraft().gameSettings.showDebugInfo && GPUInfo.hasGPUInfo()) {
                int i;
                for (i = 0; i < event.right.size(); i++) {
                    if (event.right.get(i) != null && event.right.get(i).startsWith("Display: ")) {
                        i++;
                        break;
                    }
                }
                event.right.add(i, GPUInfo.debug());
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
            if (event.type == RenderGameOverlayEvent.ElementType.HOTBAR) {
                func.render(new RenderState(), event.partialTicks);
            }
        });
    }

    /** Register a function that is called to render during the mouse over phase (only if a block is moused over) */
    public static void registerItemMouseover(CustomItem item, MouseoverEvent fn) {
        ClientEvents.RENDER_MOUSEOVER.subscribe(partialTicks -> {
            if (MinecraftClient.getBlockMouseOver() != null) {
                Player player = MinecraftClient.getPlayer();
                if (!player.getHeldItem(Player.Hand.PRIMARY).isEmpty() && item.internal == player.getHeldItem(Player.Hand.PRIMARY).internal.getItem()) {
                    fn.render(player, player.getHeldItem(Player.Hand.PRIMARY), MinecraftClient.getBlockMouseOver(), MinecraftClient.getPosMouseOver(), new RenderState(), partialTicks);
                }
            }
        });
    }

    /** Is MC in the Transparent Render Pass? */
    public static boolean isTransparentPass() {
        return MinecraftForgeClient.getRenderPass() != 0;
    }

    /** Get global position of the player's eyes (with partialTicks taken into account) */
    public static Vec3d getCameraPos(float partialTicks) {
        net.minecraft.entity.Entity playerrRender = Minecraft.getMinecraft().renderViewEntity;
        double d0 = playerrRender.lastTickPosX + (playerrRender.posX - playerrRender.lastTickPosX) * partialTicks;
        double d1 = playerrRender.lastTickPosY + (playerrRender.posY - playerrRender.lastTickPosY) * partialTicks;
        double d2 = playerrRender.lastTickPosZ + (playerrRender.posZ - playerrRender.lastTickPosZ) * partialTicks;
        return new Vec3d(d0, d1, d2);
    }

    /** Internal camera helper */
    static ICamera getCamera(float partialTicks) {
        ICamera camera = new Frustrum();
        Vec3d cameraPos = getCameraPos(partialTicks);
        camera.setPosition(cameraPos.x, cameraPos.y, cameraPos.z);
        return camera;
    }

    /** Return the render distance in meters */
    public static int getRenderDistance() {
        return Minecraft.getMinecraft().gameSettings.renderDistanceChunks * 16;
    }


    /** Similar to drawNameplate */
    public static void drawText(String str, RenderState state, Vec3d pos, float scale, float rotate)
    {
        RenderManager renderManager = RenderManager.instance;
        float viewerYaw = renderManager.playerViewY + rotate;
        float viewerPitch = renderManager.playerViewX;
        boolean isThirdPersonFrontal = renderManager.options.thirdPersonView == 2;

        FontRenderer fontRendererIn = Minecraft.getMinecraft().fontRendererObj;

        state = state.clone()
                .lighting(false)
                .depth_test(false)
                .color(1, 1, 1, 1)
                .translate(pos.x, pos.y, pos.z)
                .rotate(-viewerYaw, 0.0F, 1.0F, 0.0F)
                .rotate((float) (isThirdPersonFrontal ? -1 : 1) * viewerPitch, 1.0F, 0.0F, 0.0F)
                .scale(scale, scale, scale)
                .scale(-0.025F, -0.025F, 0.025F);

        try (With ctx = RenderContext.apply(state)) {
            fontRendererIn.drawString(str, -fontRendererIn.getStringWidth(str) / 2, 0, -1);
        }
    }

    @FunctionalInterface
    public interface MouseoverEvent {
        void render(Player player, ItemStack stack, Vec3i pos, Vec3d offset, RenderState state, float partialTicks);
    }

    public static class GlobalRenderHelper extends TileEntity {

        @Override
        public net.minecraft.util.AxisAlignedBB getRenderBoundingBox() {
            return INFINITE_EXTENT_AABB;
        }

        @Override
        public double getMaxRenderDistanceSquared() {
            return Double.POSITIVE_INFINITY;
        }

        @Override
        public double getDistanceSq(double x, double y, double z) {
            return 1;
        }

        @Override
        public boolean shouldRenderInPass(int pass) {
            return true;
        }

    }
}
