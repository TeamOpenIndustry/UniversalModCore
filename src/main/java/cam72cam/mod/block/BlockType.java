package cam72cam.mod.block;

import cam72cam.mod.entity.Player;
import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Facing;
import cam72cam.mod.util.Hand;
import cam72cam.mod.world.World;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.Direction;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public abstract class BlockType {
    public final net.minecraft.block.Block internal;
    protected final BlockSettings settings;

    public BlockType(BlockSettings settings) {
        this.settings = settings;

        internal = getBlock();

        CommonEvents.Block.REGISTER.subscribe(() -> ForgeRegistries.BLOCKS.register(internal));

        CommonEvents.Block.BROKEN.subscribe((world, pos, player) -> {
            net.minecraft.block.Block block = world.getBlockState(pos).getBlock();
            if (block instanceof BlockInternal) {
                return ((BlockInternal) block).tryBreak(world, pos, player);
            }
            return true;
        });
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
            setSoundType(settings.material.soundType);
            setUnlocalizedName(settings.modID + ":" + settings.name);
            setRegistryName(new ResourceLocation(settings.modID, settings.name));
        }

        @Override
        public final void breakBlock(net.minecraft.world.World world, BlockPos pos, BlockState state) {
            BlockType.this.onBreak(World.get(world), new Vec3i(pos));
            super.breakBlock(world, pos, state);
        }

        @Override
        public final boolean onBlockActivated(net.minecraft.world.World world, BlockPos pos, BlockState state, EntityPlayer player, EnumHand hand, Direction facing, float hitX, float hitY, float hitZ) {
            return BlockType.this.onClick(World.get(world), new Vec3i(pos), new Player(player), Hand.from(hand), Facing.from(facing), new Vec3d(hitX, hitY, hitZ));
        }

        @Override
        public final net.minecraft.item.ItemStack getPickBlock(BlockState state, RayTraceResult target, net.minecraft.world.World world, BlockPos pos, EntityPlayer player) {
            return BlockType.this.onPick(World.get(world), new Vec3i(pos)).internal;
        }

        @Override
        public void neighborChanged(BlockState state, net.minecraft.world.World worldIn, BlockPos pos, net.minecraft.block.Block blockIn, BlockPos fromPos) {
            this.onNeighborChange(worldIn, pos, fromPos);
        }

        @Override
        public void onNeighborChange(IBlockAccess world, BlockPos pos, BlockPos neighbor) {
            BlockType.this.onNeighborChange(World.get((net.minecraft.world.World) world), new Vec3i(pos), new Vec3i(neighbor));
        }

        /*
        Overrides
         */
        @Override
        public final float getExplosionResistance(Entity exploder) {
            return settings.resistance;
        }


        @Override
        public final EnumBlockRenderType getRenderType(BlockState state) {
            // TESR Renderer TODO OPTIONAL!@!!!!
            return EnumBlockRenderType.MODEL;
        }


        @Override
        public final boolean isOpaqueCube(BlockState state) {
            return false;
        }

        @Override
        public final boolean isFullCube(BlockState state) {
            return false;
        }

        @Override
        public AxisAlignedBB getCollisionBoundingBox(BlockState state, IBlockAccess source, BlockPos pos) {
            return new AxisAlignedBB(0, 0, 0, 1, BlockType.this.getHeight(), 1);
        }

        @Override
        public AxisAlignedBB getBoundingBox(BlockState state, IBlockAccess source, BlockPos pos) {
            return new AxisAlignedBB(0, 0, 0, 1, BlockType.this.getHeight(), 1);
        }

        @Override
        public AxisAlignedBB getSelectedBoundingBox(BlockState state, net.minecraft.world.World worldIn, BlockPos pos) {
            return getCollisionBoundingBox(state, worldIn, pos).expand(0, 0.1, 0).offset(pos);
        }

        @Override
        public int getMetaFromState(BlockState state) {
            return 0;
        }

        /*
         * Fence, glass override
         */
        @Override
        public boolean canBeConnectedTo(IBlockAccess internal, BlockPos pos, Direction facing) {
            return settings.connectable;
        }

        @Deprecated
        @Override
        public BlockFaceShape getBlockFaceShape(IBlockAccess p_193383_1_, BlockState p_193383_2_, BlockPos p_193383_3_, Direction p_193383_4_) {
            if (settings.connectable) {
                return super.getBlockFaceShape(p_193383_1_, p_193383_2_, p_193383_3_, p_193383_4_);
            }

            if (p_193383_4_ == Direction.UP) {
                // SNOW ONLY?
                return BlockFaceShape.SOLID;
            }
            return BlockFaceShape.UNDEFINED;
        }

        public boolean tryBreak(net.minecraft.world.World world, BlockPos pos, EntityPlayer player) {
            return BlockType.this.tryBreak(World.get(world), new Vec3i(pos), new Player(player));
        }

        /* Redstone */
        /* TODO REDSTONE!!!

        @Override
        public int getWeakPower(BlockState blockState, IBlockAccess blockAccess, BlockPos pos, Direction side)
        {
            if (settings.entity == null) {
                return 0;
            }
            World world = World.get((net.minecraft.world.World) blockAccess);
            net.minecraft.block.entity.BlockEntity ent =  world.getTileEntity(new Vec3i(pos), net.minecraft.block.entity.BlockEntity.class);
            if (ent instanceof IRedstoneProvider) {
                IRedstoneProvider provider = (IRedstoneProvider) ent;
                return provider.getRedstoneLevel();
            }
            return 0;
        }

        @Override
        public int getStrongPower(BlockState blockState, IBlockAccess blockAccess, BlockPos pos, Direction side)
        {
            return this.getWeakPower(blockState, blockAccess, pos, side);
        }

        @Override
        public boolean canProvidePower(BlockState state)
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
