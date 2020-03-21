package cam72cam.mod.block;

import cam72cam.mod.block.tile.TileEntity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.util.Facing;
import cam72cam.mod.util.Hand;
import cam72cam.mod.world.World;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.IBlockAccess;

import java.util.function.Supplier;

public abstract class BlockTypeEntity extends BlockType {
    protected final Identifier id;
    private final Supplier<BlockEntity> constructData;

    public BlockTypeEntity(BlockSettings settings, Supplier<BlockEntity> constructData) {
        super(settings.withRedstonePovider(constructData.get() instanceof IRedstoneProvider));
        id = new Identifier(settings.modID, settings.name);
        this.constructData = constructData;
        TileEntity.register(constructData, id);
        constructData.get().supplier(id).register();
    }

    public BlockEntity createBlockEntity(World world, Vec3i pos) {
        TileEntity te = ((TileEntity) internal.createTileEntity(null, 0));
        te.hasTileData = true;
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

    @Override
    public int getStrongPower(World world, Vec3i pos, Facing from) {
        if (settings.redstoneProvider) {
            BlockEntity instance = getInstance(world, pos);
            if (instance instanceof IRedstoneProvider) {
                return ((IRedstoneProvider)instance).getStrongPower(from);
            }
        }
        return 0;
    }

    @Override
    public int getWeakPower(World world, Vec3i pos, Facing from) {
        if (settings.redstoneProvider) {
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
            return constructData.get().supplier(id);
        }

        public void setBlockBoundsBasedOnState(IBlockAccess source, int posX, int posY, int posZ) {
            net.minecraft.tileentity.TileEntity entity = source.getTileEntity(posX, posY, posZ);
            if (entity == null) {
                super.setBlockBoundsBasedOnState(source, posX, posY, posZ);
            }
            super.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, (float) BlockTypeEntity.this.getHeight(World.get(entity.getWorldObj()), new Vec3i(posX, posY, posZ)), 1.0F);
        }

        @Override
        public AxisAlignedBB getCollisionBoundingBoxFromPool(net.minecraft.world.World source, int posX, int posY, int posZ) {
            net.minecraft.tileentity.TileEntity entity = source.getTileEntity(posX, posY, posZ);
            if (entity == null) {
                return super.getCollisionBoundingBoxFromPool(source, posX, posY, posZ);
            }
            return AxisAlignedBB.getBoundingBox(0.0F, 0.0F, 0.0F, 1.0F, BlockTypeEntity.this.getHeight(World.get(source), new Vec3i(posX, posY, posZ)), 1.0F).offset(posX, posY, posZ);
        }
    }


}
