package cam72cam.mod.render;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.EntityRegistry;
import cam72cam.mod.entity.ModdedEntity;
import cam72cam.mod.entity.SeatEntity;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.world.World;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityRenderer extends net.minecraft.client.render.entity.EntityRenderer<ModdedEntity> {
    private static Map<Class<? extends Entity>, IEntityRender> renderers = new HashMap<>();

    static {
        //GlobalRender.registerRender(EntityRenderer::renderLargeEntities);
    }

    public static void registerClientEvents() {
        ClientEvents.REGISTER_ENTITY.subscribe(() -> {
            EntityRegistry.getTypes().forEach(t -> {
                EntityRendererRegistry.INSTANCE.register(t, EntityRenderer::new);
            });
        });

        ClientEvents.REGISTER_ENTITY.subscribe(() -> EntityRendererRegistry.INSTANCE.register(SeatEntity.TYPE, (manager, ctx) -> new net.minecraft.client.render.entity.EntityRenderer<SeatEntity>(manager) {
            @Nullable
            @Override
            public Identifier getTexture(SeatEntity var1) {
                return null;
            }
        }));
    }

    public EntityRenderer(EntityRenderDispatcher entityRenderDispatcher, EntityRendererRegistry.Context context) {
        super(entityRenderDispatcher);
    }

    public static void register(Class<? extends Entity> type, IEntityRender render) {
        renderers.put(type, render);
    }

    /*  I think 1.15 fixed this!
    private static void renderLargeEntities(float partialTicks) {
        net.minecraft.client.MinecraftClient.getInstance().getProfiler().push("large_entity_helper");

        Camera camera = GlobalRender.getCamera(partialTicks);

        Frustum frustum_1 = GlMatrixFrustum.get();
        VisibleRegion visibleRegion_1 = new FrustumWithOrigin(frustum_1);
        double double_1 = camera.getPos().x;
        double double_2 = camera.getPos().y;
        double double_3 = camera.getPos().z;
        visibleRegion_1.setOrigin(double_1, double_2, double_3);

        World world = MinecraftClient.getPlayer().getWorld();
        List<Entity> entities = world.getEntities(Entity.class);
        for (Entity entity : entities) {
            if (!(entity.internal instanceof ModdedEntity)) {
                continue;
            }
            // Duplicate minecraft logic and render entity if the chunk is not rendered but entity is visible (MC entitysize issues/optimization)
            Box chunk = new Box(entity.getBlockPosition().toChunkMin().internal, entity.getBlockPosition().toChunkMax().internal);
            if (!visibleRegion_1.intersects(chunk) && visibleRegion_1.intersects(entity.internal.getVisibilityBoundingBox())) {
                net.minecraft.client.MinecraftClient.getInstance().getEntityRenderManager().render(entity.internal, partialTicks, true);
            }
        }

        net.minecraft.client.MinecraftClient.getInstance().getProfiler().pop();
    }
    */

    @Override
    public void render(ModdedEntity stock, float entityYaw, float partialTicks, MatrixStack matrixStack_1, VertexConsumerProvider vertexConsumerProvider_1, int int_1) {
        Entity self = stock.getSelf();

        GL11.glPushMatrix();
        {
            //TODO 1.15 lerp xyz
            RenderLayer.getCutout().startDrawing();
            RenderSystem.multMatrix(matrixStack_1.peek().getModel());
            GL11.glRotatef(180 - entityYaw, 0, 1, 0);
            GL11.glRotatef(self.getRotationPitch(), 1, 0, 0);
            GL11.glRotatef(-90, 0, 1, 0);
            renderers.get(self.getClass()).render(self, partialTicks);
            RenderLayer.getCutout().endDrawing();
        }
        GL11.glPopMatrix();

    }

    @Nullable
    @Override
    public Identifier getTexture(ModdedEntity var1) {
        return null;
    }
}
