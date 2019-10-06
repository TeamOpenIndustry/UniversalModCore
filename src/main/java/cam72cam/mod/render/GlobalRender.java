package cam72cam.mod.render;

import cam72cam.mod.entity.Player;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.item.ItemBase;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Hand;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.render.BlockEntityRendererRegistry;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GlobalRender {
    private static List<Consumer<Float>> renderFuncs = new ArrayList<>();

    public static void registerClientEvents() {
        ClientEvents.REGISTER_ENTITY.subscribe(() -> {
            BlockEntityRendererRegistry.INSTANCE.register(GlobalRenderHelper.class, new BlockEntityRenderer<GlobalRenderHelper>() {
                @Override
                public void render(GlobalRenderHelper te, double x, double y, double z, float partialTicks, int destroyStage) {
                    renderFuncs.forEach(r -> r.accept(partialTicks));
                }

                @Override
                public boolean method_3563(GlobalRenderHelper te) {
                    return true;
                }
            });
        });

        BlockEntity grh = new GlobalRenderHelper(null);
        List<BlockEntity> grhList = new ArrayList<>();
        grhList.add(grh);
        ClientEvents.TICK.subscribe(() -> MinecraftClient.getInstance().worldRenderer.updateBlockEntities(grhList, grhList));

        ClientEvents.RENDER_DEBUG.subscribe(event -> {
            if (MinecraftClient.getInstance().options.debugEnabled && GPUInfo.hasGPUInfo()) {
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

    public static void registerRender(Consumer<Float> func) {
        renderFuncs.add(func);
    }

    public static void registerOverlay(Consumer<Float> func) {
        ClientEvents.RENDER_OVERLAY.subscribe(event -> {
            if (event.getType() == RenderGameOverlayEvent.ElementType.ALL) {
                func.accept(event.getPartialTicks());
            }
        });
    }

    public static void registerItemMouseover(ItemBase item, MouseoverEvent fn) {
        ClientEvents.RENDER_MOUSEOVER.subscribe(partialTicks -> {
            if (MinecraftClient.getBlockMouseOver() != null) {
                Player player = MinecraftClient.getPlayer();
                if (item.internal == player.getHeldItem(Hand.PRIMARY).internal.getItem()) {
                    fn.render(player, player.getHeldItem(Hand.PRIMARY), MinecraftClient.getBlockMouseOver(), MinecraftClient.getPosMouseOver(), partialTicks);
                }
            }
        });
    }

    public static Vec3d getCameraPos(float partialTicks) {
        net.minecraft.entity.Entity playerrRender = MinecraftClient.getInstance().cameraEntity;
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
        return MinecraftClient.getInstance().player.getPos().distanceTo(pos.internal) < ((MinecraftClient.getInstance().options.viewDistance + 1) * 16 + 50);
    }

    @FunctionalInterface
    public interface MouseoverEvent {
        void render(Player player, ItemStack stack, Vec3i pos, Vec3d offset, float partialTicks);
    }

    public static class GlobalRenderHelper extends net.minecraft.block.entity.BlockEntity {

        public GlobalRenderHelper(BlockEntityType<?> blockEntityType_1) {
            super(blockEntityType_1);
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
