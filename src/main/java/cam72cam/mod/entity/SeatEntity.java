package cam72cam.mod.entity;

import cam72cam.mod.ModCore;
import cam72cam.mod.util.TagCompound;
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
    public UUID delayedRider;
    private Integer parent;
    private UUID rider;
    private int ticksUnsure = 0;
    boolean shouldSit = true;

    public SeatEntity(net.minecraft.world.World worldIn) {
        super(worldIn);
    }

    @Override
    protected void entityInit() {

    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        TagCompound data = new TagCompound(compound);
        parent = data.getInteger("parent");
        shouldSit = data.getBoolean("shouldSit");
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
        TagCompound data = new TagCompound(compound);
        data.setInteger("parent", parent);
        data.setBoolean("shouldSit", shouldSit);
    }

    @Override
    public void onEntityUpdate() {
        if (this.ticksExisted < 5) {
            return;
        }

        if (this.riddenByEntity != null) {
            delayedRider = null;
        }
        if (delayedRider != null) {
            cam72cam.mod.entity.Entity r = World.get(worldObj).getEntity(delayedRider, cam72cam.mod.entity.Entity.class);
            if (r != null) {
                r.internal.mountEntity(this);
                System.out.println("YeHaw");
                delayedRider = null;
            }
            return;
        }

        if (parent == null) {
            System.out.println("No parent, goodbye");
            removePassenger();
            this.setDead();
            return;
        }
        if (riddenByEntity == null) {
            System.out.println("No passengers, goodbye");
            removePassenger();
            this.setDead();
            return;
        }
        rider = riddenByEntity.getUniqueID();

        if (ticksUnsure > 10) {
            System.out.println("Parent not loaded, goodbye");
            this.setDead();
            return;
        }

        cam72cam.mod.entity.Entity linked = World.get(worldObj).getEntity(parent, cam72cam.mod.entity.Entity.class);
        if (linked != null && linked.internal instanceof ModdedEntity) {
            ticksUnsure = 0;
        } else {
            ticksUnsure++;
        }
    }

    public void setParent(ModdedEntity moddedEntity) {
        this.parent = moddedEntity.getEntityId();
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
            if (rider != null) {
                System.out.println("FALLBACK UNMOUNT");
                return World.get(worldObj).getEntity(rider, cam72cam.mod.entity.Entity.class);
            }
            return null;
        }
        return World.get(worldObj).getEntity(riddenByEntity);
    }

    @Override
    public void writeSpawnData(ByteBuf buffer) {
        TagCompound data = new TagCompound();
        data.setInteger("parent", parent);
        data.setUUID("delayedRider", delayedRider);
        ByteBufUtils.writeTag(buffer, data.internal);
    }

    @Override
    public void readSpawnData(ByteBuf additionalData) {
        TagCompound data = new TagCompound(ByteBufUtils.readTag(additionalData));
        parent = data.getInteger("parent");
        delayedRider = data.getUUID("delayedRider");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean isInRangeToRenderDist(double distance) {
        return true;
    }
}
