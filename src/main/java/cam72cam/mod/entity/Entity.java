package cam72cam.mod.entity;

import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.entity.sync.EntitySync;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.net.Packet;
import cam72cam.mod.world.World;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.EntityDamageSource;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.explosion.Explosion;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class Entity {
    public final EntitySync sync;
    public net.minecraft.entity.Entity internal;
    private ModdedEntity modded;

    protected Entity() {
        this.sync = new EntitySync(this);
    }

    public Entity(net.minecraft.entity.Entity entity) {
        this();
        setup(entity);
    }

    Entity setup(net.minecraft.entity.Entity entity) {
        this.internal = entity;
        this.modded = entity instanceof ModdedEntity ? (ModdedEntity) entity : null;
        return this;
    }

    public String tryJoinWorld() {
        return null;
    }

    public World getWorld() {
        return World.get(internal.world);
    }

    public UUID getUUID() {
        return internal.getUuid();
    }

    /* Position / Rotation */

    public Vec3i getBlockPosition() {
        return new Vec3i(internal.getPos());
    }

    public Vec3d getPosition() {
        return new Vec3d(internal.getPosVector());
    }

    public void setPosition(Vec3d pos) {
        internal.setPosition(pos.x, pos.y, pos.z);
    }

    public Vec3d getVelocity() {
        return new Vec3d(internal.getVelocity());
    }

    public void setVelocity(Vec3d motion) {
        internal.setVelocity(motion.internal);
    }

    public float getRotationYaw() {
        return internal.yaw;
    }

    public void setRotationYaw(float yaw) {
        internal.prevYaw = internal.yaw;
        internal.yaw = yaw;
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

    public Vec3d getPositionEyes(float partialTicks) {
        return new Vec3d(internal.x, internal.y + internal.getStandingEyeHeight(), internal.z);
    }


    /* Casting */


    public Player asPlayer() {
        if (internal instanceof PlayerEntity) {
            return new Player((PlayerEntity) internal);
        }
        return null;
    }

    public boolean is(Class<? extends net.minecraft.entity.Entity> entity) {
        return entity.isInstance(internal);
    }

    public <T extends net.minecraft.entity.Entity> T asInternal(Class<T> entity) {
        if (internal.getClass().isInstance(entity)) {
            return (T) internal;
        }
        return null;
    }

    public <T extends Entity> T as(Class<T> type) {
        if (type.isInstance(this)) {
            return (T) this;
        }
        return null;
    }

    public boolean isVillager() {
        return this.is(VillagerEntity.class);
    }

    public void kill() {
        internal.remove();
    }

    public final boolean isDead() {
        return !internal.isAlive();
    }


    /* Networking */

    public void sendToObserving(Packet packet) {
        boolean found = false;
        int syncDist = internal.getType().getMaxTrackDistance();
        for (PlayerEntity player : internal.world.getPlayers()) {
            if (player.getPosVector().distanceTo(internal.getPosVector()) < syncDist) {
                found = true;
                break;
            }
        }
        if (found) {
            packet.sendToAllAround(getWorld(), getPosition(), syncDist);
        }
    }

    public int getTickCount() {
        return internal.age;
    }

    public int getPassengerCount() {
        if (modded != null) {
            return modded.getPassengerCount();
        } else {
            return internal.getPassengerList().size();
        }
    }

    public final void addPassenger(cam72cam.mod.entity.Entity entity) {
        entity.internal.startRiding(internal);
    }

    public final boolean isPassenger(cam72cam.mod.entity.Entity passenger) {
        if (modded != null) {
            return modded.isPassenger(passenger);
        }
        return internal.hasPassenger(passenger.internal);
    }

    public void removePassenger(Entity entity) {
        if (modded != null) {
            modded.removePassenger(entity);
        } else {
            entity.internal.stopRiding();
        }
    }

    public List<Entity> getPassengers() {
        if (modded != null) {
            return modded.getActualPassengers();
        }
        return internal.getPassengerList().stream().map(Entity::new).collect(Collectors.toList());
    }

    public boolean isPlayer() {
        return internal instanceof PlayerEntity;
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

    public IBoundingBox getBounds() {
        return IBoundingBox.from(internal.getBoundingBox());
    }

    public float getRotationYawHead() {
        return internal.getHeadYaw();
    }

    public Vec3d getLastTickPos() {
        return new Vec3d(internal.prevX, internal.prevY, internal.prevZ);
    }

    public boolean isLiving() {
        return internal instanceof LivingEntity;
    }

    public void startRiding(Entity entity) {
        internal.startRiding(entity.internal);
    }

    public float getRidingSoundModifier() {
        return 1;
    }

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

    public int getId() {
        return internal.getEntityId();
    }
}
