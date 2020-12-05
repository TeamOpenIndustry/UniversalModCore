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
import cam72cam.mod.serialization.SerializationException;
import cam72cam.mod.serialization.TagSerializer;
import cam72cam.mod.util.Facing;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.util.SingleCache;
import cam72cam.mod.world.World;
import cofh.api.energy.IEnergyConnection;
import cofh.api.energy.IEnergyHandler;
import com.google.common.collect.HashBiMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * TileEntity is an internal class that should only be extended when you need to implement
 * an interface.
 *
 * If you need to create a standard tile entity and wound up here, take a look at BlockEntity instead.
 *
 * @see BlockEntity
 */
public class TileEntity extends net.minecraft.tileentity.TileEntity implements IEnergyHandler, IEnergyConnection, IFluidHandler, ISidedInventory {
    static {
        registerTileEntity(TileEntity.class, new Identifier(ModCore.MODID, "hack"));
    }

    // InstanceId -> Supplier mapping
    private static final Map<String, Supplier<BlockEntity>> registry = HashBiMap.create();

    // Set before initialization, used to lookup supplier
    private String instanceId;
    // Set during initialization
    private BlockEntity instance;
    public TagCompound deferredLoad;

    // Cached
    private Vec3i umcPos;
    private World umcWorld;

    /**
     * Used only be Forge's reflection.
     * <ul>
     *     <li>Must be implemented in subclasses.</li>
     *     <li>Do not use directly.</li>
     * </ul>
     */
    public TileEntity() {
        super();
    }

    /**
     * Used only by BlockEntity to construct an instance to register.
     * <ul>
     *     <li>Must be implemented in subclasses.</li>
     *     <li>Do not use directly.</li>
     * </ul>
     *
     * @see BlockEntity
     * @param id Block Entity ID
     */
    public TileEntity(Identifier id) {
        this();
        instanceId = id.toString();
    }

    /**
     * Allows us to construct a BlockEntity from an identifier.  Do not use directly.
     * @param instance constructor
     * @param id Block Entity ID
     */
    public static void register(Supplier<BlockEntity> instance, Identifier id) {
        registry.put(id.toString(), instance);
    }

    /**
     * Registers a tile entity class with forge.  Only use when implementing a new TileEntity directly.
     * @param cls TileEntity class to register
     * @param name Internal (forge) name
     */
    public static void registerTileEntity(Class<? extends TileEntity> cls, Identifier name) {
        try {
            TileEntity.addMapping(cls, name.toString());
        } catch (IllegalArgumentException ex) {
            //pass
        }
    }

    /** Wrap getPos() in a cached UMC Vec3i */
    public Vec3i getUMCPos() {
        if (umcPos == null || umcPos.x != xCoord || umcPos.y != yCoord || umcPos.z != zCoord) {
            umcPos = new Vec3i(xCoord, yCoord, zCoord);
        }
        return umcPos;
    }

    /** Wrap getWorld in cached UMC World */
    public World getUMCWorld() {
        if (umcWorld == null || umcWorld.internal != worldObj) {
            umcWorld = World.get(worldObj);
        }
        return umcWorld;
    }

    /*
    Standard Tile function overrides
    */

