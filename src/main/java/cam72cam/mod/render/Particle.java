package cam72cam.mod.render;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.world.World;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.Entity;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/** Registry and Abstraction for Particles */
public abstract class Particle {
    /** Current position of the particle */
    protected Vec3d pos;
    /** Current alive ticks of the particle */
    protected long ticks;
    /** Used internally for multirendering */
    boolean canRender = true;
    /** Used internally for rendering */
    Vec3d renderPos;

    /** Simple registration */
    public static <P extends ParticleData> Consumer<P> register(Function<P, Particle> ctr) {
        return register(ctr, null);
    }

    /** Particle registration with multi-particle renderer (useful for efficient shaders / textures) */
    public static <P extends ParticleData, I extends Particle> Consumer<P> register(Function<P, I> ctr, MultiRenderer<I> renderer) {
        List<I> particles = new ArrayList<>();

        return data -> {
            I ip = ctr.apply(data);
            net.minecraft.client.particle.Particle p = new net.minecraft.client.particle.Particle(data.world.internal, data.pos.x, data.pos.y, data.pos.z, data.motion.x, data.motion.y, data.motion.z) {
                {
                    particleMaxAge = data.lifespan;
                    motionX = data.motion.x;
                    motionY = data.motion.y;
                    motionZ = data.motion.z;
                }

                @Override
                public boolean shouldDisableDepth() {
                    return !ip.depthTestEnabled();
                }

                @Override
                public void renderParticle(BufferBuilder buffer, Entity entityIn, float partialTicks, float rotationX, float rotationZ, float rotationYZ, float rotationXY, float rotationXZ) {
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

    /** Should depth test be applied to this particle? */
    protected abstract boolean depthTestEnabled();

    /** Render this particle */
    protected abstract void render(float partialTicks);

    /** Used to render multiple particles in the same function for efficiency */
    @FunctionalInterface
    public interface MultiRenderer<I extends Particle> {
        void accept(List<I> l, Consumer<I> c, float pt);
    }

    /** Data to be stored for each particle (can be extended) */
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
