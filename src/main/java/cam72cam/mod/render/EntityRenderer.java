package cam72cam.mod.render;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.ModCore;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.EntityRegistry;
import cam72cam.mod.entity.ModdedEntity;
import cam72cam.mod.entity.SeatEntity;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.world.World;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderType;
import cam72cam.mod.render.OpenGL.With;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cam72cam.mod.event.ClientEvents.OPTIFINE_SUCKS;

/** Entity Rendering Registry */
public class EntityRenderer<T extends ModdedEntity> extends net.minecraft.client.renderer.entity.EntityRenderer<T> {
    private static Map<Class<? extends Entity>, IEntityRender> renderers = new HashMap<>();

    static {
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
        });
    }

    public static void registerClientEvents() {
        // Hook in our entity renderer which will dispatch to the IEntityRenderers
        ClientEvents.REGISTER_ENTITY.subscribe(() -> {
            renderers.forEach((cls, renderer) -> {
                RenderingRegistry.registerEntityRenderingHandler(EntityRegistry.type(cls), EntityRenderer::new);
            });
        });

        // Don't render seat entities
        ClientEvents.REGISTER_ENTITY.subscribe(() -> RenderingRegistry.registerEntityRenderingHandler(SeatEntity.TYPE, manager -> new net.minecraft.client.renderer.entity.EntityRenderer<SeatEntity>(manager) {
            @Nullable
            @Override
            public ResourceLocation getTextureLocation(SeatEntity entity) {
                return null;
            }
        }));
    }

    /** Internal, do not use */
    public EntityRenderer(EntityRendererManager factory) {
        super(factory);
    }

    /** This is how you register your entities renderer */
    public static void register(Class<? extends Entity> type, IEntityRender render) {
        renderers.put(type, render);
    }

    /** Fixed in 1.15? Minecraft yes! Optifine Broke it!
     * Sooo this is a fun one...
     *
     * MC culls out entities in chunks that are not in view, which breaks when entities span chunk boundaries
     * For 1-2 block entities, this is barely noticeable.  For large entities it's a problem.
     * We try to detect entities in this edge case and render them here to prevent the issue.
     *
     */
    private static void renderLargeEntities(RenderWorldLastEvent event) {
        if (GlobalRender.isTransparentPass()) {
            return;
        }

        Minecraft.getInstance().getProfiler().push("large_entity_helper");

        float partialTicks = event.getPartialTicks();
        EntityRendererManager renderManager = Minecraft.getInstance().getEntityRenderDispatcher();

        ActiveRenderInfo info = GlobalRender.getCamera(event.getPartialTicks());
        Vector3d vec3d = info.getPosition();
        double camX = vec3d.x();
        double camY = vec3d.y();
        double camZ = vec3d.z();

        ClippingHelper camera = new ClippingHelper(event.getMatrixStack().last().pose(), event.getProjectionMatrix());
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
            AxisAlignedBB chunk = new AxisAlignedBB(min.internal(), max.internal());
            if (!camera.isVisible(chunk) && camera.isVisible(entity.internal.getBoundingBoxForCulling())) {
                net.minecraft.entity.Entity entityIn = entity.internal;
                double d0 = MathHelper.lerp(partialTicks, entityIn.xo, entityIn.getX());
                double d1 = MathHelper.lerp(partialTicks, entityIn.yo, entityIn.getY());
                double d2 = MathHelper.lerp(partialTicks, entityIn.zo, entityIn.getZ());
                float f = MathHelper.lerp(partialTicks, entityIn.yRotO, entityIn.yRot);
                renderManager.render(entityIn, d0 - camX, d1 - camY, d2 - camZ, f, partialTicks, event.getMatrixStack(), Minecraft.getInstance().renderBuffers().bufferSource(), renderManager.getPackedLightCoords(entityIn, partialTicks));
            }
        }

        Minecraft.getInstance().getProfiler().pop();
    }

    @Override
    public void render(T stock, float entityYaw, float partialTicks, MatrixStack p_225623_4_, IRenderTypeBuffer p_225623_5_, int i) {
        Entity self = stock.getSelf();

        RenderType.cutout().setupRenderState();

        RenderHelper.turnBackOn();

        Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();

        int j = i % 65536;
        int k = i / 65536;
        GL13.glMultiTexCoord2f(33986, (float)j, (float)k);

        try (With c = OpenGL.matrix()) {
            //TODO 1.15 lerp xyz
            RenderSystem.multMatrix(p_225623_4_.last().pose());
            GL11.glRotatef(180 - entityYaw, 0, 1, 0);
            GL11.glRotatef(self.getRotationPitch(), 1, 0, 0);
            GL11.glRotatef(-90, 0, 1, 0);
            renderers.get(self.getClass()).render(self, partialTicks);
        }

        RenderType.cutout().clearRenderState();
    }

    @Nullable
    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return null;
    }
}