    /**
     * Initialize instance (if possible), deserialize into the instance and call the load method (explicit load)
     * @see TagSerializer
     */
    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);

        TagCompound data = new TagCompound(compound);

        if (instanceId == null) {
            // If this fails something is really wrong
            instanceId = data.getString("instanceId");
            if (instanceId == null) {
                throw new RuntimeException("Unable to load instanceid with " + data.toString());
            }
        }

        TagCompound instanceData = data.get("instanceData");
        if (instanceData == null) {
            // Legacy fallback
            instanceData = data;
        }

        if (instance(instanceData) != null) {
            try {
                TagSerializer.deserialize(instanceData, instance());
                instance().load(instanceData);
            } catch (SerializationException e) {
                // TODO how should we handle this?
                throw new RuntimeException(e);
            }
        } else {
            deferredLoad = instanceData;
        }
    }

    /**
     * Serialize into the instance and call the save method (explicit save)
     * @see TagSerializer
     */
    @Override
    public void writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);

        TagCompound data = new TagCompound(compound);

        data.setString("instanceId", instanceId);
        if (instance() != null) {
            TagCompound instanceData = new TagCompound();
            try {
                TagSerializer.serialize(instanceData, instance());
                instance().save(instanceData);
            } catch (SerializationException e) {
                // TODO how should we handle this?
                throw new RuntimeException(e);
            }
            data.set("instanceData", instanceData);
        }
    }

    /** Active Synchronization from markDirty */
    @Override
    public Packet getDescriptionPacket() {
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 6, getUpdateTag());
    }

    /** Active Synchronization from markDirty */
    @Override
    public final void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        handleUpdateTag(pkt.getNbtCompound());
    }

    /** Active Synchronization from markDirty */
    // 1.7.10 @Override
    public final NBTTagCompound getUpdateTag() {
        NBTTagCompound tag = new NBTTagCompound();
        if (this.isLoaded()) {
            this.writeToNBT(tag);
            TagCompound umcUpdate = new TagCompound();
            try {
                instance().writeUpdate(umcUpdate);
            } catch (SerializationException e) {
                ModCore.catching(e);
            }
            tag.setTag("umcUpdate", umcUpdate.internal);
        }
        return tag;
    }

    /** Active Synchronization from markDirty */
    // 1.7.10 @Override
    public final void handleUpdateTag(NBTTagCompound tag) {
        try {
            this.readFromNBT(tag);
            if (instance(new TagCompound(tag)) != null) {
                try {
                    instance().readUpdate(new TagCompound(tag.getCompoundTag("umcUpdate")));
                } catch (SerializationException e) {
                    ModCore.catching(e);
                }
            }
        } catch (Exception ex) {
            ModCore.error("IN UPDATE: %s", tag);
            ModCore.catching(ex);
        }
        worldObj.markBlockRangeForRenderUpdate(xCoord, yCoord, zCoord,xCoord, yCoord, zCoord);
    }

    /** Fire off update packet if on server, re-render if on client */
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

    private final SingleCache<IBoundingBox, AxisAlignedBB> bbCache =
            new SingleCache<>(internal -> BoundingBox.from(internal).getOffsetBoundingBox(xCoord, yCoord, zCoord));
    /**
     * @return Instance's bounding box
     * @see BlockEntity
     */
    @Override
    public net.minecraft.util.AxisAlignedBB getRenderBoundingBox() {
        if (instance() != null) {
            return bbCache.get(instance().getRenderBoundingBox());
        }
        return INFINITE_EXTENT_AABB;
    }

    /**
     * @return Instance's render distance
     * @see BlockEntity
     */
    @Override
    public double getMaxRenderDistanceSquared() {
        return instance() != null ? instance().getRenderDistance() * instance().getRenderDistance() : Integer.MAX_VALUE;
    }

    /*
    New Functionality
    */

    /** @return If the BlockEntity instance is loaded */
    public boolean isLoaded() {
        return instance() != null;
    }

    /** @return The instance of the BlockEntity if possible */
    public BlockEntity instance() {
        return instance(deferredLoad);
    }

    /**
     * So this is a fun one...<br>
     * <br>
     * First we require the world object to have been set.  Without that we can't do much at all.<br>
     * Secondly we need to see if we are on the client or server.  Server side is easy, if a TE has been created
     * server side, it's no problem to load the instance as there's no waiting for packets.  Client side is a bit more
     * interesting.  There's a few paths into it, but the idea is that we can only load the TE once the client has
     * received a copy of the tile data from the server.<br>
     *<br>
     * If there are bugs in loading / synchronizing / faking TE's look here first...
     *
     * @param data
     * @return The instance of the BlockEntity if possible
     */
    private BlockEntity instance(TagCompound data) {
        if (this.instance == null) {
            if (hasWorldObj() && (!worldObj.isRemote || data != null)) {
                if (this.instanceId == null) {
                    ModCore.debug("WAT NULL");
                }
                if (!registry.containsKey(instanceId)) {
                    ModCore.debug("WAT " + instanceId);
                }
                this.instance = registry.get(this.instanceId).get();
                this.instance.internal = this;
                this.deferredLoad = null;
                if (data != null) {
                    try {
                        TagSerializer.deserialize(data, this.instance);
                        this.instance.load(data);
                    } catch (SerializationException e) {
                        throw new RuntimeException(e);
                    }
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
    public int[] getSlotsForFace(int side) {
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
    public void openChest() {

    }

    @Override
    public void closeChest() {

    }

    @Override
    public int getSizeInventory() {
        return getInventory(null) == null ? 0 : getInventory(null).getSlotCount();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
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
            inv.set(slot, new cam72cam.mod.item.ItemStack(stack));
        }
    }

    @Override
    public String getInventoryName() {
        return "";
    }

    @Override
    public boolean isCustomInventoryName() {
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
        return target.drain(new cam72cam.mod.fluid.FluidStack(new FluidStack(target.getContents().internal, maxDrain)), !doDrain).internal;
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
