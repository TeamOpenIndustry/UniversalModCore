package cam72cam.mod.entity;

import cam72cam.mod.ModCore;
import cam72cam.mod.util.TagCompound;
import cam72cam.mod.world.World;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Packet;
import net.minecraft.util.Identifier;

import java.util.UUID;

public class SeatEntity extends Entity implements IAdditionalSpawnData {
    public static EntityType<SeatEntity> TYPE;
    static final Identifier ID = new Identifier(ModCore.MODID, "seat");
    private UUID parent;
    private int ticksUnsure = 0;
    boolean shouldSit = true;

    public SeatEntity(EntityType<SeatEntity> type, net.minecraft.world.World worldIn) {
        super(type, worldIn);
    }

    @Override
    protected void readCustomDataFromTag(CompoundTag compound) {
        TagCompound data = new TagCompound(compound);
        parent = data.getUUID("parent");
        shouldSit = data.getBoolean("shouldSit");
    }

    @Override
    protected void writeCustomDataToTag(CompoundTag compound) {
        TagCompound data = new TagCompound(compound);
        data.setUUID("parent", parent);
        data.setBoolean("shouldSit", shouldSit);
    }

    @Override
    protected void initDataTracker() {

    }

    @Override
    public void tick() {
        if (parent == null) {
            System.out.println("No parent, goodbye");
            this.remove();
            return;
        }
        if (getPassengerList().isEmpty()) {
            System.out.println("No passengers, goodbye");
            this.remove();
            return;
        }
        if (ticksUnsure > 10) {
            System.out.println("Parent not loaded, goodbye");
            this.remove();
            return;
        }

        cam72cam.mod.entity.Entity linked = World.get(world).getEntity(parent, cam72cam.mod.entity.Entity.class);
        if (linked != null && linked.internal instanceof ModdedEntity) {
            ticksUnsure = 0;
        } else {
            ticksUnsure++;
        }
    }

    public void setParent(ModdedEntity moddedEntity) {
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

    @Override
    public Packet<?> createSpawnPacket() {
        return new CustomSpawnPacket(this).toPacket();
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
    public final void writeSpawnData(TagCompound data) {
        data.setUUID("parent", parent);
    }

    @Override
    public final void readSpawnData(TagCompound data, float yaw, float pitch) {
        parent = data.getUUID("parent");
        this.setRotation(yaw, pitch);
    }
}
