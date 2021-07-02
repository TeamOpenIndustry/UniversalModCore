package cam72cam.mod.render;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.world.World;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.IParticleRenderType;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.vector.Vector3d;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Registry and Abstraction for Particles
 *
 * Try not to allocate anything for each render frame...
 * */
public abstract class Particle {
    /** Current position of the particle */
    protected double posX;
    protected double posY;
    protected double posZ;

    /** Current alive ticks of the particle */
    protected long ticks;
    /** Used internally for multirendering */
    boolean canRender = true;
    /** Used internally for rendering */
    protected double renderX;
    protected double renderY;
    protected double renderZ;

    /** Simple registration */
    public static <P extends ParticleData> Consumer<P> register(Function<P, Particle> ctr) {
        return register(ctr, null);
    }

    /** Particle registration with multi-particle renderer (useful for efficient shaders / textures) */
    public static <P extends ParticleData, I extends Particle> Consumer<P> register(Function<P, I> ctr, MultiRenderer<I> renderer) {
        List<I> particles = new ArrayList<>();

        return data -> {
            I ip = ctr.apply(data);
            net.minecraft.client.particle.Particle p = new net.minecraft.client.particle.Particle((ClientWorld)data.world.internal, data.pos.x, data.pos.y, data.pos.z, data.motion.x, data.motion.y, data.motion.z) {
                {
                    lifetime = data.lifespan;
                    xd = data.motion.x;
                    yd = data.motion.y;
                    zd = data.motion.z;
                    ip.posX = x;
                    ip.posY = y;
                    ip.posZ = z;
                }

                @Override
                public IParticleRenderType getRenderType() {
                    return IParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
                }

                @Override
                public void tick() {
                    super.tick();
                    ip.posX = x;
                    ip.posY = y;
                    ip.posZ = z;
                }

                @Override
                public void render(IVertexBuilder buffer, ActiveRenderInfo renderInfo, float partialTicks) {
                    Vector3d vec3d = renderInfo.getPosition();
                    ip.ticks = age;
                    ip.renderX = x + x - xo + this.xd * partialTicks - vec3d.x;
                    ip.renderY = y + y - yo + this.yd * partialTicks - vec3d.y;
                    ip.renderZ = z + z - zo + this.zd * partialTicks - vec3d.z;

                    if (renderer == null) {
                        try (OpenGL.With c = OpenGL.matrix()) {
                            GL11.glTranslated(ip.renderX, ip.renderY, ip.renderZ);
                            ip.render(partialTicks);
                        }
                    } else {
                        if (!ip.canRender) {
                            renderer.accept(particles, partialTicks);
                            particles.forEach(p -> p.canRender = true);
                            particles.clear();
                        }
                        particles.add(ip);
                        ip.canRender = false;
                    }
                }

            };

            Minecraft.getInstance().particleEngine.add(p);
        };
    }

    /** Should depth test be applied to this particle? */
    protected abstract boolean depthTestEnabled();

    /** Render this particle */
    protected abstract void render(float partialTicks);

    protected void lookAtPlayer() {
        Vec3d eyes = MinecraftClient.getPlayer().getPositionEyes();
        double x = eyes.x - posX;
        double y = eyes.y - posY;
        double z = eyes.z - posZ;
        GL11.glRotated(180 - Math.toDegrees(Math.atan2(-x, z)), 0, 1, 0);
        GL11.glRotated(180 - Math.toDegrees(Math.atan2(Math.sqrt(z * z + x * x), y)) + 90, 1, 0, 0);
    }

    /** Used to render multiple particles in the same function for efficiency */
    @FunctionalInterface
    public interface MultiRenderer<I extends Particle> {
        void accept(List<I> l, float pt);
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
