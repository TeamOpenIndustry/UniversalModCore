package cam72cam.mod.render;

import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.EntityRegistry;
import cam72cam.mod.entity.ModdedEntity;
import cam72cam.mod.entity.SeatEntity;
import cam72cam.mod.event.ClientEvents;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class EntityRenderer<T extends ModdedEntity> extends net.minecraft.client.renderer.entity.EntityRenderer<T> {
    private static Map<Class<? extends Entity>, IEntityRender> renderers = new HashMap<>();

    /*
    static {
        GlobalRender.registerRender(EntityRenderer::renderLargeEntities);
    }*/

    public static void registerClientEvents() {
        ClientEvents.REGISTER_ENTITY.subscribe(() -> {
            renderers.forEach((cls, renderer) -> {
                RenderingRegistry.registerEntityRenderingHandler(EntityRegistry.type(cls), EntityRenderer::new);
            });
        });

        ClientEvents.REGISTER_ENTITY.subscribe(() -> RenderingRegistry.registerEntityRenderingHandler(SeatEntity.TYPE, manager -> new net.minecraft.client.renderer.entity.EntityRenderer<SeatEntity>(manager) {
            @Nullable
            @Override
            public ResourceLocation getEntityTexture(SeatEntity entity) {
                return null;
            }
        }));
    }

    public EntityRenderer(EntityRendererManager factory) {
        super(factory);
    }

    public static void register(Class<? extends Entity> type, IEntityRender render) {
        renderers.put(type, render);
    }

    /* Fixed in 1.15?
    private static void renderLargeEntities(float partialTicks) {
        if (GlobalRender.isTransparentPass()) {
            return;
        }

        Minecraft.getInstance().getProfiler().startSection("large_entity_helper");

        ActiveRenderInfo camera = GlobalRender.getCamera(partialTicks);

        World world = MinecraftClient.getPlayer().getWorld();
        List<Entity> entities = world.getEntities(Entity.class);
        for (Entity entity : entities) {
            if (!(entity.internal instanceof ModdedEntity)) {
                continue;
            }

            // Duplicate forge logic and render entity if the chunk is not rendered but entity is visible (MC entitysize issues/optimization)
            double yoff = Math.floor(entity.getPosition().y / 16f);
            Vec3d min = entity.getBlockPosition().toChunkMin();
            min = new Vec3d(min.x, yoff, min.z);
            Vec3d max = entity.getBlockPosition().toChunkMax();
            max = new Vec3d(max.x, yoff + 16, max.z);
            AxisAlignedBB chunk = new AxisAlignedBB(min.internal, max.internal);
            if (!camera.isBoundingBoxInFrustum(chunk) && camera.isBoundingBoxInFrustum(entity.internal.getRenderBoundingBox())) {
                Minecraft.getInstance().getRenderManager().renderEntityStatic(entity.internal, partialTicks, true);
            }
        }

        Minecraft.getInstance().getProfiler().endSection();
    } */

    @Override
    public void render(T stock, float entityYaw, float partialTicks, MatrixStack p_225623_4_, IRenderTypeBuffer p_225623_5_, int p_225623_6_) {
        Entity self = stock.getSelf();

        RenderType.getSolid().setupRenderState();


        GL11.glPushMatrix();
        {
            //TODO 1.15 lerp xyz
            RenderSystem.multMatrix(p_225623_4_.getLast().getMatrix());
            GL11.glRotatef(180 - entityYaw, 0, 1, 0);
            GL11.glRotatef(self.getRotationPitch(), 1, 0, 0);
            GL11.glRotatef(-90, 0, 1, 0);
            renderers.get(self.getClass()).render(self, partialTicks);
        }
        GL11.glPopMatrix();

        RenderType.getSolid().clearRenderState();

    }

    @Nullable
    @Override
    public ResourceLocation getEntityTexture(T entity) {
        return null;
    }
}
