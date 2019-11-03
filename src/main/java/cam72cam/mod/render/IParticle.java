package cam72cam.mod.render;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.world.World;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import org.apache.logging.log4j.util.TriConsumer;
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

    public static <P extends ParticleData, I extends IParticle> Consumer<P> register(Function<P, I> ctr, TriConsumer<List<I>, Consumer<I>, Float> renderer) {
        List<I> particles = new ArrayList<>();

        return data -> {
            I ip = ctr.apply(data);
            Particle p = new Particle(data.world.internal, data.pos.x, data.pos.y, data.pos.z, data.motion.x, data.motion.y, data.motion.z) {
                {
                    maxAge = data.lifespan;
                    velocityX = data.motion.x;
                    velocityY = data.motion.y;
                    velocityZ = data.motion.z;
                }

                @Override
                public ParticleTextureSheet getType() {
                    return ip.depthTestEnabled() ? ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT : ParticleTextureSheet.PARTICLE_SHEET_OPAQUE;
                }

                @Override
                public void buildGeometry(BufferBuilder buffer, Camera entityIn, float partialTicks, float rotationX, float rotationZ, float rotationYZ, float rotationXY, float rotationXZ) {
                    ip.ticks = age;
                    ip.pos = new Vec3d(x,y,z);
                    ip.renderPos = new Vec3d(x - cameraX, y - cameraY, z - cameraZ);
                    ip.renderPos = ip.renderPos.add(this.velocityX * partialTicks, this.velocityY * partialTicks, this.velocityZ * partialTicks);

                    if (renderer == null) {
                        GL11.glPushMatrix();
                        {
                            GL11.glTranslated(ip.renderPos.x, ip.renderPos.y, ip.renderPos.z);
                            ip.render(partialTicks);
                        }
                        GL11.glPopMatrix();
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

            MinecraftClient.getInstance().particleManager.addParticle(p);
        };
    }

    protected abstract boolean depthTestEnabled();

    protected abstract void render(float partialTicks);

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
