package cam72cam.mod.render;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.world.World;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.*;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.client.renderer.Tessellator;
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
            net.minecraft.client.particle.EntityFX p = new net.minecraft.client.particle.EntityFX(data.world.internal, data.pos.x, data.pos.y, data.pos.z, data.motion.x, data.motion.y, data.motion.z) {
                {
                    particleMaxAge = data.lifespan;
                    motionX = data.motion.x;
                    motionY = data.motion.y;
                    motionZ = data.motion.z;
                    ip.posX = posX;
                    ip.posY = posY;
                    ip.posZ = posZ;
                    this.noClip = true;
                }

                /*1.7.10 @Override
                public boolean isTransparent() {
                    return !ip.depthTestEnabled();
                }*/

                @Override
                public void onUpdate() {
                    super.onUpdate();
                    ip.posX = posX;
                    ip.posY = posY;
                    ip.posZ = posZ;
                }

                @Override
                public void renderParticle(Tessellator buffer, float partialTicks, float rotationX, float rotationZ, float rotationYZ, float rotationXY, float rotationXZ) {
                    ip.ticks = particleAge;
                    ip.renderX = posX - interpPosX + this.motionX * partialTicks;
                    ip.renderY = posY - interpPosY + this.motionY * partialTicks;
                    ip.renderZ = posZ - interpPosZ + this.motionZ * partialTicks;

                    if (renderer == null) {
                        RenderState state = new RenderState();
                        state.translate(ip.renderX, ip.renderY, ip.renderZ);
                        ip.render(state, partialTicks);
                    } else {
                        if (!ip.canRender) {
                            renderer.accept(particles, new RenderState(), partialTicks);
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
    protected abstract void render(RenderState state, float partialTicks);

    protected void lookAtPlayer(Matrix4 mat) {
        Vec3d eyes = MinecraftClient.getPlayer().getPositionEyes();
        double x = eyes.x - posX;
        double y = eyes.y - posY;
        double z = eyes.z - posZ;
        mat.rotate(Math.toRadians(180 - Math.toDegrees(Math.atan2(-x, z))), 0, 1, 0);
        mat.rotate(Math.toRadians(180 - Math.toDegrees(Math.atan2(Math.sqrt(z * z + x * x), y))) + 90, 1, 0, 0);
    }

    public static void renderVanilla(VanillaParticles vanilla, Vec3d pos, Vec3d velocity, float scale) {
        if (scale < 1E-7
                || !MinecraftClient.isReady()
                || MinecraftClient.getPlayer() == null
                || MinecraftClient.getPlayer().getWorld() == null) {
            return;
        }

        EntityFX particle;
//        particle = Minecraft.getMinecraft().effectRenderer.spawnEffectParticle(
//                vanilla.internal.getParticleID(), pos.x, pos.y, pos.z, velocity.x, velocity.y, velocity.z, extraArgument);
        particle = vanilla.internal.create(MinecraftClient.getPlayer().getWorld().internal, pos.x, pos.y, pos.z, velocity.x, velocity.y, velocity.z);

        if (particle != null) {
            particle.multipleParticleScaleBy(scale);
        }

        Minecraft.getMinecraft().effectRenderer.addEffect(particle);
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
        ANGRY_VILLAGER(
                (world, xPos, yPos, zPos, xMotion, yMotion, zMotion) -> {
                    EntityFX entityfx = new EntityHeartFX(world, xPos, yPos, zPos, xMotion, yMotion, zMotion);
                    entityfx.setParticleTextureIndex(81);
                    entityfx.setRBGColorF(1.0F, 1.0F, 1.0F);
                    return entityfx;
                }
        ),
        BUBBLE(EntityBubbleFX::new),
        CRITICAL_HIT(EntityCritFX::new),
        CRITICAL_MAGIC_HIT(
                (world, xPos, yPos, zPos, xMotion, yMotion, zMotion) -> {
                    EntityFX entityfx = new EntityCritFX(world, xPos, yPos, zPos, xMotion, yMotion, zMotion);
                    entityfx.setRBGColorF(entityfx.getRedColorF() * 0.3F, entityfx.getGreenColorF() * 0.8F, entityfx.getBlueColorF());
                    entityfx.nextTextureIndexX();
                    return entityfx;
                }
        ),
        DIRT_DUST(
                (world, xPos, yPos, zPos, xMotion, yMotion, zMotion) -> {
                    Block block = Blocks.dirt;
                    return new EntityBlockDustFX(world, xPos, yPos, zPos, xMotion, yMotion, zMotion, block, 2).applyRenderColor(2);
                }
        ),
        EXPLOSION(EntityExplodeFX::new),
        FLAME(EntityFlameFX::new),
        HAPPY_VILLAGER(
                (world, xPos, yPos, zPos, xMotion, yMotion, zMotion) -> {
                    EntityFX entityfx = new EntityAuraFX(world, xPos, yPos, zPos, xMotion, yMotion, zMotion);
                    entityfx.setParticleTextureIndex(82);
                    entityfx.setRBGColorF(1.0F, 1.0F, 1.0F);
                    return entityfx;
                }
        ),
        HEART(EntityHeartFX::new),
        LAVA(
                (world, xPos, yPos, zPos, xMotion, yMotion, zMotion) ->
                        new EntityLavaFX(world, xPos, yPos, zPos)
        ),
        LAVA_DROP(
                (world, xPos, yPos, zPos, xMotion, yMotion, zMotion) ->
                        new EntityDropParticleFX(world, xPos, yPos, zPos, Material.lava)

        ),
        LARGE_SMOKE(
                (world, xPos, yPos, zPos, xMotion, yMotion, zMotion) ->
                        new EntitySmokeFX(world, xPos, yPos, zPos, xMotion, yMotion, zMotion, 2.5F)
        ),
        NORMAL_SMOKE(EntitySmokeFX::new),
        NOTE(EntityNoteFX::new),
        REDSTONE(
                (world, xPos, yPos, zPos, xMotion, yMotion, zMotion) ->
                        new EntityReddustFX(world, xPos, yPos, zPos, 1, 1, 1, 1)
        ),
        RUNE(EntityEnchantmentTableParticleFX::new),
        SAND_DUST(
                (world, xPos, yPos, zPos, xMotion, yMotion, zMotion) -> {
                    Block block = Blocks.sand;
                    return new EntityBlockDustFX(world, xPos, yPos, zPos, xMotion, yMotion, zMotion, block, 2).applyRenderColor(2);
                }
        ),
        SNOWBALL_BREAKING(
                (world, xPos, yPos, zPos, xMotion, yMotion, zMotion) ->
                        new EntityBreakingFX(world, xPos, yPos, zPos, Items.snowball)
        ),
        SNOW_SHOVEL(EntitySnowShovelFX::new),
        WATER_DRIP(
                (world, xPos, yPos, zPos, xMotion, yMotion, zMotion) ->
                        new EntityDropParticleFX(world, xPos, yPos, zPos, Material.water)

        ),
        WATER_SPLASH(EntitySplashFX::new)
        ;

        final ParticleFactory internal;

        VanillaParticles(ParticleFactory type) {
            this.internal = type;
        }
    }

    @FunctionalInterface
    private interface ParticleFactory {
        EntityFX create(net.minecraft.world.World world, double xPos, double yPos, double zPos, double xMotion, double yMotion, double zMotion);
    }
}
