package cam72cam.mod.render;

import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.ModdedEntity;
import cam72cam.mod.entity.SeatEntity;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.render.OpenGL.With;
import cpw.mods.fml.client.registry.RenderingRegistry;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

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

    @Override
    public void doRender(net.minecraft.entity.Entity stockuncast, double x, double y, double z, float entityYaw, float partialTicks) {
        ModdedEntity stock = (ModdedEntity) stockuncast;
        Entity self = stock.getSelf();

        try (With c = OpenGL.matrix()) {
                GL11.glTranslated(x, y, z);
                GL11.glRotatef(180 - entityYaw, 0, 1, 0);
                GL11.glRotatef(self.getRotationPitch(), 1, 0, 0);
                GL11.glRotatef(-90, 0, 1, 0);
                renderers.get(self.getClass()).render(self, partialTicks);
        }
    }

    @Override
    protected ResourceLocation getEntityTexture(net.minecraft.entity.Entity p_110775_1_) {
        return null;
    }
}
