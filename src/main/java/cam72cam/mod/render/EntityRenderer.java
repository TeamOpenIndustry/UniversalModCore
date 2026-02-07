package cam72cam.mod.render;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.EntityRegistry;
import cam72cam.mod.entity.ModdedEntity;
import cam72cam.mod.entity.SeatEntity;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.opengl.RenderContext;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.world.World;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Entity Rendering Registry */
public class EntityRenderer<T extends ModdedEntity> extends net.minecraft.client.renderer.entity.EntityRenderer<T> {
    private static Map<Class<? extends Entity>, IEntityRender> renderers = new HashMap<>();

    static {
        /*
        ClientEvents.REGISTER_ENTITY.subscribe(() -> {
            ModCore.info("Attempting to detect optifine...");
            try {
                Class<?> opticlass = Class.forName("net.optifine.Config");
                ModCore.debug("Optifine class" + opticlass);

                ModCore.warn("===========================================================================");
                ModCore.warn("             DETECTED OPTIFINE, PATCHING CRAP THAT IT BROKE...             ");
                ModCore.warn("===========================================================================");

                OPTIFINE_SUCKS.subscribe(EntityRenderer::renderLargeEntities);
            } catch (ClassNotFoundException e) {
                ModCore.info("Optifine not detected, phew");
            }
        });*/

        ClientEvents.RENDER_LEVEL_POST.subscribe(EntityRenderer::renderLargeEntities);
    }

    public static void registerClientEvents() {
        // Hook in our entity renderer which will dispatch to the IEntityRenderers
        ClientEvents.REGISTER_ENTITY.subscribe(() -> {
            renderers.forEach((cls, renderer) -> {
                EntityRenderers.register(EntityRegistry.type(cls), EntityRenderer::new);
            });
        });

        // Don't render seat entities
        ClientEvents.REGISTER_ENTITY.subscribe(() -> EntityRenderers.register(SeatEntity.TYPE, manager -> new net.minecraft.client.renderer.entity.EntityRenderer<>(manager) {
            @Nullable
            @Override
            public ResourceLocation getTextureLocation(SeatEntity entity) {
                return null;
            }
        }));
    }

    /** Internal, do not use */
    public EntityRenderer(EntityRendererProvider.Context factory) {
        super(factory);
    }

    /** This is how you register your entities renderer */
    public static void register(Class<? extends Entity> type, IEntityRender render) {
        renderers.put(type, render);
    }

    /**
     * So this is a fun one...
     * <p>
     * Our <code>WorldEntityTracker</code> only work on server side which mean we can't sync entity bb data
     * to client... so enable the wrapper here to allow proper rendering for large entities when connected to dedicated server
     * @see cam72cam.mod.world.WorldEntityTracker
     */
    private static void renderLargeEntities(RenderWorldLastEvent event) {
        if (GlobalRender.isTransparentPass()) {
            return;
        }

        Minecraft.getInstance().getProfiler().push("large_entity_helper");

        float partialTicks = event.getPartialTicks();
        EntityRenderDispatcher renderManager = Minecraft.getInstance().getEntityRenderDispatcher();

        Camera info = GlobalRender.getCamera(event.getPartialTicks());
        Vec3 vec3d = info.getPosition();
        double camX = vec3d.x();
        double camY = vec3d.y();
        double camZ = vec3d.z();

        Frustum camera = new Frustum(event.getMatrixStack().last().pose(), event.getProjectionMatrix());
        camera.prepare(camX, camY, camZ);

        World world = MinecraftClient.getPlayer().getWorld();
        List<Entity> entities = world.getEntities(Entity.class);
        for (Entity entity : entities) {
            if (!(entity.internal instanceof ModdedEntity)) {
                continue;
            }

            // Duplicate forge logic and render entity if the chunk is not rendered but entity is visible (MC entitysize issues/optimization)
            double yoff = ((int)entity.getPosition().y) >> 4 << 4;
            Vec3d min = entity.getBlockPosition().toChunkMin();
            min = new Vec3d(min.x, yoff, min.z);
            Vec3d max = entity.getBlockPosition().toChunkMax();
            max = new Vec3d(max.x, yoff + 16, max.z);
            AABB chunk = new AABB(min.internal(), max.internal());
            if (!camera.isVisible(chunk) && camera.isVisible(entity.internal.getBoundingBoxForCulling())) {
                net.minecraft.world.entity.Entity entityIn = entity.internal;
                double d0 = Mth.lerp(partialTicks, entityIn.xo, entityIn.getX());
                double d1 = Mth.lerp(partialTicks, entityIn.yo, entityIn.getY());
                double d2 = Mth.lerp(partialTicks, entityIn.zo, entityIn.getZ());
                float f = Mth.lerp(partialTicks, entityIn.yRotO, entityIn.getYRot());
                renderManager.render(entityIn, d0 - camX, d1 - camY, d2 - camZ, f, partialTicks, event.getMatrixStack(), Minecraft.getInstance().renderBuffers().bufferSource(), renderManager.getPackedLightCoords(entityIn, partialTicks));
            }
        }

        Minecraft.getInstance().getProfiler().pop();
    }

    @Override
    public void render(T stock, float entityYaw, float partialTicks, PoseStack p_225623_4_, MultiBufferSource p_225623_5_, int i) {
        Entity self = stock.getSelf();

        RenderType.cutout().setupRenderState();

        int j = (i >> 4) & 0xF;
        int k = (i >> 20) & 0xF;
        RenderState state = new RenderState(p_225623_4_).lightmap(j / 15f, k / 15f);
        state.rotate(180 - entityYaw, 0, 1, 0);
        state.rotate(self.getRotationPitch(), 1, 0, 0);
        state.rotate(-90, 0, 1, 0);
        state.stage(RenderContext.Stage.ENTITY);
        //Set up our own light state
        RenderContext.lastLightX = j / 15f;
        RenderContext.lastLightY = k / 15f;

        // State may be modified in render, before calling in to post-render
        renderers.get(self.getClass()).render(self, state.clone(), partialTicks);
        // TODO
        renderers.get(self.getClass()).postRender(self, state, partialTicks);

        RenderType.cutout().clearRenderState();
    }

    @Nullable
    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return null;
    }
}
