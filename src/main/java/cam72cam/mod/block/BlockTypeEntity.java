package cam72cam.mod.block;

import cam72cam.mod.block.tile.TileEntity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.boundingbox.BoundingBox;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Facing;
import cam72cam.mod.world.World;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;

import javax.annotation.Nonnull;

/**
 * Extension to BlockType that integrates with BlockEntities.
 *
 * Most if not all of the functions exposed are now redirected to the block entity (break/pick/etc...)
 */
public abstract class BlockTypeEntity extends BlockType {
    /** This is a crappy hack that allows us to pass the block entity instance into the renderer */
    public static final PropertyObject BLOCK_DATA = new PropertyObject("BLOCK_DATA");

    // Cached from ctr
    private final boolean isRedstoneProvider;

    public BlockTypeEntity(String modID, String name) {
        super(modID, name);
        TileEntity.register(this::constructBlockEntity, id);
        this.isRedstoneProvider = constructBlockEntity() instanceof IRedstoneProvider;

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
        TileEntity te = ((TileEntity) internal.createTileEntity(null, null));
        te.setWorld(world.internal);
        te.setPos(pos.internal());
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

    protected class BlockTypeInternal extends BlockInternal {
        @Override
        public final boolean hasTileEntity(IBlockState state) {
            return true;
        }

        @Override
        public final net.minecraft.tileentity.TileEntity createTileEntity(net.minecraft.world.World world, IBlockState state) {
            return constructBlockEntity().supplier(id);
        }

        @Override
        @Nonnull
        protected BlockStateContainer createBlockState() {
            return new ExtendedBlockState(this, new IProperty[0], new IUnlistedProperty<?>[]{BLOCK_DATA});
        }

        @Override
        public IBlockState getExtendedState(IBlockState origState, IBlockAccess access, BlockPos pos) {
            World world = getWorldOrNull(access, pos);
            if (world != null) {
                BlockEntity te = world.getBlockEntity(new Vec3i(pos), BlockEntity.class);
                if (te != null) {
                    IExtendedBlockState state = (IExtendedBlockState) origState;
                    state = state.withProperty(BLOCK_DATA, te);
                    return state;
                }
            }
            return super.getExtendedState(origState, access, pos);
        }

        @Override
        protected World getWorldOrNull(IBlockAccess source, BlockPos pos) {
            if (source instanceof net.minecraft.world.World) {
                return World.get((net.minecraft.world.World) source);
            }
            net.minecraft.tileentity.TileEntity te = source.getTileEntity(pos);
            if (te instanceof TileEntity && ((TileEntity) te).isLoaded()) {
                return ((TileEntity) te).getUMCWorld();
            }
            return null;
        }
    }
}
