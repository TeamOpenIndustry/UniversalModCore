package cam72cam.mod.render;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Player;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.item.ItemBase;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.CollectionUtil;
import cam72cam.mod.util.Hand;
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
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;

public class GlobalRender {
    private static List<Consumer<Float>> renderFuncs = new ArrayList<>();

    public static void registerClientEvents() {
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

        List<BlockEntity> grhList = CollectionUtil.listOf(new GlobalRenderHelper(null));
        ClientEvents.TICK.subscribe(() -> net.minecraft.client.MinecraftClient.getInstance().worldRenderer.updateBlockEntities(grhList, grhList));

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

    public static void registerRender(Consumer<Float> func) {
        renderFuncs.add(func);
    }

    public static void registerOverlay(Consumer<Float> func) {
        // InGameHud
        ClientEvents.RENDER_OVERLAY.subscribe(func::accept);
    }

    public static void registerItemMouseover(ItemBase item, MouseoverEvent fn) {
        ClientEvents.RENDER_MOUSEOVER.subscribe(partialTicks -> {
            if (MinecraftClient.getBlockMouseOver() != null) {
                Player player = MinecraftClient.getPlayer();
                if (item.internal == player.getHeldItem(Hand.PRIMARY).internal.getItem()) {
                    fn.render(player, player.getHeldItem(Hand.PRIMARY), MinecraftClient.getBlockMouseOver().down(), MinecraftClient.getPosMouseOver(), partialTicks);
                }
            }
        });
    }

    public static Vec3d getCameraPos(float partialTicks) {
        net.minecraft.entity.Entity playerrRender = net.minecraft.client.MinecraftClient.getInstance().cameraEntity;
        return new Vec3d(playerrRender.getCameraPosVec(partialTicks));
    }

    static Camera getCamera(float partialTicks) {
        return new Camera() {
            {
                setPos(getCameraPos(partialTicks).internal);
            }
        };
    }

    public static boolean isInRenderDistance(Vec3d pos) {
        // max rail length is 100, 50 is center
        return net.minecraft.client.MinecraftClient.getInstance().player.getPos().distanceTo(pos.internal) < ((net.minecraft.client.MinecraftClient.getInstance().options.viewDistance + 1) * 16 + 50);
    }

    public static void mulMatrix(FloatBuffer fbm) {
        GL11.glMultMatrixf(fbm);
    }

    @FunctionalInterface
    public interface MouseoverEvent {
        void render(Player player, ItemStack stack, Vec3i pos, Vec3d offset, float partialTicks);
    }

    public static class GlobalRenderHelper extends net.minecraft.block.entity.BlockEntity {
        public GlobalRenderHelper(BlockEntityType<?> blockEntityType_1) {
            super(blockEntityType_1);
        }

        @Override
        public boolean hasWorld() {
            return true;
        }

        public BlockEntityType<?> getType() {
            return new BlockEntityType<GlobalRenderHelper>(() -> new GlobalRenderHelper(null), new HashSet<>(), null) {
                @Override
                public boolean supports(Block block_1) {
                    return true;
                }
            };
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
