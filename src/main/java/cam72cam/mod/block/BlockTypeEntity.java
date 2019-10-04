package cam72cam.mod.block;

import alexiil.mc.lib.attributes.*;
import alexiil.mc.lib.attributes.fluid.FixedFluidInv;
import alexiil.mc.lib.attributes.fluid.FluidExtractable;
import alexiil.mc.lib.attributes.fluid.FluidInsertable;
import alexiil.mc.lib.attributes.fluid.FluidInvTankChangeListener;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidKeys;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;
import alexiil.mc.lib.attributes.item.FixedItemInv;
import alexiil.mc.lib.attributes.item.InvMarkDirtyListener;
import alexiil.mc.lib.attributes.item.compat.FixedInventoryVanillaWrapper;
import cam72cam.mod.block.tile.TileEntity;
import cam72cam.mod.block.tile.TileEntityTickable;
import cam72cam.mod.block.tile.TileEntityTickableTrack;
import cam72cam.mod.energy.IEnergy;
import cam72cam.mod.entity.Player;
import cam72cam.mod.fluid.Fluid;
import cam72cam.mod.fluid.FluidStack;
import cam72cam.mod.fluid.ITank;
import cam72cam.mod.item.IInventory;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.util.Facing;
import cam72cam.mod.util.Hand;
import cam72cam.mod.util.ITrack;
import cam72cam.mod.world.World;
import io.github.cottonmc.energy.api.DefaultEnergyTypes;
import io.github.cottonmc.energy.api.EnergyAttribute;
import io.github.cottonmc.energy.api.EnergyType;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.EntityContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Supplier;

public abstract class BlockTypeEntity extends BlockType {
    protected final Identifier id;
    private final BlockEntityType<? extends TileEntity> teType;

    public BlockTypeEntity(BlockSettings settings, Supplier<BlockEntity> constructData) {
        super(settings);
        id = new Identifier(settings.modID, settings.name);

        if (constructData.get() instanceof BlockEntityTickable) {
            if (constructData.get() instanceof ITrack) {
                teType = TileEntityTickableTrack.register(id, constructData);
            } else {
                teType = TileEntityTickable.register(id, constructData);
            }
        } else {
            teType = TileEntity.register(id, constructData);
        }
    }

    public BlockEntity createBlockEntity(World world, Vec3i pos) {
        TileEntity te = teType.instantiate();
        te.hasTileData = true;
        te.world = world;
        te.pos = pos;
        return te.instance();
    }

    /*

    BlockType Implementation

    */

    protected BlockInternal getBlock() {
        return new BlockTypeInternal();
    }

    private BlockEntity getInstance(World world, Vec3i pos) {
        TileEntity te = world.getTileEntity(pos, TileEntity.class);
        if (te != null) {
            return (BlockEntity) te.instance();
        }
        return null;
    }

    @Override
    public final boolean tryBreak(World world, Vec3i pos, Player player) {
        BlockEntity instance = getInstance(world, pos);
        if (instance != null) {
            return instance.tryBreak(player);
        }
        return true;
    }

    /*

    Add block data to normal block calls

     */

    @Override
    public final void onBreak(World world, Vec3i pos) {
        BlockEntity instance = getInstance(world, pos);
        if (instance != null) {
            instance.onBreak();
        }
    }

    @Override
    public final boolean onClick(World world, Vec3i pos, Player player, Hand hand, Facing facing, Vec3d hit) {
        BlockEntity instance = getInstance(world, pos);
        if (instance != null) {
            return instance.onClick(player, hand, facing, hit);
        }
        return false;
    }

