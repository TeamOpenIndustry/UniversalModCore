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
import net.minecraft.util.AxisAlignedBB;

import java.util.function.Supplier;

public abstract class BlockTypeEntity extends BlockType {
    protected final Identifier id;
    private final Supplier<BlockEntity> constructData;

    public BlockTypeEntity(BlockSettings settings, Supplier<BlockEntity> constructData) {
        super(settings);
        id = new Identifier(settings.modID, settings.name);
        this.constructData = constructData;
        TileEntity.register(constructData, id);
        ((TileEntity) internal.createTileEntity(null, 0)).register();
    }

    public BlockEntity createBlockEntity(World world, Vec3i pos) {
        TileEntity te = ((TileEntity) internal.createTileEntity(null, 0));
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

    protected class BlockTypeInternal extends BlockInternal {
        @Override
        public final boolean hasTileEntity() {
            return true;
        }

        @Override
        public final net.minecraft.tileentity.TileEntity createTileEntity(net.minecraft.world.World world, int meta) {
            if (constructData.get() instanceof BlockEntityTickable) {
                if (constructData.get() instanceof ITrack) {
                    return new TileEntityTickableTrack(id);
                }
                return new TileEntityTickable(id);
            }
            return new TileEntity(id);
        }

        @Override
        public AxisAlignedBB getCollisionBoundingBoxFromPool(net.minecraft.world.World source, int posX, int posY, int posZ) {
            net.minecraft.tileentity.TileEntity entity = source.getTileEntity(posX, posY, posZ);
            if (entity == null) {
                return super.getCollisionBoundingBoxFromPool(source, posX, posY, posZ);
            }
            return AxisAlignedBB.getBoundingBox(0.0F, 0.0F, 0.0F, 1.0F, BlockTypeEntity.this.getHeight(World.get(source), new Vec3i(posX, posY, posZ)), 1.0F);
        }
    }


}
