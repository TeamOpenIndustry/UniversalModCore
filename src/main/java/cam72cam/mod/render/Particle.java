package cam72cam.mod.render;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.opengl.BlendMode;
import cam72cam.mod.render.opengl.RenderContext;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.world.World;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.core.particles.*;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import util.Matrix4;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Registry and Abstraction for Particles
 * <p>
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
            net.minecraft.client.particle.Particle p = new net.minecraft.client.particle.Particle((ClientLevel) data.world.internal, data.pos.x, data.pos.y, data.pos.z, data.motion.x, data.motion.y, data.motion.z) {
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
                public ParticleRenderType getRenderType() {
                    return ParticleRenderType.CUSTOM;
                }

                @Override
                public void tick() {
                    super.tick();
                    ip.posX = x;
                    ip.posY = y;
                    ip.posZ = z;
                }

                @Override
                public void render(VertexConsumer buffer, Camera renderInfo, float partialTicks) {
                    RenderState base = new RenderState();
                    base.blend(new BlendMode(BlendMode.GL_SRC_ALPHA, BlendMode.GL_ONE_MINUS_SRC_ALPHA));
                    base.alpha_test(true);
                    base.depth_mask(ip.depthTestEnabled());
                    base.color(1, 1, 1, 1);
                    base.stage(RenderContext.Stage.ENTITY);
                    Minecraft.getInstance().gameRenderer.lightTexture().turnOffLightLayer();

                    Vec3 vec3d = renderInfo.getPosition();
                    ip.ticks = age;
                    ip.renderX = x + x - xo + this.xd * partialTicks - vec3d.x;
                    ip.renderY = y + y - yo + this.yd * partialTicks - vec3d.y;
                    ip.renderZ = z + z - zo + this.zd * partialTicks - vec3d.z;

                    if (renderer == null) {
                        RenderState state = base.clone();
                        state.stage(RenderContext.Stage.ENTITY);
                        state.translate(ip.renderX, ip.renderY, ip.renderZ);
                        ip.render(state, partialTicks);
                    } else {
                        if (!ip.canRender) {
                            renderer.accept(particles, base.clone(), partialTicks);
                            particles.forEach(p -> p.canRender = true);
                            particles.clear();
                        }
                        particles.add(ip);
                        ip.canRender = false;
                    }


                    Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();
                }

            };

            Minecraft.getInstance().particleEngine.add(p);
        };
    }

    /** Should depth test be applied to this particle? */
    protected abstract boolean depthTestEnabled();

    /** Render this particle */
    protected abstract void render(RenderState state, float partialTicks);

    protected void lookAtPlayer(Matrix4 mat) {
        Vec3d eyes = MinecraftClient.getPlayer().getPositionEyes();
        double x = eyes.x - posX;
        double y = eyes.y - posY;
        double z = eyes.z - posZ;
        mat.rotate(Math.toRadians(180 - Math.toDegrees(Mth.atan2(-x, z))), 0, 1, 0);
        mat.rotate(Math.toRadians(180 - Math.toDegrees(Mth.atan2(Math.sqrt(z * z + x * x), y))) + 90, 1, 0, 0);
        mat.rotate(Math.toRadians(180), 1, 0, 0);
    }

    public static void renderVanilla(VanillaParticles vanilla, Vec3d pos, Vec3d velocity, float scale) {
        if (scale < 1E-7) return;

        ParticleOptions options = switch (vanilla) {
            case SAND_DUST -> new BlockParticleOption(vanilla.internal, Blocks.SAND.defaultBlockState());
            case DIRT_DUST -> new BlockParticleOption(vanilla.internal, Blocks.DIRT.defaultBlockState());
            case REDSTONE -> new DustParticleOptions(new Vector3f(1, 1, 1), 1);
            default -> (ParticleOptions) vanilla.internal;
        };
        net.minecraft.client.particle.Particle particle =
                Minecraft.getInstance().particleEngine.createParticle(options, pos.x, pos.y, pos.z, velocity.x, velocity.y, velocity.z);

        if (particle != null) {
            particle.scale(scale);
        }
    }

    /** Used to render multiple particles in the same function for efficiency */
    @FunctionalInterface
    public interface MultiRenderer<I extends Particle> {
        void accept(List<I> l, RenderState state, float pt);
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

    public enum VanillaParticles {
        ANGRY_VILLAGER(ParticleTypes.ANGRY_VILLAGER),
        BUBBLE(ParticleTypes.BUBBLE),
        CRITICAL_HIT(ParticleTypes.CRIT),
        CRITICAL_MAGIC_HIT(ParticleTypes.ENCHANTED_HIT),
        DIRT_DUST(ParticleTypes.BLOCK),
        EXPLOSION(ParticleTypes.EXPLOSION),
        FLAME(ParticleTypes.FLAME),
        HAPPY_VILLAGER(ParticleTypes.HAPPY_VILLAGER),
        HEART(ParticleTypes.HEART),
        LAVA(ParticleTypes.LAVA),
        LAVA_DROP(ParticleTypes.DRIPPING_LAVA),
        LARGE_SMOKE(ParticleTypes.LARGE_SMOKE),
        NORMAL_SMOKE(ParticleTypes.SMOKE),
        NOTE(ParticleTypes.NOTE),
        REDSTONE(ParticleTypes.DUST),
        RUNE(ParticleTypes.ENCHANT),
        SAND_DUST(ParticleTypes.BLOCK),
        SNOWBALL_BREAKING(ParticleTypes.ITEM_SNOWBALL),
        SNOW_SHOVEL(ParticleTypes.ITEM_SNOWBALL), //Removed in 1.14
        WATER_DRIP(ParticleTypes.DRIPPING_WATER),
        WATER_SPLASH(ParticleTypes.SPLASH)
        ;

        private final ParticleType internal;

        VanillaParticles(ParticleType type) {
            this.internal = type;
        }
    }
}
