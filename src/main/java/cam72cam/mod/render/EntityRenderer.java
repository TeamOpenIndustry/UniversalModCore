package cam72cam.mod.render;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.ModdedEntity;
import cam72cam.mod.entity.SeatEntity;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.OpenGL.With;
import cam72cam.mod.world.World;
import net.fabricmc.fabric.api.client.render.EntityRendererRegistry;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Entity Rendering Registry */
public class EntityRenderer extends net.minecraft.client.render.entity.EntityRenderer<ModdedEntity> {
    private static Map<Class<? extends Entity>, IEntityRender> renderers = new HashMap<>();

    static {
        GlobalRender.registerRender(EntityRenderer::renderLargeEntities);
    }

    public static void registerClientEvents() {
        // Hook in our entity renderer which will dispatch to the IEntityRenderers
        ClientEvents.REGISTER_ENTITY.subscribe(() -> EntityRendererRegistry.INSTANCE.register(ModdedEntity.class, EntityRenderer::new));

        // Don't render seat entities
        ClientEvents.REGISTER_ENTITY.subscribe(() -> EntityRendererRegistry.INSTANCE.register(SeatEntity.class, (manager, ctx) -> new net.minecraft.client.render.entity.EntityRenderer<SeatEntity>(manager) {
            @Nullable
            @Override
            protected Identifier getTexture(SeatEntity var1) {
                return null;
            }
        }));
    }

    /** Internal, do not use */
    public EntityRenderer(EntityRenderDispatcher entityRenderDispatcher, EntityRendererRegistry.Context context) {
        super(entityRenderDispatcher);
    }

    /** This is how you register your entities renderer */
    public static void register(Class<? extends Entity> type, IEntityRender render) {
        renderers.put(type, render);
    }

    /**
     * Sooo this is a fun one...
     *
     * MC culls out entities in chunks that are not in view, which breaks when entities span chunk boundaries
     * For 1-2 block entities, this is barely noticeable.  For large entities it's a problem.
     * We try to detect entities in this edge case and render them here to prevent the issue.
     */
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
            double yoff = ((int)entity.getPosition().y) >> 4 << 4;
            Vec3d min = entity.getBlockPosition().toChunkMin();
            min = new Vec3d(min.x, yoff, min.z);
            Vec3d max = entity.getBlockPosition().toChunkMax();
            max = new Vec3d(max.x, yoff + 16, max.z);
            Box chunk = new Box(min.internal(), max.internal());
            if (!visibleRegion_1.intersects(chunk) && visibleRegion_1.intersects(entity.internal.getVisibilityBoundingBox())) {
                net.minecraft.client.MinecraftClient.getInstance().getEntityRenderManager().render(entity.internal, partialTicks, true);
            }
        }

        net.minecraft.client.MinecraftClient.getInstance().getProfiler().pop();
    }

    @Override
    public void render(ModdedEntity stock, double x, double y, double z, float entityYaw, float partialTicks) {
        Entity self = stock.getSelf();

        try (With c = OpenGL.matrix()) {
                GL11.glTranslated(x, y, z);
                GL11.glRotatef(180 - entityYaw, 0, 1, 0);
                GL11.glRotatef(self.getRotationPitch(), 1, 0, 0);
                GL11.glRotatef(-90, 0, 1, 0);
                renderers.get(self.getClass()).render(self, partialTicks);
        }
    }

    @Nullable
    @Override
    protected Identifier getTexture(ModdedEntity var1) {
        return null;
    }
}
