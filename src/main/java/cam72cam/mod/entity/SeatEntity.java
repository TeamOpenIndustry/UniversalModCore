package cam72cam.mod.entity;

import cam72cam.mod.ModCore;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.world.World;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Packet;
import net.minecraft.util.Identifier;

import java.util.UUID;

/** Seat construct to make multiple riders actually work */
public class SeatEntity extends Entity implements IAdditionalSpawnData {
    public static EntityType<SeatEntity> TYPE;
    static final Identifier ID = new Identifier(ModCore.MODID, "seat");

    // What it's a part of
    private UUID parent;
    // What is in the seat
    private UUID passenger;
    // If we should try to render the rider as standing or sitting (partial support!)
    boolean shouldSit = true;
    // If a passenger has mounted and then dismounted (if so, we can go away)
    private boolean hasHadPassenger = false;
    // ticks alive?
    private int ticks = 0;

    /** MC reflection */
    public SeatEntity(EntityType<SeatEntity> type, net.minecraft.world.World worldIn) {
        super(type, worldIn);
    }

    @Override
    protected void readCustomDataFromTag(CompoundTag compound) {
        TagCompound data = new TagCompound(compound);
        parent = data.getUUID("parent");
        passenger = data.getUUID("passenger");
        shouldSit = data.getBoolean("shouldSit");
    }

    @Override
    protected void writeCustomDataToTag(CompoundTag compound) {
        TagCompound data = new TagCompound(compound);
        data.setUUID("parent", parent);
        data.setUUID("passenger", passenger);
        data.setBoolean("shouldSit", shouldSit);
    }

    @Override
    protected void initDataTracker() {

    }

    @Override
    public void tick() {
        ticks ++;
        if (world.isClient || ticks < 5) {
            return;
        }

        if (parent == null) {
            ModCore.debug("No parent, goodbye");
            this.remove();
            return;
        }
        if (passenger == null) {
            ModCore.debug("No passenger, goodbye");
            this.remove();
            return;
        }

        if (getPassengerList().isEmpty()) {
            if (this.ticks < 20) {
                if (!hasHadPassenger) {
                    cam72cam.mod.entity.Entity toRide = World.get(world).getEntity(passenger, cam72cam.mod.entity.Entity.class);
                    if (toRide != null) {
                        ModCore.debug("FORCE RIDER");
                        toRide.internal.startRiding(this, true);
                        hasHadPassenger = true;
                    }
                }
            } else {
                ModCore.debug("No passengers, goodbye");
                this.remove();
                return;
            }
        }

        if (getParent() == null) {
            if (ticks > 20) {
                ModCore.debug("No parent found, goodbye");
                this.remove();
            }
        }
    }

    public void setup(ModdedEntity moddedEntity, Entity passenger) {
        this.parent = moddedEntity.getUuid();
        this.updatePosition(moddedEntity.x, moddedEntity.y, moddedEntity.z);
        this.passenger = passenger.getUuid();
    }

    public void moveTo(ModdedEntity moddedEntity) {
        this.parent = moddedEntity.getUuid();
    }

    public cam72cam.mod.entity.Entity getParent() {
        cam72cam.mod.entity.Entity linked = World.get(world).getEntity(parent, cam72cam.mod.entity.Entity.class);
        if (linked != null && linked.internal instanceof ModdedEntity) {
            return linked;
        }
        return null;
    }

    @Override
    public double getMountedHeightOffset() {
        return 0;
    }

    @Override
    public final void updatePassengerPosition(net.minecraft.entity.Entity passenger) {
        cam72cam.mod.entity.Entity linked = World.get(world).getEntity(parent, cam72cam.mod.entity.Entity.class);
        if (linked != null && linked.internal instanceof ModdedEntity) {
            ((ModdedEntity) linked.internal).updateSeat(this);
        }
    }

    /* TODO LivingEntityRender.method_4054
    @Override
    public boolean shouldRiderSit() {
        return shouldSit;
    }
    */

    @Override
    public final void removePassenger(net.minecraft.entity.Entity passenger) {
        cam72cam.mod.entity.Entity linked = World.get(world).getEntity(parent, cam72cam.mod.entity.Entity.class);
        if (linked != null && linked.internal instanceof ModdedEntity) {
            ((ModdedEntity) linked.internal).removeSeat(this);
        }
        super.removePassenger(passenger);
    }

    public cam72cam.mod.entity.Entity getEntityPassenger() {
        if (this.removed) {
            return null;
        }
        if (this.getPassengerList().size() == 0) {
            return null;
        }
        return World.get(world).getEntity(getPassengerList().get(0));
    }


    @Override
    public Packet<?> createSpawnPacket() {
        return new CustomSpawnPacket(this).toPacket();
    }

    @Override
    public final void writeSpawnData(TagCompound data) {
        data.setUUID("parent", parent);
        data.setUUID("passenger", passenger);
    }

    @Override
    public final void readSpawnData(TagCompound data, float yaw, float pitch) {
        parent = data.getUUID("parent");
        this.setRotation(yaw, pitch);
        passenger = data.getUUID("passenger");
    }
}
