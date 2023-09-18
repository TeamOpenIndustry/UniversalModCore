package cam72cam.mod.entity;

import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.SingleCache;
import cam72cam.mod.world.World;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.AABB;

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
    public net.minecraft.world.entity.Entity internal;

    /** Wrap a MC entity in UMC entity.  Do not use directly. */
    public Entity(net.minecraft.world.entity.Entity entity) {
        this.internal = entity;
    }

    public World getWorld() {
        return World.get(internal.level());
    }

    /** UUID that persists across loads */
    public UUID getUUID() {
        return internal.getUUID();
    }

    private final SingleCache<Vec3d, Vec3i> blockPosCache = new SingleCache<>(pos -> new Vec3i(internal.blockPosition()));
    /* Position / Rotation */
    public Vec3i getBlockPosition() {
        return blockPosCache.get(getPosition());
    }

    private Vec3d posCache;
    public Vec3d getPosition() {
        if (posCache == null || (
                posCache.x != internal.getX() ||
                posCache.y != internal.getY() ||
                posCache.z != internal.getZ() )
        ) {
            posCache = new Vec3d(internal.position());
        }
        return posCache;
    }

    public void setPosition(Vec3d pos) {
        internal.setPos(pos.x, pos.y, pos.z);
    }

    public Vec3d getVelocity() {
        return new Vec3d(internal.getDeltaMovement());
    }

    public void setVelocity(Vec3d motion) {
        internal.setDeltaMovement(motion.internal());
    }

    public float getRotationYaw() {
        return internal.getYRot();
    }

    public void setRotationYaw(float yaw) {
        internal.yRotO = internal.getYRot();
        internal.setYRot(yaw);
        double d0 = internal.yRotO - yaw;
        if (d0 < -180.0D)
        {
            internal.yRotO += 360.0F;
        }

        if (d0 >= 180.0D)
        {
            internal.yRotO -= 360.0F;
        }

    }

    public float getRotationPitch() {
        return internal.getXRot();
    }

    public void setRotationPitch(float pitch) {
        internal.xRotO = internal.getXRot();
        internal.setXRot(pitch);
    }

    public float getPrevRotationYaw() {
        return internal.yRotO;
    }

    public float getPrevRotationPitch() {
        return internal.xRotO;
    }

    Vec3d eyeCache;
    public Vec3d getPositionEyes() {
        if (eyeCache == null || (
                eyeCache.x != internal.getX() ||
                eyeCache.y != internal.getY() + internal.getEyeHeight() ||
                eyeCache.z != internal.getZ() )
        ) {
            eyeCache = new Vec3d(internal.getX(), internal.getY() + internal.getEyeHeight(), internal.getZ());
        }
        return eyeCache;
    }


    private final SingleCache<Float, Vec3d> lookCache = new SingleCache<>(f -> new Vec3d(internal.getLookAngle()));
    public Vec3d getLookVector() {
        return lookCache.get(internal.getXRot() + internal.getYRot());
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
        return internal instanceof AbstractVillager;
    }

    public boolean isMob() {
        return internal instanceof Mob;
    }

    public boolean isPlayer() {
        return this instanceof Player;
    }

    public boolean isLiving() {
        return this instanceof Living;
    }

    public void kill() {
        internal.remove(!internal.level().isClientSide ? net.minecraft.world.entity.Entity.RemovalReason.KILLED : net.minecraft.world.entity.Entity.RemovalReason.DISCARDED); // TODO MAYBE BORK
    }

    public final boolean isDead() {
        return !internal.isAlive();
    }

    public int getTickCount() {
        return internal.tickCount;
    }

    public int getPassengerCount() {
        return internal.getPassengers().size();
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
        return internal.getPassengers().stream().map(Entity::new).collect(Collectors.toList());
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

    private final SingleCache<AABB, IBoundingBox> boundingBox = new SingleCache<>(IBoundingBox::from);
    public IBoundingBox getBounds() {
        return boundingBox.get(internal.getBoundingBox());
    }

    public float getRotationYawHead() {
        return internal.getYHeadRot();
    }

    public Vec3d getLastTickPos() {
        return new Vec3d(internal.xOld, internal.yOld, internal.zOld);
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
        // TODO 1.19.4 internal.hurt((new DamageSource(msg)).bypassArmor(), (float) damage);
    }

    protected void createExplosion(Vec3d pos, float size, boolean damageTerrain) {
        Explosion explosion = new Explosion(getWorld().internal, this.internal, null, null, pos.x, pos.y, pos.z, size, false, damageTerrain ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.KEEP);
        if (net.minecraftforge.event.ForgeEventFactory.onExplosionStart(getWorld().internal, explosion)) return;
        explosion.explode();
        explosion.finalizeExplosion(true);
    }

    /** Non persistent ID.  Should use UUID instead */
    public int getId() {
        return internal.getId();
    }
}
