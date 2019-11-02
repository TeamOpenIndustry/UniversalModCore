package cam72cam.mod.block;

import cam72cam.mod.entity.Player;
import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Facing;
import cam72cam.mod.util.Hand;
import cam72cam.mod.world.World;
import net.fabricmc.fabric.api.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

public abstract class BlockType {
    public final net.minecraft.block.Block internal;
    protected final BlockSettings settings;
    public final Identifier identifier;

    public BlockType(BlockSettings settings) {
        this.settings = settings;

        internal = getBlock();

        identifier = new Identifier(settings.modID, settings.name);

        CommonEvents.Block.REGISTER.subscribe(() -> Registry.register(Registry.BLOCK, identifier, internal));

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

    public int getStrongPower(World world, Vec3i vec3i, Facing from) {
        return 0;
    }

    public int getWeakPower(World world, Vec3i vec3i, Facing from) {
        return 0;
    }

    protected class BlockInternal extends net.minecraft.block.Block {
        public BlockInternal() {
            super(FabricBlockSettings
                    .of(settings.material.internal)
                    .sounds(settings.material.soundType)
                    .hardness(settings.hardness)
                    .resistance(settings.resistance)
                    .dynamicBounds()
                    .build());
        }

        @Override
        public void onBlockRemoved(BlockState blockState_1, net.minecraft.world.World world, BlockPos pos, BlockState blockState_2, boolean boolean_1) {
            BlockType.this.onBreak(World.get(world), new Vec3i(pos));
            super.onBlockRemoved(blockState_1, world, pos, blockState_2, boolean_1);
        }

        @Override
        public boolean activate(BlockState state, net.minecraft.world.World world, BlockPos pos, PlayerEntity player, net.minecraft.util.Hand hand, BlockHitResult hit) {
            return BlockType.this.onClick(World.get(world), new Vec3i(pos), new Player(player), Hand.from(hand), Facing.from(hit.getSide()), new Vec3d(hit.getPos()));
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


        @Override
        public boolean isOpaque(BlockState blockState_1) {
            return false;
        }

        @Override
        public VoxelShape getCollisionShape(BlockState blockState_1, BlockView blockView_1, BlockPos blockPos_1, EntityContext entityContext_1) {
            return Block.createCuboidShape(0, 0, 0, 16, BlockType.this.getHeight()*16, 16);
        }

        @Override
        public VoxelShape getRayTraceShape(BlockState blockState_1, BlockView blockView_1, BlockPos blockPos_1) {
            return Block.createCuboidShape(0, 0, 0, 16, BlockType.this.getHeight()*16, 16);
        }

        @Override
        public VoxelShape getOutlineShape(BlockState blockState_1, BlockView blockView_1, BlockPos blockPos_1, EntityContext entityContext_1) {
            return Block.createCuboidShape(0, 0, 0, 16, BlockType.this.getHeight()*16, 16);
        }

        /*
         * Fence, glass override
         */
        /*
        @Override
        public boolean canBeConnectedTo(BlockView internal, BlockPos pos, Direction facing) {
            return settings.connectable;
        }
        */

        public boolean tryBreak(net.minecraft.world.World world, BlockPos pos, PlayerEntity player) {
            return BlockType.this.tryBreak(World.get(world), new Vec3i(pos), new Player(player));
        }

        /* Redstone */

        @Override
        public int getWeakRedstonePower(BlockState blockState, BlockView blockAccess, BlockPos pos, Direction side)
        {
            return settings.redstoneProvider ? BlockType.this.getWeakPower(World.get((net.minecraft.world.World)blockAccess), new Vec3i(pos), Facing.from(side)) : 0;
        }

        @Override
        public int getStrongRedstonePower(BlockState blockState, BlockView blockAccess, BlockPos pos, Direction side)
        {
            return settings.redstoneProvider ? BlockType.this.getStrongPower(World.get((net.minecraft.world.World)blockAccess), new Vec3i(pos), Facing.from(side)) : 0;
        }

        @Override
        public boolean emitsRedstonePower(BlockState state)
        {
            return settings.redstoneProvider;
        }

        /* TODO
        @SideOnly(Side.CLIENT)
        public BlockRenderLayer getBlockLayer() {
            return BlockRenderLayer.CUTOUT_MIPPED;
        }
        */

    }
}
