package cam72cam.mod.entity;

import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.SingleCache;
import cam72cam.mod.world.World;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Explosion;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The base entity abstraction that wraps MC entities.
 * <p>
 * TODO: Make sure we are setting prevRot/Loc stuff correctly.  Should it only be changed on a tick processing the movement?
 */
public class Entity {
    /** The wrapped MC construct.  Do not use directly */
    public net.minecraft.entity.Entity internal;

    /** Wrap a MC entity in UMC entity.  Do not use directly. */
    public Entity(net.minecraft.entity.Entity entity) {
        this.internal = entity;
    }

    public World getWorld() {
        return World.get(internal.world);
    }

    /** UUID that persists across loads */
    public UUID getUUID() {
        return internal.getUniqueID();
    }

    private final SingleCache<Vec3d, Vec3i> blockPosCache = new SingleCache<>(pos -> new Vec3i(internal.getPosition()));
    /* Position / Rotation */
    public Vec3i getBlockPosition() {
        return blockPosCache.get(getPosition());
    }

    private Vec3d posCache;
    public Vec3d getPosition() {
        Vec3d pos = checkRidingPosition();
        if(pos != null) {
            return pos;
        }

        if (posCache == null || (
                posCache.x != internal.getPosX() ||
                posCache.y != internal.getPosY() ||
                posCache.z != internal.getPosZ() )
        ) {
            posCache = new Vec3d(internal.getPositionVector());
        }
        return posCache;
    }

    public void setPosition(Vec3d pos) {
        internal.setPosition(pos.x, pos.y, pos.z);
    }

    public Vec3d getVelocity() {
        return new Vec3d(internal.getMotion());
    }

    public void setVelocity(Vec3d motion) {
        internal.setMotion(motion.internal());
    }

    public float getRotationYaw() {
        return internal.rotationYaw;
    }

    public float getRotationPitch() {
        return internal.rotationPitch;
    }

    /**
     * @see CustomEntity#getRotationRoll()
     */
    public float getRotationRoll() {
        return 0f;
    }

    public float getRotationYaw(float partialTicks) {
        return (float) MathHelper.clampedLerp(internal.prevRotationYaw, internal.rotationYaw, partialTicks);
    }

    public float getRotationPitch(float partialTicks) {
        return (float) MathHelper.clampedLerp(internal.prevRotationPitch, internal.rotationPitch, partialTicks);
    }

    /**
     * @see CustomEntity#getRotationRoll(float)
     */
    public float getRotationRoll(float partialTicks) {
        return 0;
    }

    public void setRotationYaw(float yaw) {
        internal.prevRotationYaw = internal.rotationYaw;
        internal.rotationYaw = yaw;

        while (internal.rotationYaw - internal.prevRotationYaw < -180.0F)
        {
            internal.prevRotationYaw -= 360.0F;
        }
        while (internal.rotationYaw - internal.prevRotationYaw >= 180.0F)
        {
            internal.prevRotationYaw += 360.0F;
        }
    }

    public void setRotationPitch(float pitch) {
        internal.prevRotationPitch = internal.rotationPitch;
        internal.rotationPitch = pitch;

        while (internal.rotationPitch - internal.prevRotationPitch < -180.0F)
        {
            internal.prevRotationPitch -= 360.0F;
        }
        while (internal.rotationPitch - internal.prevRotationPitch >= 180.0F)
        {
            internal.prevRotationPitch += 360.0F;
        }
    }

    /**
     * @see CustomEntity#setRotationRoll(float)
     */
    public void setRotationRoll(float roll) {
    }

    public float getPrevRotationYaw() {
        return internal.prevRotationYaw;
    }

    public float getPrevRotationPitch() {
        return internal.prevRotationPitch;
    }

    /**
     * @see CustomEntity#getPrevRotationRoll()
     */
    public float getPrevRotationRoll() {
        return 0f;
    }

