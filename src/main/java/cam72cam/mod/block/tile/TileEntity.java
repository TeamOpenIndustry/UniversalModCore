package cam72cam.mod.block.tile;

import cam72cam.mod.ModCore;
import cam72cam.mod.block.BlockEntity;
import cam72cam.mod.energy.IEnergy;
import cam72cam.mod.entity.boundingbox.BoundingBox;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.fluid.ITank;
import cam72cam.mod.item.IInventory;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.util.Facing;
import cam72cam.mod.util.TagCompound;
import cam72cam.mod.world.World;
import cofh.api.energy.IEnergyConnection;
import cofh.api.energy.IEnergyHandler;
import cofh.api.energy.IEnergyStorage;
import com.google.common.collect.HashBiMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public class TileEntity extends net.minecraft.tileentity.TileEntity implements IEnergyHandler, IEnergyConnection, IFluidHandler, ISidedInventory {
    private static final Map<String, Supplier<BlockEntity>> registry = HashBiMap.create();
    public boolean hasTileData;
    private String instanceId;

    /*
    Tile registration
    */
    private BlockEntity instance;
    private TagCompound deferredLoad;

    public TileEntity() {
        // Forge reflection
        super();
    }

    public TileEntity(Identifier id) {
        this();
        instanceId = id.toString();
    }

    public static void register(Supplier<BlockEntity> instance, Identifier id) {
        registry.put(id.toString(), instance);
    }

    public final void register() {
        try {
            TileEntity.addMapping(this.getClass(), this.getName().internal.toString());
        } catch (IllegalArgumentException ex) {
            //pass
        }
    }

    public Identifier getName() {
        return new Identifier(ModCore.MODID, "hack");
    }


    /*
    Standard Tile function overrides
    */

    @Override
    public final void readFromNBT(NBTTagCompound compound) {
        hasTileData = true;
        load(new TagCompound(compound));
    }

    @Override
    public final void writeToNBT(NBTTagCompound compound) {
        save(new TagCompound(compound));
    }

    @Override
    public Packet getDescriptionPacket() {
        TagCompound nbt = new TagCompound();
        this.writeToNBT(nbt.internal);
        this.writeUpdate(nbt);

        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 6, nbt.internal);
    }

    @Override
    public final void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        hasTileData = true;
        this.readFromNBT(pkt.func_148857_g());
        this.readUpdate(new TagCompound(pkt.func_148857_g()));
        super.onDataPacket(net, pkt);
        worldObj.markBlockRangeForRenderUpdate(xCoord, yCoord, zCoord,xCoord, yCoord, zCoord);
    }

    @Override
    public void markDirty() {
        super.markDirty();
        if (!worldObj.isRemote) {
            worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord, zCoord, blockType, 1 + 2 + 8);
            worldObj.notifyBlockChange(xCoord, yCoord, zCoord, this.getBlockType());
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    /* Forge Overrides */

    public net.minecraft.util.AxisAlignedBB getRenderBoundingBox() {
        if (instance() != null) {
            IBoundingBox bb = instance().getBoundingBox();
            if (bb != null) {
                return new BoundingBox(bb);
            }
        }
        return INFINITE_EXTENT_AABB;
    }

    public double getMaxRenderDistanceSquared() {
        return instance() != null ? instance().getRenderDistance() * instance().getRenderDistance() : Integer.MAX_VALUE;
    }

    /*
    Wrapped functionality
    */

    public void setWorld(World world) {
        super.setWorldObj(world.internal);
    }

    public void load(TagCompound data) {
        super.readFromNBT(data.internal);

        if (instanceId == null) {
            // If this fails something is really wrong
            instanceId = data.getString("instanceId");
            if (instanceId == null) {
                throw new RuntimeException("Unable to load instanceid with " + data.toString());
            }
        }

        if (instance() != null) {
            instance().load(data);
        } else {
            deferredLoad = data;
        }
    }

    public void save(TagCompound data) {
        super.writeToNBT(data.internal);
        data.setString("instanceId", instanceId);
        if (instance() != null) {
            instance().save(data);
        }
    }

    public void writeUpdate(TagCompound nbt) {
        if (instance() != null) {
            instance().writeUpdate(nbt);
        }
    }

    public void readUpdate(TagCompound nbt) {
        if (instance() != null) {
            instance().readUpdate(nbt);
        }
    }

    /*
    New Functionality
    */

    public boolean isLoaded() {
        return this.hasWorldObj() && (!worldObj.isRemote || hasTileData);
    }

    public BlockEntity instance() {
        if (this.instance == null) {
            if (isLoaded()) {
                if (this.instanceId == null) {
                    ModCore.debug("WAT NULL");
                }
                if (!registry.containsKey(instanceId)) {
                    ModCore.debug("WAT " + instanceId);
                }
                this.instance = registry.get(this.instanceId).get();
                this.instance.internal = this;
                this.instance.world = World.get(worldObj);
                this.instance.pos = new Vec3i(xCoord, yCoord, zCoord);
                if (deferredLoad != null) {
                    this.instance.load(deferredLoad);
                }
                this.deferredLoad = null;
                if (worldObj.isRemote) {
                    worldObj.notifyBlockChange(xCoord, yCoord, zCoord, this.getBlockType());
                    worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
                }
            }
        }
        return this.instance;
    }

    /* Capabilities */

    public IInventory getInventory(Facing side) {
        return instance() != null ? instance().getInventory(side) : null;
    }

    public ITank getTank(Facing side) {
        return instance() != null ? instance().getTank(side) : null;
    }

    public IEnergy getEnergy(Facing side) {
        return instance() != null ? instance().getEnergy(side) : null;
    }

    @Override
    public int receiveEnergy(ForgeDirection dir, int maxReceive, boolean simulate) {
        return getEnergy(Facing.from(dir)) == null ? 0 : getEnergy(Facing.from(dir)).receive(maxReceive, simulate);
    }

    @Override
    public int extractEnergy(ForgeDirection dir, int maxExtract, boolean simulate) {
        return getEnergy(Facing.from(dir)) == null ? 0 : getEnergy(Facing.from(dir)).extract(maxExtract, simulate);
    }

    @Override
    public int getEnergyStored(ForgeDirection dir) {
        return getEnergy(Facing.from(dir)) == null ? 0 : getEnergy(Facing.from(dir)).getCurrent();
    }

    @Override
    public int getMaxEnergyStored(ForgeDirection dir) {
        return getEnergy(Facing.from(dir)) == null ? 0 : getEnergy(Facing.from(dir)).getMax();
    }

    @Override
    public boolean canConnectEnergy(ForgeDirection dir) {
        return getEnergy(Facing.from(dir)) != null;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return IntStream.range(0, getInventory(Facing.from((byte) side)) == null ? 0 : getInventory(Facing.from((byte) side)).getSlotCount()).toArray();
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack, int side) {
        IInventory inv = getInventory(null);
        return inv != null && stack != null && inv.insert(slot, new cam72cam.mod.item.ItemStack(stack), true).getCount() != stack.stackSize;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int side) {
        IInventory inv = getInventory(null);
        return inv != null && stack != null && !inv.extract(slot, stack.stackSize, true).isEmpty();
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return true;
    }

    @Override
    public void openInventory() {

    }

    @Override
    public void closeInventory() {

    }

    @Override
    public int getSizeInventory() {
        return getInventory(null) == null ? 0 : getInventory(null).getSlotCount();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        //System.out.println("GET " + slot);
        return getInventory(null) == null ? null : getInventory(null).get(slot).internal;
    }

    @Override
    public ItemStack decrStackSize(int slot, int count) {
        IInventory inv = getInventory(null);
        return inv == null ? null : inv.extract(slot, count, false).internal;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int p_70304_1_) {
        return null;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        IInventory inv = getInventory(null);
        if (inv != null) {
            System.out.println("SET " + slot + " " + stack);
            inv.set(slot, new cam72cam.mod.item.ItemStack(stack));
        }
    }

    @Override
    public String getInventoryName() {
        return "";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        IInventory inv = getInventory(null);
        // remainder != input size
        return inv != null && stack != null && inv.insert(slot, new cam72cam.mod.item.ItemStack(stack), true).getCount() != stack.stackSize;
    }

    @Override
    public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
        return getTank(Facing.from(from)) == null ? 0 : getTank(Facing.from(from)).fill(new cam72cam.mod.fluid.FluidStack(resource), !doFill);
    }

    @Override
    public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
        return getTank(Facing.from(from)) == null ? null : getTank(Facing.from(from)).drain(new cam72cam.mod.fluid.FluidStack(resource), !doDrain).internal;
    }

    @Override
    public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
        ITank target = getTank(Facing.from(from));
        if (target == null) {
            return null;
        }
        if (target.getContents().internal == null) {
            return null;
        }
        return target.drain(new cam72cam.mod.fluid.FluidStack(new FluidStack(target.getContents().internal, maxDrain)), doDrain).internal;
    }

    @Override
    public boolean canFill(ForgeDirection from, Fluid fluid) {
        return getTank(Facing.from(from)) != null;
    }

    @Override
    public boolean canDrain(ForgeDirection from, Fluid fluid) {
        return getTank(Facing.from(from)) != null;
    }

    @Override
    public FluidTankInfo[] getTankInfo(ForgeDirection from) {
        ITank tank = getTank(Facing.from(from));
        return tank == null ? new FluidTankInfo[0] : new FluidTankInfo[]{ new FluidTankInfo(tank.getContents().internal, tank.getCapacity() ) };
    }
}
