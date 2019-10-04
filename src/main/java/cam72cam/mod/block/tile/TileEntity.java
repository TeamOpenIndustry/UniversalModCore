package cam72cam.mod.block.tile;

import cam72cam.mod.ModCore;
import cam72cam.mod.block.BlockEntity;
import cam72cam.mod.energy.IEnergy;
import cam72cam.mod.fluid.ITank;
import cam72cam.mod.item.IInventory;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.util.Facing;
import cam72cam.mod.util.TagCompound;
import cam72cam.mod.world.World;
import com.google.common.collect.HashBiMap;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.network.packet.BlockEntityUpdateS2CPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class TileEntity extends net.minecraft.block.entity.BlockEntity {
    private static final Map<Supplier<? extends BlockEntity>, BlockEntityType<?>> registry = HashBiMap.create();
    public World world;
    public Vec3i pos;
    public boolean hasTileData;

    /*
    Tile registration
    */
    private final BlockEntity instance;

    protected TileEntity(Supplier<? extends BlockEntity> ctr) {
        super(registry.get(ctr));
        instance = ctr.get();
    }

    public static BlockEntityType<? extends TileEntity> register(Identifier id, Supplier<BlockEntity> ctr) {
        return register(id, ctr, TileEntity::new);
    }
    protected static BlockEntityType<? extends TileEntity> register(Identifier id, Supplier<? extends BlockEntity> ctr, Function<Supplier<? extends BlockEntity>, ? extends TileEntity> tctr) {
        BlockEntityType<? extends TileEntity> type = Registry.register(Registry.BLOCK_ENTITY, id.internal, BlockEntityType.Builder.create(() -> tctr.apply(ctr)).build(null));
        registry.put(ctr, type);
        return type;
    }

    public Identifier getName() {
        return new Identifier(ModCore.MODID, "hack");
    }


    /*
    Standard Tile function overrides
    */

    @Override
    public void setWorld(net.minecraft.world.World world) {
        super.setWorld(world);
        this.world = World.get(world);
    }

    @Override
    public void setPos(BlockPos pos) {
        super.setPos(pos);
        this.pos = new Vec3i(pos);
    }


    @Override
    public final void fromTag(CompoundTag compound) {
        hasTileData = true;
        load(new TagCompound(compound));
        if (compound.getBoolean("isUpdate")) {
            readUpdate(new TagCompound(compound));
        }
    }

    @Override
    public final CompoundTag toTag(CompoundTag compound) {
        save(new TagCompound(compound));
        return compound;
    }

    public CompoundTag toInitialChunkDataTag() {
        TagCompound data = new TagCompound(super.toInitialChunkDataTag());
        save(data);
        data.setBoolean("isUpdate", true);
        return data.internal;
    }

    public final TagCompound getUpdateTag() {
        TagCompound tag = new TagCompound();
        if (this.isLoaded()) {
            this.toTag(tag.internal);
            this.writeUpdate(tag);
        }
        return tag;
    }

    public final void handleUpdateTag(TagCompound tag) {
        hasTileData = true;
        this.fromTag(tag.internal);
        this.readUpdate(tag);
        if (updateRerender()) {
            world.internal.scheduleBlockRender(getPos(), super.world.getBlockState(super.pos), super.world.getBlockState(super.pos));
        }
    }
    public BlockEntityUpdateS2CPacket toUpdatePacket() {
        new BlockEntityUpdatePacket(this).sendToAllAround(world, new Vec3d(pos), 8*16);
        return super.toUpdatePacket();
    }

    @Override
    public void markDirty() {
        super.markDirty();
        if (world.isServer) {
            world.internal.updateListeners(getPos(), world.internal.getBlockState(getPos()), world.internal.getBlockState(getPos()), 1 + 2 + 8);
            world.internal.updateNeighborsAlways(pos.internal, super.world.getBlockState(super.pos).getBlock());
        }
    }

    /* Forge Overrides */

    @Override
    public double getSquaredRenderDistance() {
        return instance() != null ? instance().getRenderDistance() * instance().getRenderDistance() : Integer.MAX_VALUE;
    }

    /* TODO capabilities
    @Override
    public boolean hasCapability(net.minecraftforge.common.capabilities.Capability<?> capability, @Nullable net.minecraft.util.Direction facing) {
        //TODO more efficient
        return getCapability(capability, facing) != null;
    }

    @Override
    @Nullable
    public <T> T getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable net.minecraft.util.Direction facing) {
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
                    return target.drain(new cam72cam.mod.fluid.FluidStack(new FluidStack(target.getContents().internal, maxDrain)), doDrain).internal;
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
            return CapabilityEnergy.ENERGY.cast(new IEnergyStorage() {
                @Override
                public int receiveEnergy(int maxReceive, boolean simulate) {
                    return target.receiveEnergy(maxReceive, simulate);
                }

                @Override
                public int extractEnergy(int maxExtract, boolean simulate) {
                    return target.extractEnergy(maxExtract, simulate);
                }

                @Override
                public int getEnergyStored() {
                    return target.getEnergyStored();
                }

                @Override
                public int getMaxEnergyStored() {
                    return target.getMaxEnergyStored();
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
    */

    /*
    Wrapped functionality
    */

    public void setWorld(World world) {
        super.setWorld(world.internal);
    }

    public void setPos(Vec3i pos) {
        super.setPos(pos.internal);
    }

    public void load(TagCompound data) {
        super.fromTag(data.internal);
        pos = new Vec3i(super.pos);
        instance.load(data);
    }

    public void save(TagCompound data) {
        super.toTag(data.internal);
        instance.save(data);
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
        return this.hasWorld() && (world.isServer || hasTileData);
    }

    // TODO render system?
    public boolean updateRerender() {
        return false;
    }

    public BlockEntity instance() {
        if (isLoaded()) {
            this.instance.internal = this;
            this.instance.world = this.world;
            this.instance.pos = this.pos;
            return this.instance;
        }
        return null;
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
