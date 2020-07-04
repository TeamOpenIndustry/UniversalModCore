package cam72cam.mod.entity;

import cam72cam.mod.ModCore;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.world.World;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import java.util.UUID;

public class SeatEntity extends Entity implements IEntityAdditionalSpawnData {
    static final ResourceLocation ID = new ResourceLocation(ModCore.MODID, "seat");
    private UUID parent;
    private UUID passenger;
    boolean shouldSit = true;
    private boolean hasHadPassenger = false;
    private int ticks = 0;

    public SeatEntity(net.minecraft.world.World worldIn) {
        super(worldIn);
    }

    @Override
    protected void entityInit() {

    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        TagCompound data = new TagCompound(compound);
        parent = data.getUUID("parent");
        passenger = data.getUUID("passenger");
        shouldSit = data.getBoolean("shouldSit");
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
        TagCompound data = new TagCompound(compound);
        data.setUUID("parent", parent);
        data.setUUID("passenger", passenger);
        data.setBoolean("shouldSit", shouldSit);
    }

    @Override
    public void onEntityUpdate() {
        ticks ++;
        if (ticks < 5) {
            return;
        }

        if (parent == null) {
            ModCore.debug("No parent, goodbye");
            this.setDead();
            return;
        }
        if (passenger == null) {
            ModCore.debug("No passenger, goodbye");
            this.setDead();
            return;
        }

        if (riddenByEntity == null) {
            if (this.ticks < 20) {
                if (!hasHadPassenger) {
                    cam72cam.mod.entity.Entity toRide = World.get(worldObj).getEntity(passenger, cam72cam.mod.entity.Entity.class);
                    if (toRide != null) {
                        ModCore.debug("FORCE RIDER");
                        toRide.internal.mountEntity(this);
                        hasHadPassenger = true;
                    }
                }
            } else {
                cam72cam.mod.entity.Entity toRide = World.get(worldObj).getEntity(passenger, cam72cam.mod.entity.Entity.class);
                if (toRide != null && !(toRide.internal.ridingEntity instanceof SeatEntity)) {
                    removePassenger();
                }
                ModCore.debug("No passengers, goodbye");
                this.setDead();
                return;
            }
        }

        if (getParent() == null) {
            if (ticks > 20) {
                ModCore.debug("No parent found, goodbye");
                this.setDead();
            }
        }
    }

    public void setup(ModdedEntity moddedEntity, Entity passenger) {
        this.parent = moddedEntity.getUniqueID();
        this.setPosition(moddedEntity.posX, moddedEntity.posY, moddedEntity.posZ);
        this.passenger = passenger.getUniqueID();
    }

    public cam72cam.mod.entity.Entity getParent() {
        cam72cam.mod.entity.Entity linked = World.get(worldObj).getEntity(parent, cam72cam.mod.entity.Entity.class);
        if (linked != null && linked.internal instanceof ModdedEntity) {
            return linked;
        }
        return null;
    }

    @Override
    public double getMountedYOffset() {
        return 0;
    }

    @Override
    public void updateRiderPosition() {
        cam72cam.mod.entity.Entity linked = World.get(worldObj).getEntity(parent, cam72cam.mod.entity.Entity.class);
        if (linked != null && linked.internal instanceof ModdedEntity) {
            ((ModdedEntity) linked.internal).updateSeat(this);
        }
    }

    @Override
    public boolean shouldRiderSit() {
        return shouldSit;
    }

    private final void removePassenger() {
        cam72cam.mod.entity.Entity linked = World.get(worldObj).getEntity(parent, cam72cam.mod.entity.Entity.class);
        if (linked != null && linked.internal instanceof ModdedEntity) {
            ((ModdedEntity) linked.internal).removeSeat(this);
        }
    }

    public cam72cam.mod.entity.Entity getEntityPassenger() {
        if (this.isDead) {
            return null;
        }
        if (riddenByEntity == null) {
            if (passenger != null) {
                ModCore.debug("FALLBACK UNMOUNT");
                return World.get(worldObj).getEntity(passenger, cam72cam.mod.entity.Entity.class);
            }
            return null;
        }
        return World.get(worldObj).getEntity(riddenByEntity);
    }

    @Override
    public void writeSpawnData(ByteBuf buffer) {
        TagCompound data = new TagCompound();
        data.setUUID("parent", parent);
        data.setUUID("passenger", passenger);
        ByteBufUtils.writeTag(buffer, data.internal);
    }

    @Override
    public void readSpawnData(ByteBuf additionalData) {
        TagCompound data = new TagCompound(ByteBufUtils.readTag(additionalData));
        parent = data.getUUID("parent");
        passenger = data.getUUID("passenger");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean isInRangeToRenderDist(double distance) {
        return true;
    }
}
