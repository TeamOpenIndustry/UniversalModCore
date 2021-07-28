package cam72cam.mod.entity;

import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.SingleCache;
import cam72cam.mod.world.World;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.world.Explosion;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The base entity abstraction that wraps MC entities.
 *
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
        return World.get(internal.worldObj);
    }

    /** UUID that persists across loads */
    public UUID getUUID() {
        return internal.getPersistentID();
    }

    private final SingleCache<Vec3d, Vec3i> blockPosCache = new SingleCache<>(pos -> new Vec3i(internal.posX, internal.posY, internal.posZ));
    /* Position / Rotation */
    public Vec3i getBlockPosition() {
        return blockPosCache.get(getPosition());
    }

    private Vec3d posCache;
    public Vec3d getPosition() {
        if (posCache == null || (
                posCache.x != internal.posX ||
                posCache.y != internal.posY ||
                posCache.z != internal.posZ )
        ) {
            posCache = new Vec3d(internal.posX, internal.posY, internal.posZ);
        }
        return posCache;
    }

    public void setPosition(Vec3d pos) {
        internal.setPosition(pos.x, pos.y, pos.z);
    }

    public Vec3d getVelocity() {
        return new Vec3d(internal.motionX, internal.motionY, internal.motionZ);
    }

    public void setVelocity(Vec3d motion) {
        internal.motionX = motion.x;
        internal.motionY = motion.y;
        internal.motionZ = motion.z;
    }

    public float getRotationYaw() {
        return internal.rotationYaw;
    }

    public void setRotationYaw(float yaw) {
        internal.prevRotationYaw = internal.rotationYaw;
        internal.rotationYaw = yaw;
        double d0 = internal.prevRotationYaw - yaw;
        if (d0 < -180.0D)
        {
            internal.prevRotationYaw += 360.0F;
        }

        if (d0 >= 180.0D)
        {
            internal.prevRotationYaw -= 360.0F;
        }

    }

    public float getRotationPitch() {
        return internal.rotationPitch;
    }

    public void setRotationPitch(float pitch) {
        internal.prevRotationPitch = internal.rotationPitch;
        internal.rotationPitch = pitch;
    }

    public float getPrevRotationYaw() {
        return internal.prevRotationYaw;
    }

    public float getPrevRotationPitch() {
        return internal.prevRotationPitch;
    }

    Vec3d eyeCache;
    public Vec3d getPositionEyes() {
        if (eyeCache == null || (
                eyeCache.x != internal.posX ||
                eyeCache.y != internal.posY + internal.getEyeHeight() ||
                eyeCache.z != internal.posZ )
        ) {
            eyeCache = new Vec3d(internal.posX, internal.posY + internal.getEyeHeight(), internal.posZ);
        }
        return eyeCache;
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
        return internal instanceof EntityVillager;
    }

    public boolean isMob() {
        return internal instanceof EntityMob;
    }

    public boolean isPlayer() {
        return this instanceof Player;
    }

    public boolean isLiving() {
        return this instanceof Living;
    }

    public void kill() {
        internal.worldObj.removeEntity(internal);
    }

    public final boolean isDead() {
        return internal.isDead;
    }

    public int getTickCount() {
        return internal.ticksExisted;
    }

    public int getPassengerCount() {
        return internal.riddenByEntity != null ? 1 : 0;
    }

    public void addPassenger(cam72cam.mod.entity.Entity passenger) {
        passenger.internal.mountEntity(internal);
    }

    public boolean isPassenger(cam72cam.mod.entity.Entity passenger) {
        return internal.ridingEntity != null && internal.ridingEntity.getPersistentID().equals(passenger.getUUID());
    }

    public void removePassenger(Entity entity) {
        entity.internal.mountEntity(null);
    }

    public List<Entity> getPassengers() {
        List<Entity> passengers = new ArrayList<>();
        if (internal.riddenByEntity != null) {
            Entity passenger = getWorld().getEntity(internal.riddenByEntity.getUniqueID(), Entity.class);
            if (passenger != null) {
                passengers.add(passenger);
            }
        }

        return passengers;
    }

    public Entity getRiding() {
        if (internal.ridingEntity != null) {
            if (internal.ridingEntity instanceof SeatEntity) {
                return ((SeatEntity)internal.ridingEntity).getParent();
            }
            return getWorld().getEntity(internal.ridingEntity);
        }
        return null;
    }

    private final SingleCache<AxisAlignedBB, IBoundingBox> boundingBox = new SingleCache<>(IBoundingBox::from);
    public IBoundingBox getBounds() {
        return boundingBox.get(internal.boundingBox);
    }

    public float getRotationYawHead() {
        return internal.getRotationYawHead();
    }

    public Vec3d getLastTickPos() {
        return new Vec3d(internal.lastTickPosX, internal.lastTickPosY, internal.lastTickPosZ);
    }

    public void startRiding(Entity entity) {
        entity.addPassenger(this);
    }

    /** If riding this entity, what modifier should be applied to the overall sound level */
    public float getRidingSoundModifier() {
        return 1;
    }

    /** Damage entity directly (bypassing armor) */
    public void directDamage(String msg, double damage) {
        internal.attackEntityFrom((new DamageSource(msg)).setDamageBypassesArmor(), (float) damage);
    }

    protected void createExplosion(Vec3d pos, float size, boolean damageTerrain) {
        Explosion explosion = new Explosion(getWorld().internal, this.internal, pos.x, pos.y, pos.z, size);
        explosion.isFlaming = false;
        if (net.minecraftforge.event.ForgeEventFactory.onExplosionStart(getWorld().internal, explosion)) return;
        explosion.doExplosionA();
        explosion.doExplosionB(true);
    }

    /** Non persistent ID.  Should use UUID instead */
    public int getId() {
        return internal.getEntityId();
    }
}