    Vec3d eyeCache;
    public Vec3d getPositionEyes() {
        Vec3d pos = checkRidingPosition();
        if(pos != null) {
            return pos.add(0, internal.getEyeHeight(), 0);
        }

        if (eyeCache == null || (
                eyeCache.x != internal.getPosX() ||
                eyeCache.y != internal.getPosY() + internal.getEyeHeight() ||
                eyeCache.z != internal.getPosZ() )
        ) {
            eyeCache = new Vec3d(internal.getPosX(), internal.getPosY() + internal.getEyeHeight(), internal.getPosZ());
        }
        return eyeCache;
    }

    private Vec3d checkRidingPosition() {
        if (this.getRiding() != null && this.getRiding().internal instanceof ModdedEntity) {
            ModdedEntity entity = (ModdedEntity) this.getRiding().internal;
            Vec3d vec3d = entity.calculateRiderWorldPosition(this);
            if(vec3d != null) {
                return vec3d;
            }
        }
        return null;
    }


    private final SingleCache<Float, Vec3d> lookCache = new SingleCache<>(f -> new Vec3d(internal.getLookVec()));
    public Vec3d getLookVector() {
        return lookCache.get(internal.rotationYaw + internal.rotationPitch);
    }


    /* Casting */

    /** Wrapper around as(Player) */
    public Player asPlayer() {
        return as(Player.class);
    }

    /** Casting helper with instanceof check */
    public <T extends Entity> T as(Class<T> type) {
        if (type.isInstance(this)) {
            return (T) this;
        }
        return null;
    }

    public boolean isVillager() {
        return internal instanceof VillagerEntity;
    }

    public boolean isMob() {
        return internal instanceof MobEntity;
    }

    public boolean isPlayer() {
        return this instanceof Player;
    }

    public boolean isLiving() {
        return this instanceof Living;
    }

    public void kill() {
        internal.remove();
    }

    public final boolean isDead() {
        return !internal.isAlive();
    }

    public int getTickCount() {
        return internal.ticksExisted;
    }

    public int getPassengerCount() {
        return internal.getPassengers().size();
    }

    public void addPassenger(cam72cam.mod.entity.Entity passenger) {
        passenger.internal.startRiding(internal);
    }

    public boolean isPassenger(cam72cam.mod.entity.Entity passenger) {
        return internal.isPassenger(passenger.internal);
    }

    public void removePassenger(Entity entity) {
        entity.internal.stopRiding();
    }

    public List<Entity> getPassengers() {
        return internal.getPassengers().stream().map(Entity::new).collect(Collectors.toList());
    }

    public Entity getRiding() {
        if (internal.getRidingEntity() != null) {
            if (internal.getRidingEntity() instanceof SeatEntity) {
                return ((SeatEntity)internal.getRidingEntity()).getParent();
            }
            return getWorld().getEntity(internal.getRidingEntity());
        }
        return null;
    }

    private final SingleCache<AxisAlignedBB, IBoundingBox> boundingBox = new SingleCache<>(IBoundingBox::from);
    public IBoundingBox getBounds() {
        return boundingBox.get(internal.getBoundingBox());
    }

    public float getRotationYawHead() {
        return internal.getRotationYawHead();
    }

    public Vec3d getLastTickPos() {
        return new Vec3d(internal.lastTickPosX, internal.lastTickPosY, internal.lastTickPosZ);
    }

    public void startRiding(Entity entity) {
        internal.startRiding(entity.internal);
    }

    /** If riding this entity, what modifier should be applied to the overall sound level */
    public float getRidingSoundModifier() {
        return 1;
    }

    /** Damage entity directly (bypassing armor) */
    public void directDamage(DamageType type, double damage) {
        internal.attackEntityFrom(type.internal.setDamageBypassesArmor(), (float) damage);
    }

    protected void createExplosion(Vec3d pos, float size, boolean damageTerrain) {
        Explosion explosion = new Explosion(getWorld().internal, this.internal, pos.x, pos.y, pos.z, size, false, damageTerrain ? Explosion.Mode.DESTROY : Explosion.Mode.NONE);
        if (net.minecraftforge.event.ForgeEventFactory.onExplosionStart(getWorld().internal, explosion)) return;
        explosion.doExplosionA();
        explosion.doExplosionB(true);
    }

    /** Non persistent ID.  Should use UUID instead */
    public int getId() {
        return internal.getEntityId();
    }
}
