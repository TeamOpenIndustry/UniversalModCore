package cam72cam.mod.render;

import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.ModdedEntity;
import cam72cam.mod.entity.SeatEntity;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.render.opengl.RenderState;
import cpw.mods.fml.client.registry.RenderingRegistry;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.MinecraftForgeClient;

import java.util.HashMap;
import java.util.Map;

/** Entity Rendering Registry */
public class EntityRenderer extends Render {
    private static Map<Class<? extends Entity>, IEntityRender> renderers = new HashMap<>();

    public static void registerClientEvents() {
        // Hook in our entity renderer which will dispatch to the IEntityRenderers
        ClientEvents.REGISTER_ENTITY.subscribe(() -> RenderingRegistry.registerEntityRenderingHandler(ModdedEntity.class, new EntityRenderer()));

        // Don't render seat entities
        ClientEvents.REGISTER_ENTITY.subscribe(() -> RenderingRegistry.registerEntityRenderingHandler(SeatEntity.class, new Render() {
            @Override
            public void doRender(net.minecraft.entity.Entity p_76986_1_, double p_76986_2_, double p_76986_4_, double p_76986_6_, float p_76986_8_, float p_76986_9_) {

            }
            @Override
            protected ResourceLocation getEntityTexture(net.minecraft.entity.Entity p_110775_1_) {
                return null;
            }
        }));
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
    private static void renderLargeEntities(RenderState state, float partialTicks) {
        /*
        if (GlobalRender.isTransparentPass()) {
            return;
        }

        Minecraft.getMinecraft().mcProfiler.startSection("large_entity_helper");

        ICamera camera = GlobalRender.getCamera(partialTicks);

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
            if (!camera.isBoundingBoxInFrustum(chunk) && camera.isBoundingBoxInFrustum(entity.internal.getRenderBoundingBox())) {
                Minecraft.getMinecraft().getRenderManager().renderEntityStatic(entity.internal, partialTicks, true);
            }
        }

        Minecraft.getMinecraft().mcProfiler.endSection();

         */
    }

    @Override
    public void doRender(net.minecraft.entity.Entity stockuncast, double x, double y, double z, float entityYaw, float partialTicks) {
        ModdedEntity stock = (ModdedEntity) stockuncast;
        Entity self = stock.getSelf();

        RenderState state = new RenderState();
        state.translate(x, y, z);
        state.rotate(180 - entityYaw, 0, 1, 0);
        state.rotate(self.getRotationPitch(), 1, 0, 0);
        state.rotate(-90, 0, 1, 0);
        if (MinecraftForgeClient.getRenderPass() == 0) {
            renderers.get(self.getClass()).render(self, state, partialTicks);
        } else {
            renderers.get(self.getClass()).postRender(self, state, partialTicks);
        }
    }

    @Override
    protected ResourceLocation getEntityTexture(net.minecraft.entity.Entity p_110775_1_) {
        return null;
    }
}
