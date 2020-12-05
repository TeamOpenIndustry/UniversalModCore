package cam72cam.mod.block;

import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.boundingbox.BoundingBox;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.util.Facing;
import cam72cam.mod.util.SingleCache;
import cam72cam.mod.world.World;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;

/** A standard block with no attached entity */
public abstract class BlockType {
    /*
    Hook into the Block Broken event (not specific per block)
     */
    static {
        CommonEvents.Block.BROKEN.subscribe((world, pos, player) -> {
            net.minecraft.block.Block block = world.getBlockState(pos).getBlock();
            if (block instanceof BlockInternal) {
                return ((BlockInternal) block).tryBreak(world, pos, player);
            }
            return true;
        });
    }

    /** Wraps the minecraft construct, do not use directly. */
    public final net.minecraft.block.Block internal;

    /** Mod/name of the block */
    public final Identifier id;

    /**
     * Construct a new BlockType (backed by a std minecraft block)<br>
     * <br>
     * Should be called during ModEvent.CONSTRUCT
     */
    public BlockType(String modID, String name) {
        this.id = new Identifier(modID, name);
        internal = getBlock();
        CommonEvents.Block.REGISTER.subscribe(() -> ForgeRegistries.BLOCKS.register(internal));
    }

    /** Override to provide a custom Minecraft Block implementation (ex: support tile entities) */
    protected BlockInternal getBlock() {
        return new BlockInternal();
    }

    /** @return false if the in-progress break should be cancelled */
    public abstract boolean tryBreak(World world, Vec3i pos, Player player);

    /*
    Properties
     */

    public Material getMaterial() {
        return Material.METAL;
    }
    public float getHardness() {
        return 1.0f;
    }
    public float getExplosionResistance() {
        return getHardness() * 5;
    }

    /** @return if fencing / glass should connect to it */
    public boolean isConnectable() {
        return true;
    }
    /** @return true if redstone functions below should be wired in */
    public boolean isRedstoneProvider() {
        return false;
    }


    /*
    Public functionality
     */

    /** Called when the block is broken */
    public abstract void onBreak(World world, Vec3i pos);

    /**
     * Called when a player right-clicks a block.
     * @return true if this accepts the click (stop processing it after this call)
     */
    public abstract boolean onClick(World world, Vec3i pos, Player player, Player.Hand hand, Facing facing, Vec3d hit);

    /**
     * Called when a player performs the pick operation on this block.
     * @return An ItemStack representing this block.  Must NOT be null, return ItemStack.EMPTY instead.
     */
    public abstract ItemStack onPick(World world, Vec3i pos);

    /**
     * Called when a neighboring block has changed state
     */
    public abstract void onNeighborChange(World world, Vec3i pos, Vec3i neighbor);

    /**
     * Shape of the block.
     */
    protected static final IBoundingBox defaultBox = IBoundingBox.from(new AxisAlignedBB(0, 0, 0, 1, 1, 1));
    public IBoundingBox getBoundingBox(World world, Vec3i pos) {
        return defaultBox;
    }

    /**
     * Only applicable if isRedstoneProvider returns true
     * @return strong redstone power
     */
    public int getStrongPower(World world, Vec3i vec3i, Facing from) {
        return 0;
    }

    /**
     * Only applicable if isRedstoneProvider returns true
     * @return strong redstone power
     */
    public int getWeakPower(World world, Vec3i vec3i, Facing from) {
        return 0;
    }

    /**
     * BlockInternal is an internal class that should only be extended when you need to implement
     * an interface.
     */
    protected class BlockInternal extends net.minecraft.block.Block {
        public BlockInternal() {
            super(BlockType.this.getMaterial().internal);
            BlockType type = BlockType.this;
            setHardness(type.getHardness());
            setSoundType(type.getMaterial().soundType);
            setUnlocalizedName(type.id.toString());
            setRegistryName(type.id.internal);
        }

        /** Called server side at the end of the block break call chain as cleanup */
        @Override
        public final void breakBlock(net.minecraft.world.World world, BlockPos pos, IBlockState state) {
            BlockType.this.onBreak(World.get(world), new Vec3i(pos));
            super.breakBlock(world, pos, state);
        }

