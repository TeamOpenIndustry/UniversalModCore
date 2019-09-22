package cam72cam.mod.render;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ItemBase;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Hand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustrum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class GlobalRender {
    private static List<Consumer<Float>> renderFuncs = new ArrayList<>();
    private static List<Consumer<Float>> overlayFuncs = new ArrayList<>();
    private static Map<ItemBase, MouseoverEvent> itemMouseovers = new HashMap<>();
    private static TileEntity grh = new GlobalRenderHelper();
    private static List<TileEntity> grhList = new ArrayList<>();

    static {
        grhList.add(grh);
    }

    public static void registerGlobalRenderer() {
        ClientRegistry.bindTileEntitySpecialRenderer(GlobalRenderHelper.class, new TileEntitySpecialRenderer() {
            @Override
            public void renderTileEntityAt(TileEntity te, double x, double y, double z, float partialTicks) {
                renderFuncs.forEach(r -> r.accept(partialTicks));
            }
        });
    }

    public static class EventBus {
        @SubscribeEvent
        public void onRenderMouseover(DrawBlockHighlightEvent event) {
            Player player = MinecraftClient.getPlayer();

            if (event.target.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                Vec3i pos = new Vec3i(event.target.blockX, event.target.blockY, event.target.blockZ);
                for (ItemBase item : itemMouseovers.keySet()) {
                    ItemStack held = player.getHeldItem(Hand.PRIMARY);
                    if (!held.isEmpty() && item.internal == held.internal.getItem()) {
                        itemMouseovers.get(item).render(player, held, pos, new Vec3d(event.target.hitVec), event.partialTicks);
                    }
                }
            }
        }

        @SubscribeEvent
        public void onOverlayEvent(RenderGameOverlayEvent.Pre event) {
            if (event.type == RenderGameOverlayEvent.ElementType.ALL) {
                overlayFuncs.forEach(x -> x.accept(event.partialTicks));
            }
        }
    }

    public static void registerRender(Consumer<Float> func) {
        renderFuncs.add(func);
    }

    public static void registerOverlay(Consumer<Float> func) {
        overlayFuncs.add(func);
    }

    public static void registerItemMouseover(ItemBase item, MouseoverEvent fn) {
        itemMouseovers.put(item, fn);
    }

    public static boolean isTransparentPass() {
        return MinecraftForgeClient.getRenderPass() != 0;
    }

    /* Removed 1.7.10
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }

        Minecraft.getMinecraft().renderGlobal.updateTileEntities(grhList, grhList);
    }
    */

    public static Vec3d getCameraPos(float partialTicks) {
        net.minecraft.entity.Entity playerrRender = Minecraft.getMinecraft().renderViewEntity;
        double d0 = playerrRender.lastTickPosX + (playerrRender.posX - playerrRender.lastTickPosX) * partialTicks;
        double d1 = playerrRender.lastTickPosY + (playerrRender.posY - playerrRender.lastTickPosY) * partialTicks;
        double d2 = playerrRender.lastTickPosZ + (playerrRender.posZ - playerrRender.lastTickPosZ) * partialTicks;
        return new Vec3d(d0, d1, d2);
    }

    static ICamera getCamera(float partialTicks) {
        ICamera camera = new Frustrum();
        Vec3d cameraPos = getCameraPos(partialTicks);
        camera.setPosition(cameraPos.x, cameraPos.y, cameraPos.z);
        return camera;
    }

    @SubscribeEvent
    public static void onDebugRender(RenderGameOverlayEvent.Text event) {
        if (Minecraft.getMinecraft().gameSettings.showDebugInfo && GPUInfo.hasGPUInfo()) {
            int i;
            for (i = 0; i < event.right.size(); i++) {
                if (event.right.get(i).startsWith("Display: ")) {
                    i++;
                    break;
                }
            }
            event.right.add(i, GPUInfo.debug());
        }
    }

    public static boolean isInRenderDistance(Vec3d pos) {
        // max rail length is 100, 50 is center
        return MinecraftClient.getPlayer().getPosition().distanceTo(pos) < ((Minecraft.getMinecraft().gameSettings.renderDistanceChunks + 1) * 16 + 50);
    }

    @FunctionalInterface
    public interface MouseoverEvent {
        void render(Player player, ItemStack stack, Vec3i pos, Vec3d offset, float partialTicks);
    }

    public static class GlobalRenderHelper extends TileEntity {

        public net.minecraft.util.AxisAlignedBB getRenderBoundingBox() {
            return INFINITE_EXTENT_AABB;
        }

        public double getDistanceSq(double x, double y, double z) {
            return 1;
        }

        public boolean shouldRenderInPass(int pass) {
            return true;
        }

    }
}
