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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
public class TileEntity extends net.minecraft.world.level.block.entity.BlockEntity {
    public static final AABB INFINITE_EXTENT_AABB = new net.minecraft.world.phys.AABB(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);

    private static final Map<String, BlockEntityType<? extends TileEntity>> types = HashBiMap.create();
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
        super(types.get(id.toString()), new BlockPos.MutableBlockPos() {
            @Override
            public BlockPos immutable() {
                return this; // BAHAHAHAHA
            }
        }, new StateDefinition.Builder<Block, BlockState>(BuiltInRegistries.BLOCK.get(id.internal))
                .create(Block::defaultBlockState, BlockState::new).any());
        instance = registry.get(id.toString()).get();
        instance.internal = this;
    }

    public void setPos(BlockPos pos) {
        ((BlockPos.MutableBlockPos)worldPosition).set(pos);
    }

    public static TagCompound legacyConverter(TagCompound data) {
        for (Function<CompoundTag, BlockState> migration : migrations) {
            if (migration.apply(data.internal) != null) {
                break;
            }
        }
        return data;
    }

    private static final List<Function<CompoundTag, BlockState>> migrations = new ArrayList<>();
    public static void registerLegacyTE(Identifier legacyID) {
        registerLegacyTE(legacyID, null);
    }
    public static void registerLegacyTE(Identifier legacyID, BlockState state) {
        migrations.add(data -> {
            if (data != null && legacyID.toString().equals(data.getString("id"))) {
                BlockState myState = state;
                if (myState == null) {
                    myState = blocks.get(data.getString("instanceId")).internal.defaultBlockState();
                }
                BlockPos pos = new BlockPos(data.getInt("x"), data.getInt("y"), data.getInt("z"));
                data.putString("id", BlockEntityType.getKey(((EntityBlock)myState.getBlock()).newBlockEntity(pos, null).getType()).toString());
                return myState;
            }
            return null;
        });
    }
    static {
        registerLegacyTE(new Identifier(ModCore.MODID, "hack"));

        CommonEvents.World.LOAD_CHUNK.subscribe(chunk -> {
            //TODO figure out if still needed in 1.19+
//            if(chunk instanceof LevelChunk) {
//                return;
//            }
            for (BlockPos pos : chunk.getBlockEntitiesPos()) {
                CompoundTag data = chunk.getBlockEntityNbt(pos);
                for (Function<CompoundTag, BlockState> migration : migrations) {
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
//        example.supplier(id);

        CommonEvents.Tile.REGISTER.subscribe(helper -> {
            BlockEntityType<TileEntity> type = new BlockEntityType<>((pos, state) -> {
                TileEntity tile = example.supplier(id);
                tile.setBlockState(state);
                return tile;
            }, new HashSet<>() {
                public boolean contains(Object var1) {
                    // WHYYYYYYYYYYYYYYYY
                    return true;
                }
            }, null);
            types.put(id.toString(), type);
            helper.register(id.internal, type);
        });
    }

    public static BlockEntityType<TileEntity> getType(Identifier type) {
        return (BlockEntityType<TileEntity>) types.get(type.toString());
    }

    /** Wrap getPos() in a cached UMC Vec3i */
    public Vec3i getUMCPos() {
        if (umcPos == null || !umcPos.internal().equals(worldPosition)) {
            umcPos = new Vec3i(worldPosition);
        }
        return umcPos;
    }

    /** Wrap getWorld in cached UMC World */
    public World getUMCWorld() {
        if (umcWorld == null || umcWorld.internal != level) {
            umcWorld = World.get(level);
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
    public final void loadAdditional(CompoundTag compound, HolderLookup.Provider provider) {
        super.loadAdditional(compound, provider);
        hasTileData = true;
        //Add check here in order to avoid accessing newly created TE which doesn't have this field
        if(compound.contains("x")){
            // Hack; Because of the new BlockEntity Constructor we now have to set the position manually after loading the BE
            setPos(new BlockPos(compound.getInt("x"), compound.getInt("y"), compound.getInt("z")));
        }
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
    public void saveAdditional(CompoundTag compound, HolderLookup.Provider provider) {
        super.saveAdditional(compound, provider);

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
    }

    /** Active Synchronization from markDirty */
    @Override
    public final ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this, (e, access) -> getUpdateTag(true, access));
    }

    /** Active Synchronization from markDirty */
    @Override
    public final CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return getUpdateTag(false, provider);
    }
    public final CompoundTag getUpdateTag(boolean writeUpdate, HolderLookup.Provider provider) {
        CompoundTag tag = super.getUpdateTag(provider);
        if (this.isLoaded()) {
            this.saveAdditional(tag, provider);
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

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider provider) {
        handleUpdateTag(pkt.getTag(), provider);
    }

    /** Active Synchronization from markDirty */
    @Override
    public final void handleUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        try {
            this.loadAdditional(tag, provider);
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
        level.sendBlockUpdated(super.worldPosition, null, super.level.getBlockState(super.worldPosition), 3);
    }

    /** Fire off update packet if on server, re-render if on client */
    @Override
    public void setChanged() {
        super.setChanged();
        if (!level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), level.getBlockState(getBlockPos()), level.getBlockState(getBlockPos()), 1 + 2 + 8);
            level.updateNeighborsAt(worldPosition, level.getBlockState(getBlockPos()).getBlock());
        }
    }

    /* Forge Overrides */

    public final SingleCache<IBoundingBox, AABB> bbCache =
            new SingleCache<>(internal -> BoundingBox.from(internal).move(getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ()));

    /* Moved to cam72cam.mod.render.BlockRender
      @Override
      public net.minecraft.world.phys.AABB getRenderBoundingBox() {
          if (instance() != null) {
              return bbCache.get(instance().getRenderBoundingBox());
          }
          return INFINITE_EXTENT_AABB;
      }
      */

    /**
     * @return Instance's render distance
     * @see BlockEntity
     */
    /* Moved to BlockEntityRenderer
    @Override
    public double getViewDistance() {
        return instance() != null ? instance().getRenderDistance() * instance().getRenderDistance() : Integer.MAX_VALUE;
    }*/

    public static final BlockCapability<IItemHandler, Direction> ITEM_HANDLER_BLOCK =
            BlockCapability.create(ResourceLocation.tryBuild(ModCore.MODID, "item_handler"),
                                   IItemHandler.class, Direction.class);

    public static final BlockCapability<IFluidHandler, Direction> FLUID_HANDLER_BLOCK =
            BlockCapability.create(ResourceLocation.tryBuild(ModCore.MODID, "fluid_handler"),
                                   IFluidHandler.class, Direction.class);

    public static final BlockCapability<IEnergyStorage, Direction> ENERGY_HANDLER_BLOCK =
            BlockCapability.create(ResourceLocation.tryBuild(ModCore.MODID, "energy_handler"),
                                   IEnergyStorage.class, Direction.class);

    public IItemHandler getItemHandler(@Nullable Direction side) {
        IInventory target = getInventory(Facing.from(side));
        if (target == null) {
            return null;
        }
        return new IItemHandlerModifiable() {
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
                return target.get(slot).internal();
            }

            @Nonnull
            @Override
            public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
                return target.insert(slot, new cam72cam.mod.item.ItemStack(stack), simulate).internal();
            }

            @Nonnull
            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return target.extract(slot, amount, simulate).internal();
            }

            @Override
            public int getSlotLimit(int slot) {
                return target.getLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
                return true; //TODO 1.14.4
            }
        };
    }

    public IFluidHandler getFluidHandler(@Nullable Direction side) {
        ITank target = getTank(Facing.from(side));
        if (target == null) {
            return null;
        }
        return new IFluidHandler() {
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
                return target.drain(new cam72cam.mod.fluid.FluidStack(new FluidStack(target.getContents().internal.getFluid(), maxDrain)), action.simulate()).internal;
            }
        };
    }

    public IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        IEnergy target = getEnergy(Facing.from(side));
        if (target == null) {
            return null;
        }
        return new IEnergyStorage() {
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
        };
    }

    public boolean hasCapacity(BlockCapability<?, Direction> capability, Direction side) {
        if (capability == ITEM_HANDLER_BLOCK) {
            return getInventory(Facing.from(side)) != null;
        } else if (capability == FLUID_HANDLER_BLOCK) {
            return getTank(Facing.from(side)) != null;
        } else if (capability == ENERGY_HANDLER_BLOCK) {
            return getEnergy(Facing.from(side)) != null;
        }
        return false;
    }

    /*
    New Functionality
    */

    /** @return If the BlockEntity instance is loaded */
    public boolean isLoaded() {
        return level != null && (!level.isClientSide || hasTileData);
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
    public final ModelData getModelData() {
        return ModelData.builder().with(TE_PROPERTY, this).build();
    }

    /* 1.16+ caching */
    private final SingleCache<IBoundingBox, VoxelShape> shapeCache = new SingleCache<>((IBoundingBox box) -> Shapes.create(BoundingBox.from(box)));
    public VoxelShape getShape() {
        if (instance() != null) {
            return shapeCache.get(this.instance().getBoundingBox());
        }
        return null;
    }
}
