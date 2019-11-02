package cam72cam.mod.block.tile;

import cam72cam.mod.ModCore;
import cam72cam.mod.block.BlockEntity;
import cam72cam.mod.energy.IEnergy;
import cam72cam.mod.fluid.ITank;
import cam72cam.mod.item.IInventory;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.util.Facing;
import cam72cam.mod.util.TagCompound;
import cam72cam.mod.world.World;
import com.google.common.collect.HashBiMap;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.network.packet.BlockEntityUpdateS2CPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;

import java.util.HashSet;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class TileEntity extends net.minecraft.block.entity.BlockEntity {
    private static final Map<String, BlockEntityType<? extends TileEntity>> types = HashBiMap.create();
    private static final Map<String, Supplier<BlockEntity>> registry = HashBiMap.create();
    public World world;
    public Vec3i pos;
    public boolean hasTileData;

    /*
    Tile registration
    */
    private final BlockEntity instance;

    public TileEntity(Identifier id) {
        super(types.get(id.toString()));
        instance = registry.get(id.toString()).get();
    }

    public static BlockEntityType<? extends TileEntity> register(Supplier<BlockEntity> ctr, Supplier<TileEntity> teCtr, Identifier id) {
        registry.put(id.toString(), ctr);

        //BlockEntityType<? extends TileEntity> type = Registry.register(Registry.BLOCK_ENTITY, id.internal, BlockEntityType.Builder.create(() -> tctr.apply(ctr)).build(null));
        BlockEntityType<? extends TileEntity> type = Registry.register(Registry.BLOCK_ENTITY, id.internal, new BlockEntityType<>(teCtr, new HashSet<net.minecraft.block.Block>() {
            public boolean contains(Object var1) {
                // WHYYYYYYYYYYYYYYYY
                return true;
            }
        }, null));
        types.put(id.toString(), type);
        return type;
    }

    public static TileEntity create(Identifier id) {
        return types.get(id).instantiate();
    }

    public Identifier getName() {
        return new Identifier(ModCore.MODID, "hack");
    }


    /*
    Standard Tile function overrides
    */

    @Override
    public void setWorld(net.minecraft.world.World world) {
        super.setWorld(world);
        this.world = World.get(world);
    }

    @Override
    public void setPos(BlockPos pos) {
        super.setPos(pos);
        this.pos = new Vec3i(pos);
    }


    @Override
    public final void fromTag(CompoundTag compound) {
        hasTileData = true;
        load(new TagCompound(compound));
        if (compound.getBoolean("isUpdate")) {
            hasTileData = true;
            readUpdate(new TagCompound(compound));
        }
    }

    @Override
    public final CompoundTag toTag(CompoundTag compound) {
        save(new TagCompound(compound));
        return compound;
    }

    public CompoundTag toInitialChunkDataTag() {
        TagCompound data = new TagCompound(super.toInitialChunkDataTag());
        save(data);
        data.setBoolean("isUpdate", true);
        return data.internal;
    }

    public final TagCompound getUpdateTag() {
        TagCompound tag = new TagCompound();
        if (this.isLoaded()) {
            this.toTag(tag.internal);
            this.writeUpdate(tag);
        }
        return tag;
    }

    public final void handleUpdateTag(TagCompound tag) {
        hasTileData = true;
        this.fromTag(tag.internal);
        this.readUpdate(tag);
        world.internal.scheduleBlockRender(getPos(), null, super.world.getBlockState(super.pos));
    }
    public BlockEntityUpdateS2CPacket toUpdatePacket() {
        new BlockEntityUpdatePacket(this).sendToAllAround(world, new Vec3d(pos), 8*16);
        return super.toUpdatePacket();
    }

    @Override
    public void markDirty() {
        super.markDirty();
        if (world.isServer) {
            world.internal.updateListeners(getPos(), world.internal.getBlockState(getPos()), world.internal.getBlockState(getPos()), 1 + 2 + 8);
            world.internal.updateNeighborsAlways(pos.internal, super.world.getBlockState(super.pos).getBlock());
        } else {
            world.internal.scheduleBlockRender(getPos(), null, super.world.getBlockState(super.pos));
        }
    }

    /* Forge Overrides */

    @Override
    public double getSquaredRenderDistance() {
        return instance() != null ? instance().getRenderDistance() * instance().getRenderDistance() : Integer.MAX_VALUE;
    }

    /*
    Wrapped functionality
    */

    public void setWorld(World world) {
        super.setWorld(world.internal);
    }

    public void setPos(Vec3i pos) {
        super.setPos(pos.internal);
    }

    public void load(TagCompound data) {
        super.fromTag(data.internal);
        pos = new Vec3i(super.pos);
        instance.internal = this;
        instance.world = world;
        instance.pos = pos;

        instance.load(data);
    }

    public void save(TagCompound data) {
        super.toTag(data.internal);
        instance.save(data);
    }

    public void writeUpdate(TagCompound nbt) {
        if (instance() != null) {
            instance().writeUpdate(nbt);
        }
    }

    public void readUpdate(TagCompound nbt) {
        if (instance() != null) {
            instance().readUpdate(nbt);
        }
    }

    /*
    New Functionality
    */

    public boolean isLoaded() {
        return this.hasWorld() && (world.isServer || hasTileData);
    }

    public BlockEntity instance() {
        if (isLoaded()) {
            this.instance.internal = this;
            this.instance.world = this.world;
            this.instance.pos = this.pos;
            return this.instance;
        }
        return null;
    }

    /* Capabilities */

    public IInventory getInventory(Facing side) {
        return instance() != null ? instance().getInventory(side) : null;
    }

    public ITank getTank(Facing side) {
        return instance() != null ? instance().getTank(side) : null;
    }

    public IEnergy getEnergy(Facing side) {
        return instance() != null ? instance().getEnergy(side) : null;
    }
}
