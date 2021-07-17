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
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

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
        CommonEvents.Block.REGISTER.subscribe(() -> Registry.register(Registry.BLOCK, id.internal, internal));
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
    protected static final IBoundingBox defaultBox = IBoundingBox.from(new Box(0, 0, 0, 1, 1, 1));
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
            super(FabricBlockSettings
                    .of(BlockType.this.getMaterial().internal)
                    .sounds(BlockType.this.getMaterial().soundType)
                    .hardness(BlockType.this.getHardness())
                    .resistance(BlockType.this.getExplosionResistance())
                    .dynamicBounds()
                    );
        }

        /** Called server side at the end of the block break call chain as cleanup */
        @Override
        public void onBlockRemoved(BlockState blockState_1, net.minecraft.world.World world, BlockPos pos, BlockState blockState_2, boolean boolean_1) {
            BlockType.this.onBreak(World.get(world), new Vec3i(pos));
            super.onBlockRemoved(blockState_1, world, pos, blockState_2, boolean_1);
        }

        @Override
        public boolean activate(BlockState state, net.minecraft.world.World world, BlockPos pos, PlayerEntity player, net.minecraft.util.Hand hand, BlockHitResult hit) {
            return BlockType.this.onClick(World.get(world), new Vec3i(pos), new Player(player), Player.Hand.from(hand), Facing.from(hit.getSide()), new Vec3d(hit.getPos()).subtract(new Vec3i(pos)));
        }

        @Override
        public net.minecraft.item.ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
            return BlockType.this.onPick(World.get((net.minecraft.world.World)world), new Vec3i(pos)).internal;
        }

        @Override
        public void neighborUpdate(BlockState blockState_1, net.minecraft.world.World world, BlockPos pos, Block block_1, BlockPos neighbor, boolean boolean_1) {
            BlockType.this.onNeighborChange(World.get(world), new Vec3i(pos), new Vec3i(neighbor));
        }

        /*
        Overrides
         */

        @Override
        public final BlockRenderType getRenderType(BlockState state) {
            // TESR Renderer TODO OPTIONAL!@!!!!
            return BlockRenderType.MODEL;
        }

        protected World getWorldOrNull(BlockView source, BlockPos pos) {
            return source instanceof net.minecraft.world.World ? World.get((net.minecraft.world.World) source) : null;
        }


        @Override
        public boolean isOpaque(BlockState blockState_1) {
            return false;
        }

        private final SingleCache<IBoundingBox, VoxelShape> bbCache = new SingleCache<>((IBoundingBox box) -> VoxelShapes.cuboid(BoundingBox.from(box)));
        @Override
        public VoxelShape getOutlineShape(BlockState state, BlockView worldIn, BlockPos pos, EntityContext context) {
            World world = getWorldOrNull(worldIn, pos);
            if (world != null) {
                return bbCache.get(BlockType.this.getBoundingBox(world, new Vec3i(pos)));
            }
            return super.getOutlineShape(state, worldIn, pos, context);
        }

        /*
         * Fence, glass override
         */
        /*
        @Override
        public boolean canBeConnectedTo(IBlockAccess internal, BlockPos pos, EnumFacing facing) {
            return BlockType.this.isConnectable();
        }
        */

        public boolean tryBreak(net.minecraft.world.World world, BlockPos pos, PlayerEntity player) {
            return BlockType.this.tryBreak(World.get(world), new Vec3i(pos), new Player(player));
        }

        /* Redstone */

        @Override
        public int getWeakRedstonePower(BlockState blockState, BlockView blockAccess, BlockPos pos, Direction side)
        {
            World world = getWorldOrNull(blockAccess, pos);
            if (world != null) {
                return BlockType.this.isRedstoneProvider() ? BlockType.this.getWeakPower(world, new Vec3i(pos), Facing.from(side)) : 0;
            }
            return 0;
        }

        @Override
        public int getStrongRedstonePower(BlockState blockState, BlockView blockAccess, BlockPos pos, Direction side)
        {
            World world = getWorldOrNull(blockAccess, pos);
            if (world != null) {
                return BlockType.this.isRedstoneProvider() ? BlockType.this.getStrongPower(world, new Vec3i(pos), Facing.from(side)) : 0;
            }
            return 0;
        }

        @Override
        public boolean emitsRedstonePower(BlockState state)
        {
            return BlockType.this.isRedstoneProvider();
        }

        /* TODO
        @SideOnly(Side.CLIENT)
        public BlockRenderLayer getBlockLayer() {
            return BlockRenderLayer.CUTOUT_MIPPED;
        }
        */

        @Override
        public boolean hasSidedTransparency(BlockState state) {
            return true;
        }

        @Override
        public boolean isTranslucent(BlockState state, BlockView view, BlockPos pos) {
            return true;
        }

        @Override
        public VoxelShape getCullingShape(BlockState state, BlockView view, BlockPos pos) {
            return VoxelShapes.empty();
        }
    }
}
