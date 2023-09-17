package cam72cam.mod.block;

import cam72cam.mod.block.tile.TileEntity;
import cam72cam.mod.block.tile.TileEntityTickable;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Facing;
import cam72cam.mod.world.World;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Extension to BlockType that integrates with BlockEntities.
 *
 * Most if not all of the functions exposed are now redirected to the block entity (break/pick/etc...)
 */
public abstract class BlockTypeEntity extends BlockType {
    // Cached from ctr
    private final boolean isRedstoneProvider;
    private final boolean isTickable;

    public BlockTypeEntity(String modID, String name) {
        super(modID, name);
        TileEntity.register(() -> constructBlockEntity(), id, this);
        this.isRedstoneProvider = constructBlockEntity() instanceof IRedstoneProvider;
        this.isTickable = constructBlockEntity() instanceof BlockEntityTickable;

        // Force supplier load (may trigger static blocks like TE registration)
        constructBlockEntity().supplier(id);
    }

    /** Supply your custom BlockEntity constructor here */
    protected abstract BlockEntity constructBlockEntity();

    @Override
    public final boolean isRedstoneProvider() {
        return isRedstoneProvider;
    }

    /** Hack for initializing a "fake" te */
    public final BlockEntity createBlockEntity(World world, Vec3i pos) {
        TileEntity te = ((TileEntity) ((BlockTypeInternal)internal).newBlockEntity(pos.internal(), null));
        te.hasTileData = true;
        te.setLevel(world.internal);
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
            return te.instance();
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
    public final boolean onClick(World world, Vec3i pos, Player player, Player.Hand hand, Facing facing, Vec3d hit) {
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

    @Override
    public IBoundingBox getBoundingBox(World world, Vec3i pos) {
        BlockEntity instance = getInstance(world, pos);
        if (instance != null) {
            return instance.getBoundingBox();
        }
        return super.getBoundingBox(world, pos);
    }

    @Override
    public int getStrongPower(World world, Vec3i pos, Facing from) {
        if (isRedstoneProvider()) {
            BlockEntity instance = getInstance(world, pos);
            if (instance instanceof IRedstoneProvider) {
                return ((IRedstoneProvider)instance).getStrongPower(from);
            }
        }
        return 0;
    }

    @Override
    public int getWeakPower(World world, Vec3i pos, Facing from) {
        if (isRedstoneProvider()) {
            BlockEntity instance = getInstance(world, pos);
            if (instance instanceof IRedstoneProvider) {
                return ((IRedstoneProvider)instance).getWeakPower(from);
            }
        }
        return 0;
    }

    protected class BlockTypeInternal extends BlockInternal implements EntityBlock {
        @Override
        public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos p_153215_, BlockState p_153216_) {
            TileEntity tile = constructBlockEntity().supplier(id);
            tile.setBlockState(p_153216_);
            tile.setPos(p_153215_);
            return tile;
        }


        @Nullable
        @Override
        public <T extends net.minecraft.world.level.block.entity.BlockEntity> BlockEntityTicker<T> getTicker(Level p_154683_, BlockState p_154684_, BlockEntityType<T> p_154685_) {
            return p_154684_.getBlock() instanceof BlockTypeInternal && isTickable ? (BlockEntityTicker<T>)(BlockEntityTicker<TileEntityTickable>)this::ticker : null;
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
            net.minecraft.world.level.block.entity.BlockEntity entity = worldIn.getBlockEntity(pos);
            if (entity instanceof TileEntity) {
                VoxelShape shape = ((TileEntity) entity).getShape();
                return shape != null ? shape : Shapes.block();
            }
            return super.getShape(state, worldIn, pos, context);
        }

        @Override
        protected World getWorldOrNull(BlockGetter source, BlockPos pos) {
            if (source instanceof Level) {
                return World.get((Level) source);
            }
            net.minecraft.world.level.block.entity.BlockEntity te = source.getBlockEntity(pos);
            if (te instanceof TileEntity && ((TileEntity) te).isLoaded()) {
                return ((TileEntity) te).getUMCWorld();
            }
            return null;
        }

        private void ticker(Level level, BlockPos pos, BlockState state, TileEntityTickable instance) {
            instance.tick();
        }
    }
}
