package cam72cam.mod.entity;

import cam72cam.mod.entity.sync.EntitySync;
import cam72cam.mod.entity.sync.TagSync;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.net.Packet;
import cam72cam.mod.serialization.TagField;
import net.minecraft.util.math.MathHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implement to create a custom modded entity
 *
 * Wrapper around ModdedEntity for internal functionality
 */
public class CustomEntity extends Entity {
    /** Internal MC construct.  Do not use directly */
    public ModdedEntity internal;

    /** Data that is automatically synchronized from server to client on tick */
    public final EntitySync sync;

    /** Internal roll implementation */
    @TagField
    private float rotationRoll;
    @TagField
    private float prevRotationRoll;

    /** Do not use directly.  Construct via world on ModdedEntity load */
    protected CustomEntity() {
        super(null);
        this.sync = new EntitySync(this);
    }

    public boolean isImmuneToFire() {
        return false;
    }

    public float getCollisionReduction() {
        return 0;
    }

    public boolean canBePushed() {
        return true;
    }

    public boolean allowsDefaultMovement() {
        return true;
    }

    /**
     * So I did not want to have to pass ModdedEntity into the constructor of each custom entity.
     * That approach is leaky and directly exposes implementation details.
     * 
     * Instead this is a pseudo post-constuctor that is fired directly after the custom entity has been
     * constructed via EntityRegistry.
     * 
     * @see EntityRegistry#create 
     */
    CustomEntity setup(ModdedEntity entity) {
        super.internal = entity;
        this.internal = entity;
        return this;
    }

    /** Allow entities to refuse to load.  If a non-null value is passed we error out */
    public String tryJoinWorld() {
        return null;
    }

    @Override
    public int getPassengerCount() {
        return internal.getPassengerCount();
    }

    @Override
    public void addPassenger(cam72cam.mod.entity.Entity passenger) {
        if (passenger.getRiding() instanceof CustomEntity) {
            // If they are already riding a custom passenger, don't fire the dismount handler directly
            ((ModdedEntity)passenger.getRiding().internal).moveRiderTo(passenger, this);
        } else {
            super.addPassenger(passenger);
        }
    }

    @Override
    public boolean isPassenger(cam72cam.mod.entity.Entity passenger) {
        return internal.isPassenger(passenger);
    }

    @Override
    public void removePassenger(Entity entity) {
        internal.removePassenger(entity);
    }

    @Override
    public List<Entity> getPassengers() {
        return internal.getActualPassengers();
    }

    @Override
    public float getRotationRoll() {
        return rotationRoll;
    }

    @Override
    public void setRotationRoll(float roll) {
        this.prevRotationRoll = this.rotationRoll;
        this.rotationRoll = roll;
        while (roll - prevRotationRoll < -180.0F)
        {
            prevRotationRoll -= 360.0F;
        }
        while (roll - prevRotationRoll >= 180.0F)
        {
            prevRotationRoll += 360.0F;
        }
    }

    @Override
    public float getRotationRoll(float partialTicks) {
        return (float) MathHelper.clampedLerp(prevRotationRoll, rotationRoll, partialTicks);
    }

    @Override
    public float getPrevRotationRoll() {
        return prevRotationRoll;
    }

    public void tickRoll() {
        this.prevRotationRoll = rotationRoll;
    }

    public static class RollPacket extends Packet {
        @TagField("t")
        private CustomEntity target;
        @TagField("r")
        private float roll;
        @TagField("p")
        private float prevRoll;

        public RollPacket() {}

        public RollPacket(CustomEntity entity) {
            this.target = entity;
            this.roll = entity.rotationRoll;
            this.prevRoll = entity.prevRotationRoll;
        }

        @Override
        public void handle() {
            if (target != null && target.internal instanceof ModdedEntity) {
                target.rotationRoll = roll;
                target.prevRotationRoll = prevRoll;
            }
        }
    }
}
