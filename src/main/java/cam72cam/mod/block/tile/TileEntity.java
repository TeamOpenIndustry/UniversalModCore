package cam72cam.mod.block.tile;

import cam72cam.mod.ModCore;
import cam72cam.mod.block.BlockEntity;
import cam72cam.mod.energy.IEnergy;
import cam72cam.mod.fluid.ITank;
import cam72cam.mod.item.IInventory;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.serialization.SerializationException;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.TagSerializer;
import cam72cam.mod.util.Facing;
import cam72cam.mod.world.World;
import com.google.common.collect.HashBiMap;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.registry.Registry;

import java.util.HashSet;
import java.util.Map;
import java.util.function.Supplier;

/**
 * TileEntity is an internal class that should only be extended when you need to implement
 * an interface.
 *
 * If you need to create a standard tile entity and wound up here, take a look at BlockEntity instead.
 *
 * @see BlockEntity
 */
public class TileEntity extends net.minecraft.block.entity.BlockEntity {
    private static final Map<String, BlockEntityType<? extends TileEntity>> types = HashBiMap.create();
    // InstanceId -> Supplier mapping
    private static final Map<String, Supplier<BlockEntity>> registry = HashBiMap.create();

    // Set during initialization
    private final BlockEntity instance;
    public boolean hasTileData;

    // Cached
    private Vec3i umcPos;
    private World umcWorld;

    /**
     * Used only by BlockEntity to construct an instance to register.
     * <ul>
     *     <li>Must be implemented in subclasses.</li>
     *     <li>Do not use directly.</li>
     * </ul>
     *
     * @see BlockEntity
     * @param id Block Entity ID
     */
    public TileEntity(Identifier id) {
        super(types.get(id.toString()));
        instance = registry.get(id.toString()).get();
        instance.internal = this;
    }

    /**
     * Allows us to construct a BlockEntity from an identifier.  Do not use directly.
     * @param instance constructor
     * @param id Block Entity ID
     */
    public static BlockEntityType<? extends TileEntity> register(Supplier<BlockEntity> instance, Identifier id) {
        registry.put(id.toString(), instance);

        BlockEntity example = instance.get();

        BlockEntityType<? extends TileEntity> type = Registry.register(Registry.BLOCK_ENTITY, id.internal, new BlockEntityType<>(() -> example.supplier(id), new HashSet<net.minecraft.block.Block>() {
            public boolean contains(Object var1) {
                // WHYYYYYYYYYYYYYYYY
                return true;
            }
        }, null));
        types.put(id.toString(), type);
        return type;
    }

    /** Wrap getPos() in a cached UMC Vec3i */
    public Vec3i getUMCPos() {
        if (umcPos == null || !umcPos.internal().equals(pos)) {
            umcPos = new Vec3i(pos);
        }
        return umcPos;
    }

    /** Wrap getWorld in cached UMC World */
    public World getUMCWorld() {
        if (umcWorld == null || umcWorld.internal != world) {
            umcWorld = World.get(world);
        }
        return umcWorld;
    }

    /*
    Standard Tile function overrides
    */

    /**
     * Initialize instance (if possible), deserialize into the instance and call the load method (explicit load)
     * @see TagSerializer
     */
    @Override
    public final void fromTag(CompoundTag compound) {
        super.fromTag(compound);
        hasTileData = true;

        TagCompound data = new TagCompound(compound);
        TagCompound instanceData = data.get("instanceData");
        if (instanceData == null) {
            // Legacy fallback
            instanceData = data;
        }

        try {
            TagSerializer.deserialize(instanceData, instance);
            instance.load(instanceData);
            if (compound.contains("umcUpdate")) {
                instance.readUpdate(new TagCompound(compound).get("umcUpdate"));
            }
        } catch (SerializationException e) {
            // TODO how should we handle this?
            throw new RuntimeException(e);
        }
    }

    /**
     * Serialize into the instance and call the save method (explicit save)
     * @see TagSerializer
     */
    @Override
    public final CompoundTag toTag(CompoundTag compound) {
        super.toTag(compound);

        TagCompound data = new TagCompound(compound);

        if (instance() != null) {
            TagCompound instanceData = new TagCompound();
            try {
                TagSerializer.serialize(instanceData, instance);
                instance.save(instanceData);
            } catch (SerializationException e) {
                // TODO how should we handle this?
                throw new RuntimeException(e);
            }
            data.set("instanceData", instanceData);
        }
        return compound;
    }

    /** Active Synchronization from markDirty */
    public BlockEntityUpdateS2CPacket toUpdatePacket() {
        return new BlockEntityUpdateS2CPacket(pos, 1, toInitialChunkDataTag());
    }


    /** Active Synchronization from markDirty */
    public CompoundTag toInitialChunkDataTag() {
        TagCompound tag = new TagCompound();
        if (this.isLoaded()) {
            this.toTag(tag.internal);
            TagCompound umcUpdate = new TagCompound();
            try {
                instance().writeUpdate(umcUpdate);
            } catch (SerializationException e) {
                ModCore.catching(e);
            }
            tag.set("umcUpdate", umcUpdate);
        }
        return tag.internal;
    }

    /** Fire off update packet if on server, re-render if on client */
    @Override
    public void markDirty() {
        super.markDirty();
        if (!world.isClient) {
            world.updateListeners(getPos(), world.getBlockState(getPos()), world.getBlockState(getPos()), 1 + 2 + 8);
            world.updateNeighborsAlways(pos, world.getBlockState(pos).getBlock());
        } else {
            world.checkBlockRerender(getPos(), null, super.world.getBlockState(pos));
        }
    }

    /**
     * @return Instance's render distance
     * @see BlockEntity
     */
    @Override
    public double getSquaredRenderDistance() {
        return instance() != null ? instance().getRenderDistance() * instance().getRenderDistance() : Integer.MAX_VALUE;
    }

    /*
    private final SingleCache<IBoundingBox, AxisAlignedBB> bbCache =
            new SingleCache<>(internal -> BoundingBox.from(internal).offset(pos.getX(), pos.getY(), pos.getZ()));*/
    /**
     * @return Instance's bounding box
     * @see BlockEntity
     *//*
    @Override
    public net.minecraft.util.math.AxisAlignedBB getRenderBoundingBox() {
        if (instance() != null) {
            return bbCache.get(instance().getRenderBoundingBox());
        }
        return INFINITE_EXTENT_AABB;
    }*/


    /*
    New Functionality
    */

    /** @return If the BlockEntity instance is loaded */
    public boolean isLoaded() {
        return world != null && (!world.isClient || hasTileData);
    }

    /** @return The instance of the BlockEntity if possible */
    public BlockEntity instance() {
        return isLoaded() ? instance : null;
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
