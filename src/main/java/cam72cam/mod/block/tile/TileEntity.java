package cam72cam.mod.block.tile;

import cam72cam.mod.ModCore;
import cam72cam.mod.block.BlockEntity;
import cam72cam.mod.block.BlockTypeEntity;
import cam72cam.mod.energy.IEnergy;
import cam72cam.mod.entity.boundingbox.BoundingBox;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.fluid.Fluid;
import cam72cam.mod.fluid.ITank;
import cam72cam.mod.item.IInventory;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.serialization.SerializationException;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.TagSerializer;
import cam72cam.mod.util.Facing;
import cam72cam.mod.util.SingleCache;
import cam72cam.mod.world.World;
import com.google.common.collect.HashBiMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
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
    private static final Map<String, TileEntityType<? extends TileEntity>> types = HashBiMap.create();
    // InstanceId -> Supplier mapping
    private static final Map<String, Supplier<BlockEntity>> registry = HashBiMap.create();
    private static final Map<String, BlockTypeEntity> blocks = HashBiMap.create();

    // Set during initialization
    private final BlockEntity instance;
    public boolean hasTileData;

    // Cached
    private Vec3i umcPos;
    private World umcWorld;

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
        super(types.get(id.toString()));
        instance = registry.get(id.toString()).get();
        instance.internal = this;
    }

    public static TagCompound legacyConverter(TagCompound data) {
        for (Function<CompoundNBT, BlockState> migration : migrations) {
            if (migration.apply(data.internal) != null) {
                break;
            }
        }
        return data;
    }

    private static final List<Function<CompoundNBT, BlockState>> migrations = new ArrayList<>();
    public static void registerLegacyTE(Identifier legacyID) {
        registerLegacyTE(legacyID, null);
    }
    public static void registerLegacyTE(Identifier legacyID, BlockState state) {
        migrations.add(data -> {
            if (data != null && legacyID.toString().equals(data.getString("id"))) {
                BlockState myState = state;
                if (myState == null) {
                    myState = blocks.get(data.getString("instanceId")).internal.getDefaultState();
                }
                data.putString("id", myState.getBlock().createTileEntity(null, null).getType().getRegistryName().toString());
                return myState;
            }
            return null;
        });
    }
    static {
        registerLegacyTE(new Identifier(ModCore.MODID, "hack"));

        CommonEvents.World.LOAD_CHUNK.subscribe(chunk -> {
            if(chunk instanceof Chunk) {
                return;
            }
            for (BlockPos pos : chunk.getTileEntitiesPos()) {
                CompoundNBT data = chunk.getDeferredTileEntity(pos);
                for (Function<CompoundNBT, BlockState> migration : migrations) {
                    BlockState state = migration.apply(data);
                    if (state != null) {
                        // Usually this is a ChunkPrimer which does not propagate changes
                        chunk.setBlockState(pos, state, false);
                        break;
                    }
                }
            }
        });
    }

    /**
     * Allows us to construct a BlockEntity from an identifier.  Do not use directly.
     * @param instance constructor
     * @param id Block Entity ID
     */
    public static void register(Supplier<BlockEntity> instance, Identifier id, BlockTypeEntity blockType) {
        registry.put(id.toString(), instance);
        blocks.put(id.toString(), blockType);

        BlockEntity example = instance.get();

        // Force legacy registration
        example.supplier(id);

        CommonEvents.Tile.REGISTER.subscribe(() -> {
            TileEntityType<TileEntity> type = new TileEntityType<>(() -> example.supplier(id), new HashSet<Block>() {
                public boolean contains(Object var1) {
                    // WHYYYYYYYYYYYYYYYY
                    return true;
                }
            }, null);
            type.setRegistryName(id.internal);
            types.put(id.toString(), type);
            ForgeRegistries.TILE_ENTITIES.register(type);
        });
    }

    public static TileEntityType<TileEntity> getType(Identifier type) {
        return (TileEntityType<TileEntity>) types.get(type.toString());
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

    /*
    Standard Tile function overrides
    */

    /**
     * Initialize instance (if possible), deserialize into the instance and call the load method (explicit load)
     * @see TagSerializer
     */
    @Override
    public void read(CompoundNBT compound) {
        super.read(compound);
        hasTileData = true;

        TagCompound data = new TagCompound(compound);
        TagCompound instanceData = data.get("instanceData");
        if (instanceData == null) {
            // Legacy fallback
            instanceData = data;
        }

        try {
            TagSerializer.deserialize(instanceData, instance);
            instance.load(instanceData);
        } catch (SerializationException e) {
            // TODO how should we handle this?
            throw new RuntimeException(e);
        }
    }

    /**
     * Serialize into the instance and call the save method (explicit save)
     * @see TagSerializer
     */
    @Override
    public CompoundNBT write(CompoundNBT compound) {
        super.write(compound);

        TagCompound data = new TagCompound(compound);

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
    public final SUpdateTileEntityPacket getUpdatePacket() {
        return new SUpdateTileEntityPacket(this.getPos(), 1, getUpdateTag(true));
    }

    /** Active Synchronization from markDirty */
    @Override
    public final void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
        handleUpdateTag(pkt.getNbtCompound());
    }

    /** Active Synchronization from markDirty */
    @Override
    public final CompoundNBT getUpdateTag() {
        return getUpdateTag(false);
    }
    public final CompoundNBT getUpdateTag(boolean writeUpdate) {
        CompoundNBT tag = super.getUpdateTag();
        if (this.isLoaded()) {
            this.write(tag);
            TagCompound umcUpdate = new TagCompound();
            if (writeUpdate) {
                try {
                    instance().writeUpdate(umcUpdate);
                } catch (SerializationException e) {
                    ModCore.catching(e);
                }
            }
            tag.put("umcUpdate", umcUpdate.internal);
        }
        return tag;
    }

    /** Active Synchronization from markDirty */
    @Override
    public final void handleUpdateTag(CompoundNBT tag) {
        try {
            this.read(tag);
            if (instance() != null) {
                if (tag.contains("umcUpdate")) {
                    try {
                        instance().readUpdate(new TagCompound(tag.getCompound("umcUpdate")));
                    } catch (SerializationException e) {
                        ModCore.catching(e);
                    }
                }
            }
        } catch (Exception ex) {
            ModCore.error("IN UPDATE: %s", tag);
            ModCore.catching(ex);
        }
        world.notifyBlockUpdate(super.pos, null, super.world.getBlockState(super.pos), 3);
    }

    /** Fire off update packet if on server, re-render if on client */
    @Override
    public void markDirty() {
        super.markDirty();
        if (!world.isRemote) {
            world.notifyBlockUpdate(getPos(), world.getBlockState(getPos()), world.getBlockState(getPos()), 1 + 2 + 8);
            world.notifyNeighborsOfStateChange(pos, world.getBlockState(getPos()).getBlock());
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
    @Nullable
    public <T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable net.minecraft.util.Direction facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            ITank target = getTank(Facing.from(facing));
            if (target == null) {
                return LazyOptional.empty();
            }

            return LazyOptional.of(() -> new IFluidHandler() {
                @Override
                public int getTanks() {
                    return 1;
                }

                @Nonnull
                @Override
                public FluidStack getFluidInTank(int tank) {
                    return target.getContents().internal;
                }

                @Override
                public int getTankCapacity(int tank) {
                    return target.getCapacity();
                }

                @Override
                public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
                    return target.allows(Fluid.getFluid(stack.getFluid()));
                }

                @Override
                public int fill(FluidStack resource, FluidAction action) {
                    return target.fill(new cam72cam.mod.fluid.FluidStack(resource), action.simulate());
                }

                @Nonnull
                @Override
                public FluidStack drain(FluidStack resource, FluidAction action) {
                    return target.drain(new cam72cam.mod.fluid.FluidStack(resource), action.simulate()).internal;
                }

                @Nonnull
                @Override
                public FluidStack drain(int maxDrain, FluidAction action) {
                    if (target.getContents().internal.isEmpty()) {
                        return FluidStack.EMPTY;
                    }
                    return target.drain(new cam72cam.mod.fluid.FluidStack(new FluidStack(target.getContents().internal, maxDrain)), action.simulate()).internal;
                }
            }).cast();
        }
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            IInventory target = getInventory(Facing.from(facing));
            if (target == null) {
                return LazyOptional.empty();
            }
            return LazyOptional.of(() -> new IItemHandlerModifiable() {
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

                @Override
                public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
                    return true; //TODO 1.14.4
                }
            }).cast();
        }
        if (capability == CapabilityEnergy.ENERGY) {
            IEnergy target = getEnergy(Facing.from(facing));
            if (target == null) {
                return LazyOptional.empty();
            }
            return LazyOptional.of(() -> new IEnergyStorage() {
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
            }).cast();
        }
        return LazyOptional.empty();
    }

    /*
    New Functionality
    */

    /** @return If the BlockEntity instance is loaded */
    public boolean isLoaded() {
        return world != null && (!world.isRemote || hasTileData);
    }

    /** @return The instance of the BlockEntity if possible */
    public BlockEntity instance() {
        return isLoaded() ? this.instance : null;
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

    /* Render */
    public static ModelProperty<TileEntity> TE_PROPERTY = new ModelProperty<>();
    public final IModelData getModelData() {
        return new ModelDataMap.Builder().withInitial(TE_PROPERTY, this).build();
    }
}
