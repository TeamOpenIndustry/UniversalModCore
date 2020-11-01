package cam72cam.mod.block;

import cam72cam.mod.block.tile.TileEntity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.boundingbox.BoundingBox;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.util.Facing;
import cam72cam.mod.util.SingleCache;
import cam72cam.mod.world.World;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.IBlockAccess;
import net.minecraft.block.Block;

/**
 * Extension to BlockType that integrates with BlockEntities.
 *
 * Most if not all of the functions exposed are now redirected to the block entity (break/pick/etc...)
 */
public abstract class BlockTypeEntity extends BlockType {
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
        TileEntity te = ((TileEntity) internal.createTileEntity(null, 0));
        te.setWorldObj(world.internal);
        te.xCoord = pos.x;
        te.yCoord = pos.y;
        te.zCoord = pos.z;
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
        public final boolean hasTileEntity() {
            return true;
        }

        @Override
        public boolean hasTileEntity(int metadata) {
            return true;
        }

        @Override
        public final net.minecraft.tileentity.TileEntity createTileEntity(net.minecraft.world.World world, int state) {
            return constructBlockEntity().supplier(id);
        }

        public void setBlockBoundsBasedOnState(IBlockAccess source, int posX, int posY, int posZ) {
            net.minecraft.tileentity.TileEntity entity = source.getTileEntity(posX, posY, posZ);
            if (entity == null) {
                super.setBlockBoundsBasedOnState(source, posX, posY, posZ);
            }

            IBoundingBox box = getBoundingBox(World.get(entity.getWorld()), new Vec3i(posX, posY, posZ));
            minX = box.min().x;
            minY = box.min().y;
            minZ = box.min().z;
            maxX = box.max().x;
            maxY = box.max().y;
            maxZ = box.max().z;
        }

        private final SingleCache<IBoundingBox, AxisAlignedBB> bbCache = new SingleCache<>(BoundingBox::from);
        @Override
        public AxisAlignedBB getCollisionBoundingBoxFromPool(net.minecraft.world.World source, int posX, int posY, int posZ) {
            net.minecraft.tileentity.TileEntity entity = source.getTileEntity(posX, posY, posZ);
            if (entity == null) {
                return super.getCollisionBoundingBoxFromPool(source, posX, posY, posZ);
            }
            return bbCache.get(getBoundingBox(World.get(entity.getWorld()), new Vec3i(posX, posY, posZ)));
        }
    }
}
