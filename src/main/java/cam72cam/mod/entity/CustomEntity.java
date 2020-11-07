package cam72cam.mod.entity;

import cam72cam.mod.entity.sync.EntitySync;
import cam72cam.mod.world.World;

import java.util.List;

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
     * @see ModdedEntity
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

}
