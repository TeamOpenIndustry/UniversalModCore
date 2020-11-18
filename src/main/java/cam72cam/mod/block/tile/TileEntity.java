package cam72cam.mod.block.tile;

import cam72cam.mod.ModCore;
import cam72cam.mod.block.BlockEntity;
import cam72cam.mod.energy.IEnergy;
import cam72cam.mod.entity.boundingbox.BoundingBox;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.fluid.Fluid;
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
import com.google.common.collect.HashBiMap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.function.Supplier;

/**
 * TileEntity is an internal class that should only be extended when you need to implement
 * an interface.
 *
 * If you need to create a standard tile entity and wound up here, take a look at BlockEntity instead.
 *
 * @see BlockEntity
 */
public class TileEntity extends net.minecraft.tileentity.TileEntity {
    static {
        registerTileEntity(TileEntity.class, new Identifier(ModCore.MODID, "hack"));
    }

    // InstanceId -> Supplier mapping
    private static final Map<String, Supplier<BlockEntity>> registry = HashBiMap.create();

    // Set before initialization, used to lookup supplier
    private String instanceId;
    // Set during initialization
    private BlockEntity instance;

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
        ResourceLocation currentName = TileEntity.getKey(cls);
        if (currentName != null) {
            if (!currentName.toString().equals(name.toString())) {
                throw new RuntimeException(String.format("Duplicate TileEntity registration with different name: %s : %s", currentName, name));
            } else {
                throw new RuntimeException(String.format("TileEntity %s has already been registered", name));
            }
        }
        net.minecraft.tileentity.TileEntity.register(name.toString(), cls);
    }

    /** Wrap getPos() in a cached UMC Vec3i */
    public Vec3i getUMCPos() {
        if (umcPos == null || !umcPos.internal().equals(pos)) {
            umcPos = new Vec3i(pos);
        }
        return umcPos;
    }

    /** Wrap getWorld in cached UMC World */
    public World getUMCWorld() {
        if (umcWorld == null || umcWorld.internal != world) {
            umcWorld = World.get(world);
        }
        return umcWorld;
    }

    /**
     * So this get's called before readFromNBT and allows us to get a world object
     *
     * This allows us to set the world object and actually be in a position to load the instance
     */
    @Override
    protected void setWorldCreate(net.minecraft.world.World worldIn) {
        super.world = worldIn;
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
        }
    }

    /**
     * Serialize into the instance and call the save method (explicit save)
     * @see TagSerializer
     */
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
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
        return compound;
    }

    /** Active Synchronization from markDirty */
    @Override
    public final SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(this.getPos(), 1, getUpdateTag());
    }

    /** Active Synchronization from markDirty */
    @Override
    public final void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        handleUpdateTag(pkt.getNbtCompound());
    }

    /** Active Synchronization from markDirty */
    @Override
    public final NBTTagCompound getUpdateTag() {
        NBTTagCompound tag = super.getUpdateTag();
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
    @Override
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
        world.markBlockRangeForRenderUpdate(getPos(), getPos());
    }

    /** Fire off update packet if on server, re-render if on client */
    @Override
    public void markDirty() {
        super.markDirty();
        if (!world.isRemote) {
            world.notifyBlockUpdate(getPos(), world.getBlockState(getPos()), world.getBlockState(getPos()), 1 + 2 + 8);
            world.notifyNeighborsOfStateChange(pos, this.getBlockType(), true);
        }
    }

    /* Forge Overrides */

    private final SingleCache<IBoundingBox, AxisAlignedBB> bbCache =
            new SingleCache<>(internal -> BoundingBox.from(internal).offset(pos.getX(), pos.getY(), pos.getZ()));
    /**
     * @return Instance's bounding box
     * @see BlockEntity
     */
    @Override
    public net.minecraft.util.math.AxisAlignedBB getRenderBoundingBox() {
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

    @Override
    public boolean hasCapability(net.minecraftforge.common.capabilities.Capability<?> capability, @Nullable net.minecraft.util.EnumFacing facing) {
        //TODO more efficient
        return getCapability(capability, facing) != null;
    }

    @Override
    @Nullable
    public <T> T getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable net.minecraft.util.EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            ITank target = getTank(Facing.from(facing));
            if (target == null) {
                return null;
            }
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(new IFluidHandler() {
                @Override
                public IFluidTankProperties[] getTankProperties() {
                    return new IFluidTankProperties[]{
                            new IFluidTankProperties() {
                                @Nullable
                                @Override
                                public FluidStack getContents() {
                                    return target.getContents().internal;
                                }

                                @Override
                                public int getCapacity() {
                                    return target.getCapacity();
                                }

                                @Override
                                public boolean canFill() {
                                    return true;
                                }

                                @Override
                                public boolean canDrain() {
                                    return true;
                                }

                                @Override
                                public boolean canFillFluidType(FluidStack fluidStack) {
                                    return target.allows(Fluid.getFluid(fluidStack.getFluid()));
                                }

                                @Override
                                public boolean canDrainFluidType(FluidStack fluidStack) {
                                    return target.allows(Fluid.getFluid(fluidStack.getFluid()));
                                }
                            }
                    };
                }

                @Override
                public int fill(FluidStack resource, boolean doFill) {
                    int res = target.fill(new cam72cam.mod.fluid.FluidStack(resource), !doFill);
                    return res;
                }

                @Nullable
                @Override
                public FluidStack drain(FluidStack resource, boolean doDrain) {
                    return target.drain(new cam72cam.mod.fluid.FluidStack(resource), !doDrain).internal;
                }

                @Nullable
                @Override
                public FluidStack drain(int maxDrain, boolean doDrain) {
                    if (target.getContents().internal == null) {
                        return null;
                    }
                    return target.drain(new cam72cam.mod.fluid.FluidStack(new FluidStack(target.getContents().internal, maxDrain)), !doDrain).internal;
                }
            });
        }
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            IInventory target = getInventory(Facing.from(facing));
            if (target == null) {
                return null;
            }
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(new IItemHandlerModifiable() {
                @Override
                public int getSlots() {
                    return target.getSlotCount();
                }

                @Override
                public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
                    target.set(slot, new cam72cam.mod.item.ItemStack(stack));
                }

                @Nonnull
                @Override
                public ItemStack getStackInSlot(int slot) {
                    return target.get(slot).internal;
                }

                @Nonnull
                @Override
                public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
                    return target.insert(slot, new cam72cam.mod.item.ItemStack(stack), simulate).internal;
                }

                @Nonnull
                @Override
                public ItemStack extractItem(int slot, int amount, boolean simulate) {
                    return target.extract(slot, amount, simulate).internal;
                }

                @Override
                public int getSlotLimit(int slot) {
                    return target.getLimit(slot);
                }
            });
        }
        if (capability == CapabilityEnergy.ENERGY) {
            IEnergy target = getEnergy(Facing.from(facing));
            if (target == null) {
                return null;
            }
            return CapabilityEnergy.ENERGY.cast(new IEnergyStorage() {
                @Override
                public int receiveEnergy(int maxReceive, boolean simulate) {
                    return target.receive(maxReceive, simulate);
                }

                @Override
                public int extractEnergy(int maxExtract, boolean simulate) {
                    return target.extract(maxExtract, simulate);
                }

                @Override
                public int getEnergyStored() {
                    return target.getCurrent();
                }

                @Override
                public int getMaxEnergyStored() {
                    return target.getMax();
                }

                @Override
                public boolean canExtract() {
                    return true;
                }

                @Override
                public boolean canReceive() {
                    return true;
                }
            });
        }
        return null;
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
        return instance(null);
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
            if (hasWorld() && (!world.isRemote || data != null)) {
                if (this.instanceId == null) {
                    ModCore.debug("WAT NULL");
                }
                if (!registry.containsKey(instanceId)) {
                    ModCore.debug("WAT " + instanceId);
                }
                this.instance = registry.get(this.instanceId).get();
                this.instance.internal = this;
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
}
