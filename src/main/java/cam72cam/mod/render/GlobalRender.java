package cam72cam.mod.render;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Player;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.item.CustomItem;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/** Global Render Registry and helper functions */
public class GlobalRender {
    // Fire these off every tick
    private static List<Consumer<Float>> renderFuncs = new ArrayList<>();

    // Internal hack
    private static List<BlockEntity> grhList = Collections.singletonList(new GlobalRenderHelper(null, null));

    /** Internal, hooked into event system directly */
    public static void registerClientEvents() {
        // Beacon like hack for always running a single global render during the TE render phase
        ClientEvents.REGISTER_ENTITY.subscribe(() -> {
            try {
                BlockEntityRenderers.register(grhtype, (ted) -> new BlockEntityRenderer<>() {
                    @Override
                    public int getViewDistance() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public void render(GlobalRenderHelper te, float partialTicks, PoseStack matrixStack, MultiBufferSource iRenderTypeBuffer, int i, int i1) {
                        OpenGL.internalMultMatrix(matrixStack.last().pose());
                        // TODO 1.15+ do we need to set lightmap coords here?
                        renderFuncs.forEach(r -> r.accept(partialTicks));
                    }

                    @Override
                    public boolean shouldRenderOffScreen(GlobalRenderHelper te) {
                        return true;
                    }
                });
            } catch (ExceptionInInitializerError ex) {
                // data generator pass
                System.out.println("Shake hands with danger");
            }
        });
        ClientEvents.TICK.subscribe(() -> {
            Minecraft.getInstance().levelRenderer.updateGlobalBlockEntities(grhList, grhList);
            /*
            if (Minecraft.getInstance().player != null) {  // May be able to get away with running this every N ticks?
                grhList.get(0).setPosition(new BlockPos(Minecraft.getInstance().player.getEyePosition(0)));
            }*/
        });

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
    public static void registerRender(Consumer<Float> func) {
        renderFuncs.add(func);
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
                    fn.render(player, player.getHeldItem(Player.Hand.PRIMARY), MinecraftClient.getBlockMouseOver().down(), MinecraftClient.getPosMouseOver(), partialTicks);
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

    @FunctionalInterface
    public interface MouseoverEvent {
        void render(Player player, ItemStack stack, Vec3i pos, Vec3d offset, float partialTicks);
    }

    static BlockEntityType<GlobalRenderHelper> grhtype = new BlockEntityType<GlobalRenderHelper>(GlobalRenderHelper::new, new HashSet<>(), null) {
        @Override
        public boolean isValid(BlockState block_1) {
            return true;
        }
    };

    public static class GlobalRenderHelper extends BlockEntity {

        public GlobalRenderHelper(BlockPos pos, BlockState state) {
            super(grhtype, new BlockPos(0, 0, 0) {
                @Override
                public BlockPos immutable() {
                    // This is why I love java
                    return new BlockPos(Minecraft.getInstance().player.getEyePosition(0));
                }
            }, state);
        }

        @Override
        public boolean hasLevel() {
            return true;
        }

        @Nullable
        @Override
        public Level getLevel() {
            return Minecraft.getInstance().level;
        }



        public net.minecraft.world.phys.AABB getRenderBoundingBox() {
            return INFINITE_EXTENT_AABB;
        }

        /* Moved to renderer
        @Override
        public double getViewDistance() {
            return Double.POSITIVE_INFINITY;
        }*/

        public double getDistanceSq(double x, double y, double z) {
            return 1;
        }

        public BlockState getBlockState() {
            return Blocks.AIR.defaultBlockState();
        }
    }
}
