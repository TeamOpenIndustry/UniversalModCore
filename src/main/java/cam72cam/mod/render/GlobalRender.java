package cam72cam.mod.render;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.*;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.item.CustomItem;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import friedrichlp.renderlib.library.RenderMode;
import friedrichlp.renderlib.math.TVector3;
import friedrichlp.renderlib.math.Vector3;
import friedrichlp.renderlib.tracking.RenderManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.ClippingHelperImpl;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/** Global Render Registry and helper functions */
public class GlobalRender {
    // Fire these off every tick
    private static List<Consumer<Float>> blockRenderFuncs = new ArrayList<>();
    private static List<Consumer<Float>> entityRenderFuncs = new ArrayList<>();

    // Internal hack
    private static List<TileEntity> brhList = Collections.singletonList(new BlockRenderHelper());

    private static EntityRenderHelper erh;

    /** Updates all properties related to RenderLib so that the next frame can be rendered */
    private static void updateRenderLib() {
        TVector3 camPos = TVector3.create((float)TileEntityRendererDispatcher.staticPlayerX,
                                          (float)TileEntityRendererDispatcher.staticPlayerY,
                                          (float)TileEntityRendererDispatcher.staticPlayerZ);
        RenderManager.setCameraPos(camPos);
        RenderManager.setRenderDistance(getRenderDistance());
    }

    /** Internal, hooked into event system directly */
    public static void registerClientEvents() {
        // Beacon like hack for always running a single global render during the TE render phase
        ClientEvents.REGISTER_ENTITY.subscribe(() -> {
            ClientRegistry.bindTileEntitySpecialRenderer(BlockRenderHelper.class, new TileEntitySpecialRenderer<BlockRenderHelper>() {
                @Override
                public void render(BlockRenderHelper te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
                    blockRenderFuncs.forEach(r -> r.accept(partialTicks));

                    updateRenderLib();
                    RenderManager.render(Layers.TILES, RenderMode.USE_CUSTOM_MATS);
                }

                @Override
                public boolean isGlobalRenderer(BlockRenderHelper te) {
                    return true;
                }
            });

            RenderingRegistry.registerEntityRenderingHandler(EntityRenderHelper.class, manager -> new Render<EntityRenderHelper>(manager) {
                @Nullable
                @Override
                protected ResourceLocation getEntityTexture(EntityRenderHelper entity) {return null;}

                @Override
                public void doRender(EntityRenderHelper entity, double x, double y, double z, float entityYaw, float partialTicks) {
                    entityRenderFuncs.forEach(r -> r.accept(partialTicks));

                    updateRenderLib();
                    RenderManager.render(Layers.ENTITY, RenderMode.USE_CUSTOM_MATS);
                }
            });
        });
        ClientEvents.TICK.subscribe(() -> {
            if (!MinecraftClient.isReady()) {
                RenderManager.update(); // called in title screen
                return;
            }

            Player player = MinecraftClient.getPlayer();

            Minecraft.getMinecraft().renderGlobal.updateTileEntities(brhList, brhList);
            if (player != null) {  // May be able to get away with running this every N ticks?
               brhList.get(0).setPos(new BlockPos(player.getPositionEyes().internal()));
            }

            World world = player.getWorld().internal;
            if (world != null) {
                if (erh == null) {
                    erh = new EntityRenderHelper(world);
                    world.spawnEntity(erh);
                }

                if (erh != null) {
                    if (erh.isDead) {
                        erh.isDead = false;
                        world.spawnEntity(erh);
                    }

                    Vec3d pos = player.getPositionEyes();
                    pos = pos.add(player.getViewDirection().scale(2));
                    erh.setPosition(pos.x, pos.y, pos.z);
                }
            }
        });


        // Nice to have GPU info in F3
        ClientEvents.RENDER_DEBUG.subscribe(event -> {
            if (Minecraft.getMinecraft().gameSettings.showDebugInfo && GPUInfo.hasGPUInfo()) {
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

        CommonEvents.World.UNLOAD.subscribe(world -> {
            if (!world.isRemote) {
                RenderManager.clear();
            }
        });
    }

    /** Register a function that is called (with partial ticks) during the Block Entity render phase */
    public static void registerBlockRender(Consumer<Float> func) {
        blockRenderFuncs.add(func);
    }

    /** Register a function that is called (with partial ticks) during the Entity render phase */
    public static void registerEntityRender(Consumer<Float> func) {
        entityRenderFuncs.add(func);
    }

    /** Register a function that is called (with partial ticks) during the UI render phase */
    public static void registerOverlay(Consumer<Float> func) {
        ClientEvents.RENDER_OVERLAY.subscribe(event -> {
            if (event.getType() == RenderGameOverlayEvent.ElementType.ALL) {
                func.accept(event.getPartialTicks());
            }
        });
    }

    /** Register a function that is called to render during the mouse over phase (only if a block is moused over) */
    public static void registerItemMouseover(CustomItem item, MouseoverEvent fn) {
        ClientEvents.RENDER_MOUSEOVER.subscribe(partialTicks -> {
            if (MinecraftClient.getBlockMouseOver() != null) {
                Player player = MinecraftClient.getPlayer();
                if (item.internal == player.getHeldItem(Player.Hand.PRIMARY).internal.getItem()) {
                    fn.render(player, player.getHeldItem(Player.Hand.PRIMARY), MinecraftClient.getBlockMouseOver(), MinecraftClient.getPosMouseOver(), partialTicks);
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
        net.minecraft.entity.Entity playerrRender = Minecraft.getMinecraft().getRenderViewEntity();
        double d0 = playerrRender.lastTickPosX + (playerrRender.posX - playerrRender.lastTickPosX) * partialTicks;
        double d1 = playerrRender.lastTickPosY + (playerrRender.posY - playerrRender.lastTickPosY) * partialTicks;
        double d2 = playerrRender.lastTickPosZ + (playerrRender.posZ - playerrRender.lastTickPosZ) * partialTicks;
        return new Vec3d(d0, d1, d2);
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
        return Minecraft.getMinecraft().gameSettings.renderDistanceChunks * 16;
    }

    @FunctionalInterface
    public interface MouseoverEvent {
        void render(Player player, ItemStack stack, Vec3i pos, Vec3d offset, float partialTicks);
    }

    public static class EntityRenderHelper extends Entity {
        public EntityRenderHelper(World world) {
            super(world);
        }

        @Override
        protected void entityInit() {}

        @Override
        protected void readEntityFromNBT(NBTTagCompound compound) {}

        @Override
        protected void writeEntityToNBT(NBTTagCompound compound) {}

        @Override
        public boolean canBeCollidedWith() {
            return false;
        }
    }

    public static class BlockRenderHelper extends TileEntity {
        public net.minecraft.util.math.AxisAlignedBB getRenderBoundingBox() {
            return INFINITE_EXTENT_AABB;
        }

        @Override
        public double getMaxRenderDistanceSquared() {
            return Double.POSITIVE_INFINITY;
        }

        public double getDistanceSq(double x, double y, double z) {
            return 1;
        }

        public boolean shouldRenderInPass(int pass) {
            return true;
        }
    }
}
