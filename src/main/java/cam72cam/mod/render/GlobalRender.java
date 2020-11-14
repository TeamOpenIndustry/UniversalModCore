package cam72cam.mod.render;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Player;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.item.CustomItem;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.render.BlockEntityRendererRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/** Global Render Registry and helper functions */
public class GlobalRender {
    // Fire these off every tick
    private static final List<Consumer<Float>> renderFuncs = new ArrayList<>();

    /** Internal, hooked into event system directly */
    public static void registerClientEvents() {
        // Beacon like hack for always running a single global render during the TE render phase
        ClientEvents.REGISTER_ENTITY.subscribe(() -> {
            BlockEntityRendererRegistry.INSTANCE.register(GlobalRenderHelper.class, new BlockEntityRenderer<GlobalRenderHelper>() {
                @Override
                public void render(GlobalRenderHelper te, double x, double y, double z, float partialTicks, int destroyStage) {
                    net.minecraft.client.MinecraftClient.getInstance().getTextureManager().bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
                    renderFuncs.forEach(r -> r.accept(partialTicks));
                }

                @Override
                public boolean method_3563(GlobalRenderHelper te) {
                    return true;
                }
            });
        });

        List<BlockEntity> grhList = Collections.singletonList(new GlobalRenderHelper());

        ClientEvents.TICK.subscribe(() -> {
            net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
            mc.worldRenderer.updateNoCullingBlockEntities(grhList, grhList);
            if (MinecraftClient.isReady()) {  // May be able to get away with running this every N ticks?
                grhList.get(0).setPos(new BlockPos(MinecraftClient.getPlayer().getPositionEyes().internal()));
            }
        });

        ClientEvents.TICK.subscribe(() -> net.minecraft.client.MinecraftClient.getInstance().worldRenderer.updateNoCullingBlockEntities(grhList, grhList));

        ClientEvents.RENDER_DEBUG.subscribe(right -> {
            // DebugHud
            int i;
            for (i = 0; i < right.size(); i++) {
                if (right.get(i).startsWith("Display: ")) {
                    i++;
                    break;
                }
            }
            right.add(i, GPUInfo.debug());
        });
    }

    /** Register a function that is called (with partial ticks) during the Block Entity render phase */
    public static void registerRender(Consumer<Float> func) {
        renderFuncs.add(func);
    }

    /** Register a function that is called (with partial ticks) during the UI render phase */
    public static void registerOverlay(Consumer<Float> func) {
        // InGameHud
        ClientEvents.RENDER_OVERLAY.subscribe(func::accept);
    }

    /** Register a function that is called to render during the mouse over phase (only if a block is moused over) */
    public static void registerItemMouseover(CustomItem item, MouseoverEvent fn) {
        ClientEvents.RENDER_MOUSEOVER.subscribe(partialTicks -> {
            if (MinecraftClient.getBlockMouseOver() != null) {
                Player player = MinecraftClient.getPlayer();
                if (item.internal == player.getHeldItem(Player.Hand.PRIMARY).internal.getItem()) {
                    fn.render(player, player.getHeldItem(Player.Hand.PRIMARY), MinecraftClient.getBlockMouseOver().down(), MinecraftClient.getPosMouseOver(), partialTicks);
                }
            }
        });
    }

    /** Get global position of the player's eyes (with partialTicks taken into account) */
    public static Vec3d getCameraPos(float partialTicks) {
        net.minecraft.entity.Entity playerrRender = net.minecraft.client.MinecraftClient.getInstance().cameraEntity;
        return new Vec3d(playerrRender.getCameraPosVec(partialTicks));
    }

    /** Internal camera helper */
    static Camera getCamera(float partialTicks) {
        return new Camera() {
            {
                setPos(getCameraPos(partialTicks).internal());
            }
        };
    }

    /** Return the render distance in meters */
    public static int getRenderDistance() {
        return net.minecraft.client.MinecraftClient.getInstance().options.viewDistance * 16;
    }

    @FunctionalInterface
    public interface MouseoverEvent {
        void render(Player player, ItemStack stack, Vec3i pos, Vec3d offset, float partialTicks);
    }

    public static class GlobalRenderHelper extends net.minecraft.block.entity.BlockEntity {
        public GlobalRenderHelper() {
            super(new BlockEntityType<GlobalRenderHelper>(GlobalRenderHelper::new, new HashSet<>(), null) {
                @Override
                public boolean supports(Block block_1) {
                    return true;
                }
            });
        }

        @Override
        public boolean hasWorld() {
            return true;
        }

        public BlockState getCachedState() {
            return Blocks.AIR.getDefaultState();
        }

        @Environment(EnvType.CLIENT)
        public double getSquaredRenderDistance() {
            return Double.POSITIVE_INFINITY;
        }

        @Environment(EnvType.CLIENT)
        public double getSquaredDistance(double double_1, double double_2, double double_3) {
            return 1;
        }

    }
}