        /** Called both client and server side when a player right clicks on a block.  Can cancel the event by returning true (handled) */
        @Override
        public final boolean onBlockActivated(net.minecraft.world.World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, @Nullable net.minecraft.item.ItemStack heldItem, EnumFacing facing, float hitX, float hitY, float hitZ) {
            return BlockType.this.onClick(World.get(world), new Vec3i(pos), new Player(player), Player.Hand.from(hand), Facing.from(facing), new Vec3d(hitX, hitY, hitZ));
        }

        @Override
        public final net.minecraft.item.ItemStack getPickBlock(IBlockState state, RayTraceResult target, net.minecraft.world.World world, BlockPos pos, EntityPlayer player) {
            return BlockType.this.onPick(World.get(world), new Vec3i(pos)).internal;
        }

        @Override
        public void neighborChanged(IBlockState state, net.minecraft.world.World worldIn, BlockPos pos, Block blockIn) {
            // TODO 1.10 this might have some interesting side effects
            this.onNeighborChange(worldIn, pos, pos);
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
            return BlockType.this.getExplosionResistance();
        }


        @Override
        public final EnumBlockRenderType getRenderType(IBlockState state) {
            // TESR Renderer TODO OPTIONAL!@!!!!
            return EnumBlockRenderType.MODEL;
        }


        @Override
        public final boolean isOpaqueCube(IBlockState state) {
            return false;
        }

        @Override
        public final boolean isFullCube(IBlockState state) {
            return false;
        }

        @Override
        public AxisAlignedBB getCollisionBoundingBox(IBlockState state, net.minecraft.world.World source, BlockPos pos) {
            return getBoundingBox(state, source, pos);
        }

        protected World getWorldOrNull(IBlockAccess source, BlockPos pos) {
            return source instanceof net.minecraft.world.World ? World.get((net.minecraft.world.World)source) : null;
        }

        private final SingleCache<IBoundingBox, AxisAlignedBB> bbCache = new SingleCache<>(BoundingBox::from);
        @Override
        public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
            World world = getWorldOrNull(source, pos);
            if (world != null) {
                return bbCache.get(
                        BlockType.this.getBoundingBox(world, new Vec3i(pos))
                );
            }
            return super.getBoundingBox(state, source, pos);
        }

        @Override
        public AxisAlignedBB getSelectedBoundingBox(IBlockState state, net.minecraft.world.World worldIn, BlockPos pos) {
            return getCollisionBoundingBox(state, worldIn, pos).offset(pos);
        }

        @Override
        public int getMetaFromState(IBlockState state) {
            return 0;
        }

        /*
         * Fence, glass override
         */

        @Deprecated
        @Override
        public boolean isSideSolid(IBlockState base_state, IBlockAccess world, BlockPos pos, EnumFacing side) {
            if (BlockType.this.isConnectable()) {
                return super.isSideSolid(base_state, world, pos, side);
            }
            return false;
        }

        public boolean tryBreak(net.minecraft.world.World world, BlockPos pos, EntityPlayer player) {
            return BlockType.this.tryBreak(World.get(world), new Vec3i(pos), new Player(player));
        }

        /* Redstone */

        @Override
        public int getWeakPower(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side)
        {
            World world = getWorldOrNull(blockAccess, pos);
            if (world != null) {
                return BlockType.this.isRedstoneProvider() ? BlockType.this.getWeakPower(world, new Vec3i(pos), Facing.from(side)) : 0;
            }
            return 0;
        }

        @Override
        public int getStrongPower(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side)
        {
            World world = getWorldOrNull(blockAccess, pos);
            if (world != null) {
                return BlockType.this.isRedstoneProvider() ? BlockType.this.getStrongPower(world, new Vec3i(pos), Facing.from(side)) : 0;
            }
            return 0;
        }

        @Override
        public boolean canProvidePower(IBlockState state)
        {
            return BlockType.this.isRedstoneProvider();
        }

        /* TODO
        @SideOnly(Side.CLIENT)
        public BlockRenderLayer getBlockLayer() {
            return BlockRenderLayer.CUTOUT_MIPPED;
        }
        */

    }
}
