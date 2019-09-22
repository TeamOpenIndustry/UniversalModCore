package cam72cam.mod.render;

import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.ModdedEntity;
import cam72cam.mod.math.Vec3d;
import cpw.mods.fml.client.registry.RenderingRegistry;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;

public class EntityRenderer extends Render {
    private static Map<Class<? extends Entity>, IEntityRender> renderers = new HashMap<>();

    static {
        GlobalRender.registerRender(EntityRenderer::renderLargeEntities);
    }

    public static void registerEntities() {
        RenderingRegistry.registerEntityRenderingHandler(ModdedEntity.class, new EntityRenderer());
    }

    public static void register(Class<? extends Entity> type, IEntityRender render) {
        renderers.put(type, render);
    }

    private static void renderLargeEntities(float partialTicks) {
        if (GlobalRender.isTransparentPass()) {
            return;
        }

        /* TODO 1.7.10
        Minecraft.getMinecraft().mcProfiler.startSection("large_entity_helper");

        ICamera camera = GlobalRender.getCamera(partialTicks);

        World world = MinecraftClient.getPlayer().getWorld();
        List<Entity> entities = world.getEntities(Entity.class);
        for (Entity entity : entities) {
            // Duplicate forge logic and render entity if the chunk is not rendered but entity is visible (MC entitysize issues/optimization)
            AxisAlignedBB chunk = IBoundingBox.(entity.getBlockPosition().toChunkMin().internal, entity.getBlockPosition().toChunkMax().internal);
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

        GL11.glPushMatrix();
        {
            GL11.glTranslated(x, y, z);
            GL11.glRotatef(180 - entityYaw, 0, 1, 0);
            GL11.glRotatef(self.getRotationPitch(), 1, 0, 0);
            GL11.glRotatef(-90, 0, 1, 0);
            renderers.get(self.getClass()).render(self, partialTicks);

            for (ModdedEntity.StaticPassenger pass : stock.getStaticPassengers()) {
                if (pass.cache == null) {
                    pass.cache = pass.reconstitute(stock.worldObj);
                }
                Vec3d pos = stock.getRidingOffset(pass.uuid);
                if (pos == null) {
                    continue;
                }

                //TileEntityMobSpawnerRenderer
                EntityLiving ent = (EntityLiving) pass.cache;
                GL11.glPushMatrix();
                {
                    GL11.glTranslated(pos.x, pos.y - 0.5 + 0.35, pos.z);
                    GL11.glRotated(pass.rotation, 0, 1, 0);
                    RenderManager.instance.renderEntityWithPosYaw(ent, 0, 0, 0, 0, 0);
                }
                GL11.glPopMatrix();
            }

        }
        GL11.glPopMatrix();

    }

    @Override
    protected ResourceLocation getEntityTexture(net.minecraft.entity.Entity p_110775_1_) {
        return null;
    }
}
