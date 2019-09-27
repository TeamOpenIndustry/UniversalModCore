package cam72cam.mod.entity;

import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.entity.sync.EntitySync;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.net.Packet;
import cam72cam.mod.world.World;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.world.Explosion;
import cpw.mods.fml.common.registry.EntityRegistry;

import java.util.ArrayList;
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
        return World.get(internal.worldObj);
    }

    public UUID getUUID() {
        return internal.getPersistentID();
    }

    /* Position / Rotation */

    public Vec3i getBlockPosition() {
        return new Vec3i((int)Math.floor(internal.posX), (int)Math.floor(internal.posY), (int)Math.floor(internal.posZ));
    }

    public Vec3d getPosition() {
        return new Vec3d(internal.posX, internal.posY, internal.posZ);
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

    public Vec3d getPositionEyes(float partialTicks) {
        return getPosition().add(0, internal.getEyeHeight(), 0);
    }


    /* Casting */


    public Player asPlayer() {
        if (internal instanceof EntityPlayer) {
            return new Player((EntityPlayer) internal);
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
        return this.is(EntityVillager.class);
    }

    public void kill() {
        internal.worldObj.removeEntity(internal);
    }

    public final boolean isDead() {
        return internal.isDead;
    }


    /* Networking */

    public void sendToObserving(Packet packet) {
        boolean found = false;
        int syncDist = EntityRegistry.instance().lookupModSpawn(internal.getClass(), true).getTrackingRange();
        for (EntityPlayer player : (List<EntityPlayer>)internal.worldObj.playerEntities) {
            if (new Entity(player).getPosition().distanceTo(getPosition()) < syncDist) {
                found = true;
                break;
            }
        }
        if (found) {
            packet.sendToAllAround(getWorld(), getPosition(), syncDist);
        }
    }

    public int getTickCount() {
        return internal.ticksExisted;
    }

    //TODO 1.7.10 custom passenger system!

    public int getPassengerCount() {
        if (modded != null) {
            return modded.getPassengerCount();
        } else {
            return internal.riddenByEntity != null ? 1 : 0;
        }
    }

    public final void addPassenger(cam72cam.mod.entity.Entity entity) {
        if (internal instanceof ModdedEntity) {
            ((ModdedEntity) internal).addPassenger(entity.internal);
        } else {
            entity.internal.mountEntity(internal);
        }
    }

    public final boolean isPassenger(cam72cam.mod.entity.Entity passenger) {
        if (modded != null) {
            return modded.isPassenger(passenger);
        }
        return internal.ridingEntity != null && internal.ridingEntity.getPersistentID().equals(passenger.getUUID());
    }

    public void removePassenger(Entity entity) {
        if (modded != null) {
            modded.removePassenger(entity);
        }
        entity.internal.mountEntity(null);
    }

    public List<Entity> getPassengers() {
        if (modded != null) {
            return modded.getActualPassengers();
        }

        List<Entity> passengers = new ArrayList<>();
        if (internal.riddenByEntity != null) {
            Entity passenger = getWorld().getEntity(internal.riddenByEntity.getUniqueID(), Entity.class);
            if (passenger != null) {
                passengers.add(passenger);
            }
        }

        return passengers;
    }

    public boolean isPlayer() {
        return internal instanceof EntityPlayer;
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

    public IBoundingBox getBounds() {
        return IBoundingBox.from(internal.boundingBox);
    }

    public float getRotationYawHead() {
        return internal.getRotationYawHead();
    }

    public Vec3d getLastTickPos() {
        return new Vec3d(internal.lastTickPosX, internal.lastTickPosY, internal.lastTickPosZ);
    }

    public boolean isLiving() {
        return internal instanceof EntityLivingBase;
    }

    public void startRiding(Entity entity) {
        entity.addPassenger(this);
    }

    public float getRidingSoundModifier() {
        return 1;
    }

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

    public int getId() {
        return internal.getEntityId();
    }
}
