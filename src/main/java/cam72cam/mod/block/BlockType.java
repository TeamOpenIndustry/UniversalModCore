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
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** A standard block with no attached entity */
public abstract class BlockType {
    public static Map<BlockType, Integer> blocks = new HashMap<>();

    /*
    Hook into the Block Broken event (not specific per block)
     */
    static {
        CommonEvents.Block.BROKEN.subscribe((world, pos, player) -> {
            net.minecraft.block.Block block = world.getBlock(pos.x, pos.y, pos.z);
            if (block instanceof BlockInternal) {
                return ((BlockInternal) block).tryBreak(world, pos.x, pos.y, pos.z, player);
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
        CommonEvents.Block.REGISTER.subscribe(() -> GameRegistry.registerBlock(internal, new ResourceLocation(modID, name).toString()));
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
    protected static final IBoundingBox defaultBox = IBoundingBox.from(AxisAlignedBB.getBoundingBox(0, 0, 0, 1, 1, 1));
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
            setStepSound(type.getMaterial().soundType);
            setUnlocalizedName(type.id.toString());
            // REMOVED 1.7.10 setRegistryName(new ResourceLocation(type.id.internal));
        }

        /** Called server side at the end of the block break call chain as cleanup */
        @Override
        public int getRenderType() {
            return blocks.getOrDefault(BlockType.this, -1);
        }

        public boolean renderAsNormalBlock() {
            return false;
        }

        @Override
        public final void breakBlock(net.minecraft.world.World world, int posX, int posY, int posZ, Block block, int meta) {
            BlockType.this.onBreak(World.get(world), new Vec3i(posX, posY, posZ));
            super.breakBlock(world, posX, posY, posZ, block, meta);
        }

        /** Called both client and server side when a player right clicks on a block.  Can cancel the event by returning true (handled) */
        @Override
        public final boolean onBlockActivated(net.minecraft.world.World world, int posX, int posY, int posZ, EntityPlayer player, int facing, float hitX, float hitY, float hitZ) {
            return BlockType.this.onClick(World.get(world), new Vec3i(posX, posY, posZ), new Player(player), Player.Hand.PRIMARY, Facing.from(EnumFacing.getFront(facing)), new Vec3d(hitX, hitY, hitZ));
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
        public void onNeighborChange(IBlockAccess blockAccess, int posX, int posY, int posZ, int neighborX, int neighborY, int neighborZ) {
            World world = getWorldOrNull(blockAccess, posX, posY, posZ);
            if (world != null) {
                BlockType.this.onNeighborChange(world, new Vec3i(posX, posY, posZ), new Vec3i(neighborX, neighborY, neighborZ));
            }
        }

        /*
        Overrides
         */
        @Override
        public final float getExplosionResistance(Entity exploder) {
            return BlockType.this.getExplosionResistance();
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

        protected World getWorldOrNull(IBlockAccess source, int posX, int posY, int posZ) {
            return source instanceof net.minecraft.world.World ? World.get((net.minecraft.world.World)source) : null;
        }


        @Override
        public AxisAlignedBB getCollisionBoundingBoxFromPool(net.minecraft.world.World p_149668_1_, int p_149668_2_, int p_149668_3_, int p_149668_4_) {
            World world = getWorldOrNull(p_149668_1_, p_149668_2_, p_149668_3_, p_149668_4_);
            if (world != null) {
                IBoundingBox box = getBoundingBox(world, new Vec3i(p_149668_2_, p_149668_3_, p_149668_4_));
                return BoundingBox.from(box).copy().offset(p_149668_2_, p_149668_3_, p_149668_4_);
            }
            return super.getCollisionBoundingBoxFromPool(p_149668_1_, p_149668_2_, p_149668_3_, p_149668_4_);
        }

        public void setBlockBoundsBasedOnState(IBlockAccess p_149719_1_, int p_149719_2_, int p_149719_3_, int p_149719_4_) {
            World world = getWorldOrNull(p_149719_1_, p_149719_2_, p_149719_3_, p_149719_4_);
            if (world != null) {
                IBoundingBox box = getBoundingBox(world, new Vec3i(p_149719_2_, p_149719_3_, p_149719_4_));
                minX = box.min().x;
                minY = box.min().y;
                minZ = box.min().z;
                maxX = box.max().x;
                maxY = box.max().y;
                maxZ = box.max().z;
            }
        }

        /*
        @Override
        public AxisAlignedBB getSelectedBoundingBoxFromPool(net.minecraft.world.World worldIn, int posX, int posY, int posZ) {
            return getCollisionBoundingBoxFromPool(worldIn, posX, posY, posZ);
        }*/

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
            if (BlockType.this.isConnectable()) {
                return super.isSideSolid(world, posX, posY, posZ, side);
            }

            // SNOW ONLY?
            return side == ForgeDirection.UP;
        }

        public boolean tryBreak(net.minecraft.world.World world, int posX, int posY, int posZ, EntityPlayer player) {
            return BlockType.this.tryBreak(World.get(world), new Vec3i(posX, posY, posZ), new Player(player));
        }

        /* Redstone */

        @Override
        public int isProvidingWeakPower(IBlockAccess blockAccess, int posX, int posY, int posZ, int side)
        {
            World world = getWorldOrNull(blockAccess, posX, posY, posZ);
            if (world != null) {
                return BlockType.this.isRedstoneProvider() ? BlockType.this.getWeakPower(world, new Vec3i(posX, posY, posZ), Facing.from((byte) side)) : 0;
            }
            return 0;
        }

        @Override
        public int isProvidingStrongPower(IBlockAccess blockAccess, int posX, int posY, int posZ, int side)
        {
            World world = getWorldOrNull(blockAccess, posX, posY, posZ);
            if (world != null) {
                return BlockType.this.isRedstoneProvider() ? BlockType.this.getStrongPower(world, new Vec3i(posX, posY, posZ), Facing.from((byte) side)) : 0;
            }
            return 0;
        }

        @Override
        public boolean canProvidePower()
        {
            return BlockType.this.isRedstoneProvider();
        }

        /* TODO
        @SideOnly(Side.CLIENT)
        public BlockRenderLayer getBlockLayer() {
            return BlockRenderLayer.CUTOUT_MIPPED;
        }
        */

        public int quantityDropped(Random p_149745_1_)
        {
            return 0;
        }

        public Item getItemDropped(int p_149650_1_, Random p_149650_2_, int p_149650_3_)
        {
            return Item.getItemFromBlock(Blocks.air);
        }

    }
}
