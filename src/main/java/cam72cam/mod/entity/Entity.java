package cam72cam.mod.entity;

import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.SingleCache;
import cam72cam.mod.world.World;
import net.minecraft.entity.damage.EntityDamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.world.explosion.Explosion;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
        return World.get(internal.world);
    }

    /** UUID that persists across loads */
    public UUID getUUID() {
        return internal.getUuid();
    }

    private final SingleCache<Vec3d, Vec3i> blockPosCache = new SingleCache<>(pos -> new Vec3i(internal.getBlockPos()));
    /* Position / Rotation */
    public Vec3i getBlockPosition() {
        return blockPosCache.get(getPosition());
    }

    private Vec3d posCache;
    public Vec3d getPosition() {
        if (posCache == null || (
                posCache.x != internal.x ||
                posCache.y != internal.y ||
                posCache.z != internal.z )
        ) {
            posCache = new Vec3d(internal.getPos());
        }
        return posCache;
    }

    public void setPosition(Vec3d pos) {
        internal.updatePosition(pos.x, pos.y, pos.z);
    }

    public Vec3d getVelocity() {
        return new Vec3d(internal.getVelocity());
    }

    public void setVelocity(Vec3d motion) {
        internal.setVelocity(motion.internal());
    }

    public float getRotationYaw() {
        return internal.yaw;
    }

    public void setRotationYaw(float yaw) {
        internal.prevYaw = internal.yaw;
        internal.yaw = yaw;
        double d0 = internal.prevYaw - yaw;
        if (d0 < -180.0D)
        {
            internal.prevYaw += 360.0F;
        }

        if (d0 >= 180.0D)
        {
            internal.prevYaw -= 360.0F;
        }
    }

    public float getRotationPitch() {
        return internal.pitch;
    }

    public void setRotationPitch(float pitch) {
        internal.prevPitch = internal.pitch;
        internal.pitch = pitch;
    }

    public float getPrevRotationYaw() {
        return internal.prevYaw;
    }

    public float getPrevRotationPitch() {
        return internal.prevPitch;
    }

    Vec3d eyeCache;
    public Vec3d getPositionEyes() {
        if (eyeCache == null || (
                eyeCache.x != internal.x ||
                eyeCache.y != internal.y + internal.getStandingEyeHeight() ||
                eyeCache.z != internal.z )
        ) {
            eyeCache = new Vec3d(internal.x, internal.y + internal.getStandingEyeHeight(), internal.z);
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
        return internal.age;
    }

    public int getPassengerCount() {
        return internal.getPassengerList().size();
    }

    public void addPassenger(cam72cam.mod.entity.Entity passenger) {
        passenger.internal.startRiding(internal);
    }

    public boolean isPassenger(cam72cam.mod.entity.Entity passenger) {
        return internal.hasPassenger(passenger.internal);
    }

    public void removePassenger(Entity entity) {
        entity.internal.stopRiding();
    }

    public List<Entity> getPassengers() {
        return internal.getPassengerList().stream().map(Entity::new).collect(Collectors.toList());
    }

    public Entity getRiding() {
        if (internal.getVehicle() != null) {
            if (internal.getVehicle() instanceof SeatEntity) {
                return ((SeatEntity)internal.getVehicle()).getParent();
            }
            return getWorld().getEntity(internal.getVehicle());
        }
        return null;
    }

    private final SingleCache<Box, IBoundingBox> boundingBox = new SingleCache<>(IBoundingBox::from);
    public IBoundingBox getBounds() {
        return IBoundingBox.from(internal.getBoundingBox());
    }

    public float getRotationYawHead() {
        return internal.getHeadYaw();
    }

    public Vec3d getLastTickPos() {
        return new Vec3d(internal.prevX, internal.prevY, internal.prevZ);
    }

    public void startRiding(Entity entity) {
        internal.startRiding(entity.internal);
    }

    /** If riding this entity, what modifier should be applied to the overall sound level */
    public float getRidingSoundModifier() {
        return 1;
    }

    /** Damage entity directly (bypassing armor) */
    public void directDamage(String msg, double damage) {
        EntityDamageSource source = new EntityDamageSource(msg, null);
        source.bypassesArmor();
        internal.damage(source, (float) damage);
    }

    protected void createExplosion(Vec3d pos, float size, boolean damageTerrain) {
        Explosion explosion = new Explosion(getWorld().internal, this.internal, pos.x, pos.y, pos.z, size, false, damageTerrain ? Explosion.DestructionType.DESTROY : Explosion.DestructionType.NONE);
        explosion.collectBlocksAndDamageEntities();
        explosion.affectWorld(true);
    }

    /** Non persistent ID.  Should use UUID instead */
    public int getId() {
        return internal.getEntityId();
    }
}
