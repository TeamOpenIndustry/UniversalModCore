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
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraftforge.registries.ForgeRegistries;

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
            super(Block.Properties.of(BlockType.this.getMaterial().internal)
                    .sound(BlockType.this.getMaterial().soundType)
                    .strength(BlockType.this.getHardness(), BlockType.this.getExplosionResistance())
                    .dynamicShape());
            setRegistryName(BlockType.this.id.internal);
        }

        /** Called server side at the end of the block break call chain as cleanup */
        @Override
        public void onRemove(BlockState state, net.minecraft.world.World world, BlockPos pos, BlockState newState, boolean isMoving) {
            BlockType.this.onBreak(World.get(world), new Vec3i(pos));
            super.onRemove(state, world, pos, newState, isMoving);
        }

        /** Called both client and server side when a player right clicks on a block.  Can cancel the event by returning true (handled) */
        @Override
        public ActionResultType use(BlockState state, net.minecraft.world.World world, BlockPos pos, PlayerEntity player, net.minecraft.util.Hand hand, BlockRayTraceResult hit) {
            return BlockType.this.onClick(World.get(world), new Vec3i(pos), new Player(player), Player.Hand.from(hand), Facing.from(hit.getDirection()), new Vec3d(hit.getLocation()).subtract(new Vec3i(pos))) ? ActionResultType.SUCCESS : ActionResultType.PASS;
        }

        @Override
        public final net.minecraft.item.ItemStack getPickBlock(BlockState state, RayTraceResult target, IBlockReader worldIn, BlockPos pos, PlayerEntity player) {
            World world = getWorldOrNull(worldIn, pos);
            if (world != null) {
                return BlockType.this.onPick(world, new Vec3i(pos)).internal;
            }
            return net.minecraft.item.ItemStack.EMPTY;
        }

        @Override
        public void neighborChanged(BlockState state, net.minecraft.world.World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
            this.onNeighborChange(state, worldIn, pos, fromPos);
        }

        @Override
        public void onNeighborChange(BlockState state, IWorldReader world, BlockPos pos, BlockPos neighbor){
            BlockType.this.onNeighborChange(World.get((net.minecraft.world.World) world), new Vec3i(pos), new Vec3i(neighbor));
        }

        /*
        Overrides
         */

        @Override
        public BlockRenderType getRenderShape(BlockState state) {
            // TESR Renderer TODO OPTIONAL!@!!!!
            return BlockRenderType.MODEL;
        }

        protected World getWorldOrNull(IBlockReader source, BlockPos pos) {
            return source instanceof net.minecraft.world.World ? World.get((net.minecraft.world.World) source) : null;
        }

        private final SingleCache<IBoundingBox, VoxelShape> bbCache = new SingleCache<>((IBoundingBox box) -> VoxelShapes.create(BoundingBox.from(box)));
        @Override
        public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
            World world = getWorldOrNull(worldIn, pos);
            if (world != null) {
                return bbCache.get(BlockType.this.getBoundingBox(world, new Vec3i(pos)));
            }
            return super.getShape(state, worldIn, pos, context);
        }

        public boolean tryBreak(net.minecraft.world.IWorld worldIn, BlockPos pos, PlayerEntity player) {
            World world = getWorldOrNull(worldIn, pos);
            if (world != null) {
                return BlockType.this.tryBreak(world, new Vec3i(pos), new Player(player));
            }
            return true;

        }

        /* Redstone */

        @Override
        public int getSignal(BlockState blockState, IBlockReader blockAccess, BlockPos pos, Direction side)
        {
            World world = getWorldOrNull(blockAccess, pos);
            if (world != null) {
                return BlockType.this.isRedstoneProvider() ? BlockType.this.getWeakPower(world, new Vec3i(pos), Facing.from(side)) : 0;
            }
            return 0;
        }

        @Override
        public int getDirectSignal(BlockState blockState, IBlockReader blockAccess, BlockPos pos, Direction side)
        {
            World world = getWorldOrNull(blockAccess, pos);
            if (world != null) {
                return BlockType.this.isRedstoneProvider() ? BlockType.this.getStrongPower(world, new Vec3i(pos), Facing.from(side)) : 0;
            }
            return 0;
        }

        @Override
        public boolean isSignalSource(BlockState state)
        {
            return BlockType.this.isRedstoneProvider();
        }

        /* TODO 1.15.2
        @Override
        public BlockRenderLayer getRenderLayer() {
            return BlockRenderLayer.CUTOUT_MIPPED;
        }
         */

        @Override
        public boolean useShapeForLightOcclusion(BlockState state) {
            return true;
        }

        @Override
        public boolean propagatesSkylightDown(BlockState state, IBlockReader reader, BlockPos pos) {
            return true;
        }

        @Override
        public VoxelShape getOcclusionShape(BlockState state, IBlockReader worldIn, BlockPos pos) {
            return VoxelShapes.empty();
        }
    }
}