    @Override
    public final ItemStack onPick(World world, Vec3i pos) {
        BlockEntity instance = getInstance(world, pos);
        if (instance != null) {
            return instance.onPick();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public final void onNeighborChange(World world, Vec3i pos, Vec3i neighbor) {
        BlockEntity instance = getInstance(world, pos);
        if (instance != null) {
            instance.onNeighborChange(neighbor);
        }
    }

    public final double getHeight(World world, Vec3i pos) {
        BlockEntity instance = getInstance(world, pos);
        if (instance != null) {
            return instance.getHeight();
        }
        return 1;
    }

    protected class BlockTypeInternal extends BlockInternal implements BlockEntityProvider, AttributeProvider {
        @Override
        public final boolean hasBlockEntity() {
            return true;
        }

        public net.minecraft.block.entity.BlockEntity createBlockEntity(net.minecraft.world.BlockView var1) {
            return teType.instantiate();
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockView source, BlockPos pos, EntityContext entityContext_1) {
            net.minecraft.block.entity.BlockEntity entity = source.getBlockEntity(pos);
            if (entity == null) {
                return super.getCollisionShape(state, source, pos, entityContext_1);
            }
            return Block.createCuboidShape(0.0F, 0.0F, 0.0F, 1.0F, BlockTypeEntity.this.getHeight(World.get(entity.getWorld()), new Vec3i(pos)), 1.0F);
        }

        @Override
        public VoxelShape getRayTraceShape(BlockState state, BlockView source, BlockPos pos) {
            net.minecraft.block.entity.BlockEntity entity = source.getBlockEntity(pos);
            if (entity == null) {
                return super.getRayTraceShape(state, source, pos);
            }
            return Block.createCuboidShape(0.0F, 0.0F, 0.0F, 1.0F, Math.max(BlockTypeEntity.this.getHeight(World.get(entity.getWorld()), new Vec3i(pos)), 0.25), 1.0F);
        }

        @Override
        public VoxelShape getOutlineShape(BlockState state, BlockView source, BlockPos pos, EntityContext entityContext_1) {
            net.minecraft.block.entity.BlockEntity entity = source.getBlockEntity(pos);
            if (entity == null) {
                return super.getOutlineShape(state, source, pos, entityContext_1);
            }

            return Block.createCuboidShape(0, 0, 0, 1, Math.max(BlockTypeEntity.this.getHeight(World.get(entity.getWorld()), new Vec3i(pos)), 0.25)+0.1, 1);
        }

        @Override
        public void addAllAttributes(net.minecraft.world.World world, BlockPos pos, BlockState state, AttributeList<?> to) {
            net.minecraft.block.entity.BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof TileEntity) {
                TileEntity te = (TileEntity) be;
                ITank tank = te.getTank(Facing.UP);
                if (tank != null) {
                    to.offer(new FixedFluidInv() {

                        @Override
                        public int getTankCount() {
                            return 1;
                        }

                        @Override
                        public FluidVolume getInvFluid(int tankId) {
                            return tank.getContents().internal;
                        }

                        @Override
                        public int getMaxAmount(int tankId) {
                            return tank.getCapacity();
                        }

                        @Override
                        public boolean isFluidValidForTank(int tankId, FluidKey fluid) {
                            return tank.allows(Fluid.getFluid(fluid));
                        }

                        @Override
                        public ListenerToken addListener(FluidInvTankChangeListener listener, ListenerRemovalToken removalToken) {
                            return null;
                        }

                        @Override
                        public boolean setInvFluid(int tankId, FluidVolume to, Simulation simulation) {
                            return false;
                        }

                        @Override
                        public FluidInsertable getInsertable() {
                            return (fluid, simulation) -> {
                                int inserted = tank.fill(new FluidStack(fluid), simulation.isSimulate());
                                return FluidVolume.create(fluid.getFluidKey(), fluid.getAmount() - inserted);
                            };
                        }

                        @Override
                        public FluidExtractable getExtractable() {
                            return (filter, amount, simulation) -> {
                                if (filter.matches(tank.getContents().internal.getFluidKey())) {
                                    return tank.drain(new FluidStack(tank.getContents().getFluid(), amount), simulation.isSimulate()).internal;
                                }
                                return FluidVolume.create(FluidKeys.EMPTY, 0);
                            };
                        }
                    });
                }

                IInventory inv = ((TileEntity) be).getInventory(Facing.UP);
                if (inv != null) {
                    to.offer(new FixedInventoryVanillaWrapper(new Inventory() {
                        @Override
                        public int getInvSize() {
                            return inv.getSlotCount();
                        }

                        @Override
                        public boolean isInvEmpty() {
                            for (int i = 0; i < inv.getSlotCount(); i++) {
                                if (!inv.get(i).isEmpty()) {
                                    return false;
                                }
                            }
                            return true;
                        }

                        @Override
                        public net.minecraft.item.ItemStack getInvStack(int var1) {
                            return inv.get(var1).internal;
                        }

                        @Override
                        public net.minecraft.item.ItemStack takeInvStack(int var1, int var2) {
                            return inv.extract(var1, var2, false).internal;
                        }

                        @Override
                        public net.minecraft.item.ItemStack removeInvStack(int var1) {
                            return inv.extract(var1, inv.get(var1).getCount(), false).internal;
                        }

                        @Override
                        public void setInvStack(int var1, net.minecraft.item.ItemStack var2) {
                            inv.set(var1, new ItemStack(var2));
                        }

                        @Override
                        public void markDirty() {
                            // NOP
                        }

                        @Override
                        public boolean canPlayerUseInv(PlayerEntity var1) {
                            return true;
                        }

                        @Override
                        public void clear() {
                            for (int i = 0; i< inv.getSlotCount(); i++) {
                                removeInvStack(i);
                            }
                        }
                    }));
                }

                IEnergy energy = ((TileEntity) be).getEnergy(Facing.UP);
                if (energy != null) {
                    to.offer(new EnergyAttribute() {

                        @Override
                        public int getMaxEnergy() {
                            return energy.getMaxEnergyStored();
                        }

                        @Override
                        public int getCurrentEnergy() {
                            return energy.getEnergyStored();
                        }

                        @Override
                        public boolean canInsertEnergy() {
                            return true;
                        }

                        @Nonnull
                        @Override
                        public int insertEnergy(EnergyType energyType, int i, Simulation simulation) {
                            return energy.receiveEnergy(energyType.convertFrom(DefaultEnergyTypes.MEDIUM_VOLTAGE, i), simulation.isSimulate());
                        }

                        @Override
                        public boolean canExtractEnergy() {
                            return true;
                        }

                        @Nonnull
                        @Override
                        public int extractEnergy(EnergyType energyType, int i, Simulation simulation) {
                            return energyType.convertFrom(DefaultEnergyTypes.MEDIUM_VOLTAGE, energy.extractEnergy(energyType.convertFrom(DefaultEnergyTypes.MEDIUM_VOLTAGE, i), simulation.isSimulate()));
                        }

                        @Nonnull
                        @Override
                        public EnergyType getPreferredType() {
                            return DefaultEnergyTypes.MEDIUM_VOLTAGE;
                        }
                    });
                }
            }
        }
    }


}
