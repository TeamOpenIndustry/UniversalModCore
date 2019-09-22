package cam72cam.mod.block;

import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.render.BlockRender;
import cam72cam.mod.util.Facing;
import cam72cam.mod.util.Hand;
import cam72cam.mod.world.World;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.world.BlockEvent;

import java.util.ArrayList;
import java.util.List;

public abstract class BlockType {
    private static List<Runnable> registrations = new ArrayList<>();
    public final net.minecraft.block.Block internal;
    protected final BlockSettings settings;

    public BlockType(BlockSettings settings) {
        this.settings = settings;

        internal = getBlock();


        registrations.add(() -> GameRegistry.registerBlock(internal, getName()));
    }

    public static void registerBlocks() {
        registrations.forEach(Runnable::run);
    }

    public static class EventBus {
        @SubscribeEvent
        public static void onBlockBreakEvent(BlockEvent.BreakEvent event) {
            if (event.block instanceof BlockInternal) {
                BlockInternal internal = (BlockInternal) event.block;
                if (!internal.tryBreak(event.world, event.x, event.y, event.z, event.getPlayer())) {
                    event.setCanceled(true);
                    //TODO updateListeners?
                }
            }
        }
    }

    public String getName() {
        return settings.name;
    }

    protected BlockInternal getBlock() {
        return new BlockInternal();
    }

    public abstract boolean tryBreak(World world, Vec3i pos, Player player);

    /*
    Public functionality
     */

    public abstract void onBreak(World world, Vec3i pos);

    public abstract boolean onClick(World world, Vec3i pos, Player player, Hand hand, Facing facing, Vec3d hit);

    public abstract ItemStack onPick(World world, Vec3i pos);

    public abstract void onNeighborChange(World world, Vec3i pos, Vec3i neighbor);

    public double getHeight() {
        return 1;
    }

    protected class BlockInternal extends net.minecraft.block.Block {
        public BlockInternal() {
            super(settings.material.internal);
            setHardness(settings.hardness);
            setStepSound(settings.material.soundType);
            setBlockName(settings.modID + ":" + settings.name);
            // REMOVED 1.7.10 setRegistryName(new ResourceLocation(settings.modID, settings.name));
        }

        @Override
        public int getRenderType() {
            return BlockRender.getRenderType(BlockType.this);
        }

        @Override
        public final void breakBlock(net.minecraft.world.World world, int posX, int posY, int posZ, Block block, int meta) {
            BlockType.this.onBreak(World.get(world), new Vec3i(posX, posY, posZ));
            super.breakBlock(world, posX, posY, posZ, block, meta);
        }

        @Override
        public final boolean onBlockActivated(net.minecraft.world.World world, int posX, int posY, int posZ, EntityPlayer player, int facing, float hitX, float hitY, float hitZ) {
            return BlockType.this.onClick(World.get(world), new Vec3i(posX, posY, posZ), new Player(player), Hand.PRIMARY, Facing.from(EnumFacing.getFront(facing)), new Vec3d(hitX, hitY, hitZ));
        }

        @Override
        public final net.minecraft.item.ItemStack getPickBlock(MovingObjectPosition target, net.minecraft.world.World world, int posX, int posY, int posZ, EntityPlayer player) {
            return BlockType.this.onPick(World.get(world), new Vec3i(posX, posY, posZ)).internal;
        }

        @Override
        public void onNeighborBlockChange(net.minecraft.world.World worldIn, int posX, int posY, int posZ, Block blockIn) {
            // TODO 1.10 this might have some interesting side effects
            this.onNeighborChange(worldIn, posX, posY, posZ, posX, posY, posZ);
        }

        @Override
        public void onNeighborChange(IBlockAccess world, int posX, int posY, int posZ, int neighborX, int neighborY, int neighborZ) {
            BlockType.this.onNeighborChange(World.get((net.minecraft.world.World) world), new Vec3i(posX, posY, posZ), new Vec3i(neighborX, neighborY, neighborZ));
        }

        /*
        Overrides
         */
        @Override
        public final float getExplosionResistance(Entity exploder) {
            return settings.resistance;
        }

        /* TODO 1.7.10
        @Override
        public final EnumBlockRenderType getRenderType(Block block, int meta) {
            // TESR Renderer TODO OPTIONAL!@!!!!
            return EnumBlockRenderType.MODEL;
        }
        */


        @Override
        public final boolean isOpaqueCube() {
            return false;
        }

        @Override
        public final boolean isNormalCube() {
            return false;
        }

        @Override
        public AxisAlignedBB getCollisionBoundingBoxFromPool(net.minecraft.world.World source, int posX, int posY, int posZ) {
            return AxisAlignedBB.getBoundingBox(0, 0, 0, 1, BlockType.this.getHeight(), 1);
        }

        /* Removed 1.7.10
        @Override
        public AxisAlignedBB getBoundingBox(Block block, int meta, IBlockAccess source, int posX, int posY, int posZ) {
            return AxisAlignedBB.getBoundingBox(0, 0, 0, 1, BlockType.this.getHeight(), 1);
        }
        */

        @Override
        public AxisAlignedBB getSelectedBoundingBoxFromPool(net.minecraft.world.World worldIn, int posX, int posY, int posZ) {
            return getCollisionBoundingBoxFromPool(worldIn, posX, posY, posZ)
                    .expand(0, 0.1, 0)
                    .offset(posX, posY, posZ);
        }

        /* Removed 1.7.10
        @Override
        public int getMetaFromState(Block block, int meta) {
            return 0;
        }
        */

        /*
         * Fence, glass override
         */

        @Deprecated
        @Override
        public boolean isSideSolid(IBlockAccess world, int posX, int posY, int posZ, ForgeDirection side) {
            if (settings.connectable) {
                return super.isSideSolid(world, posX, posY, posZ, side);
            }

            // SNOW ONLY?
            return side == ForgeDirection.UP;
        }

        public boolean tryBreak(net.minecraft.world.World world, int posX, int posY, int posZ, EntityPlayer player) {
            return BlockType.this.tryBreak(World.get(world), new Vec3i(posX, posY, posZ), new Player(player));
        }

        /* Redstone */
        /* TODO REDSTONE!!!

        @Override
        public int getWeakPower(IBlockState blockState, IBlockAccess blockAccess, int posX, int posY, int posZ, EnumFacing side)
        {
            if (settings.entity == null) {
                return 0;
            }
            World world = World.get((net.minecraft.world.World) blockAccess);
            net.minecraft.tileentity.TileEntity ent =  world.getTileEntity(new Vec3i(posX, posY, posZ), net.minecraft.tileentity.TileEntity.class);
            if (ent instanceof IRedstoneProvider) {
                IRedstoneProvider provider = (IRedstoneProvider) ent;
                return provider.getRedstoneLevel();
            }
            return 0;
        }

        @Override
        public int getStrongPower(IBlockState blockState, IBlockAccess blockAccess, int posX, int posY, int posZ, EnumFacing side)
        {
            return this.getWeakPower(blockState, blockAccess, pos, side);
        }

        @Override
        public boolean canProvidePower(Block block, int meta)
        {
            return true;
        }
        */

            /* TODO
            @SideOnly(Side.CLIENT)
            public BlockRenderLayer getBlockLayer() {
                return BlockRenderLayer.CUTOUT_MIPPED;
            }
            */

    }
}
