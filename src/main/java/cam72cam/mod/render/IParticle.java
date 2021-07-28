package cam72cam.mod.render;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.util.TriConsumer;
import cam72cam.mod.world.World;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.Tessellator;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class IParticle {
    protected Vec3d pos;
    protected long ticks;
    boolean canRender = true;

    Vec3d renderPos;

    public static <P extends ParticleData> Consumer<P> register(Function<P, IParticle> ctr) {
        return register(ctr, null);
    }

    public static <P extends ParticleData, I extends IParticle> Consumer<P> register(Function<P, I> ctr, MultiRenderer<I> renderer) {
        List<I> particles = new ArrayList<>();

        return data -> {
            I ip = ctr.apply(data);
            EntityFX p = new EntityFX(data.world.internal, data.pos.x, data.pos.y, data.pos.z, data.motion.x, data.motion.y, data.motion.z) {
                {
                    particleMaxAge = data.lifespan;
                    motionX = data.motion.x;
                    motionY = data.motion.y;
                    motionZ = data.motion.z;
                    this.noClip = true;
                }

                /* TODO 1.7.10
                @Override
                public boolean isTransparent() {
                    return !ip.depthTestEnabled();
                }
                */

                @Override
                public void renderParticle(Tessellator buffer, float partialTicks, float rotationX, float rotationZ, float rotationYZ, float rotationXY, float rotationXZ) {
                    ip.ticks = particleAge;
                    ip.pos = new Vec3d(posX, posY, posZ);
                    ip.renderPos = new Vec3d(posX - interpPosX, posY - interpPosY, posZ - interpPosZ);
                    ip.renderPos = ip.renderPos.add(this.motionX * partialTicks, this.motionY * partialTicks, this.motionZ * partialTicks);

                    if (renderer == null) {
                        try (OpenGL.With c = OpenGL.matrix()) {
                            GL11.glTranslated(ip.renderPos.x, ip.renderPos.y, ip.renderPos.z);
                            ip.render(partialTicks);
                        }
                    } else {
                        if (!ip.canRender) {
                            renderer.accept(particles, subp -> GL11.glTranslated(subp.renderPos.x, subp.renderPos.y, subp.renderPos.z), partialTicks);
                            particles.forEach(p -> p.canRender = true);
                            particles.clear();
                        }
                        particles.add(ip);
                        ip.canRender = false;
                    }
                }
            };

            Minecraft.getMinecraft().effectRenderer.addEffect(p);
        };
    }

    protected abstract boolean depthTestEnabled();

    protected abstract void render(float partialTicks);

    @FunctionalInterface
    public interface MultiRenderer<I extends IParticle> {
        void accept(List<I> l, Consumer<I> c, float pt);
    }

    public static class ParticleData {
        public final World world;
        public final Vec3d pos;
        public final Vec3d motion;
        public final int lifespan;

        public ParticleData(World world, Vec3d pos, Vec3d motion, int lifespan) {
            this.world = world;
            this.pos = pos;
            this.motion = motion;
            this.lifespan = lifespan;
        }
    }
}
