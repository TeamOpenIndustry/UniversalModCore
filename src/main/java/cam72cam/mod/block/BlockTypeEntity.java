package cam72cam.mod.block;

import cam72cam.mod.block.tile.TileEntity;
import cam72cam.mod.block.tile.TileEntityTickable;
import cam72cam.mod.block.tile.TileEntityTickableTrack;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.util.Facing;
import cam72cam.mod.util.Hand;
import cam72cam.mod.util.ITrack;
import cam72cam.mod.world.World;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.EntityContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

import java.util.function.Supplier;

public abstract class BlockTypeEntity extends BlockType {
    protected final Identifier id;
    private final BlockEntityType<TileEntity> teType;

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

    protected class BlockTypeInternal extends BlockInternal implements BlockEntityProvider {
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
    }


}
