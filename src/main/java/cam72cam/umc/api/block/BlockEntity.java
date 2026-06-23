package cam72cam.umc.api.block;

import cam72cam.umc.api.block.tile.TileEntity;
import cam72cam.umc.api.energy.IEnergy;
import cam72cam.umc.api.entity.Player;
import cam72cam.umc.api.entity.boundingbox.IBoundingBox;
import cam72cam.umc.api.fluid.ITank;
import cam72cam.umc.api.item.IInventory;
import cam72cam.umc.api.item.ItemStack;
import cam72cam.umc.api.math.Vec3d;
import cam72cam.umc.api.math.Vec3i;
import cam72cam.umc.api.resource.Identifier;
import cam72cam.umc.api.serialization.SerializationException;
import cam72cam.umc.api.util.Facing;
import cam72cam.umc.api.serialization.TagCompound;
import cam72cam.umc.api.world.World;
import cam72cam.umc.api.serialization.TagSerializer;

/**
 * Block entity is the building block for more functional blocks in game.  It allows you to save complex data and
 * expose certain capabilities (Fluid/Item/Energy/etc...).
 */
public abstract class BlockEntity {
    /** Wraps the minecraft construct, do not use directly. */
    public TileEntity internal;

    /** @return the current world. */
    public World getWorld() {
        return internal.getUMCWorld();
    }

    /** @return the current position. */
    public Vec3i getPos() {
        return internal.getUMCPos();
    }

    /**
     * Use for explicit loading, prefer TagSerializer.<br>
     * <br>
     * Called on the server during chunk load.<br>
     * Called on the client during packet synchronization.
     *
     * @see TagSerializer
     */
    public void load(TagCompound nbt) throws SerializationException {
    }

    /**
     * Use for explicit saving, prefer TagSerializer.<br>
     * Called on the server during chunk save.
     *
     * @see TagSerializer
     */
    public void save(TagCompound nbt) throws SerializationException {
    }

    /**
     * Called on the server to gather additional (non-persistent) data that
     * should be sent to the client.
     */
    public void writeUpdate(TagCompound nbt) throws SerializationException {
    }

    /**
     * Called on the client to apply additional (non-persistent) data that was
     * sent from the server.
     */
    public void readUpdate(TagCompound nbt) throws SerializationException {
    }

    /**
     * Called when the block containing this entity is broken.
     *
     * @see #tryBreak
     */
    public void onBreak() {
    }

    /**
     * Called when a player right-clicks a block.
     * @return true if this accepts the click (stop processing it after this call)
     */
    public boolean onClick(Player player, Player.Hand hand, Facing facing, Vec3d hit) {
        return false;
    }

    /**
     * Called when a player performs the pick operation on this block.
     * @return An ItemStack representing this block.  Must NOT be null, return ItemStack.EMPTY instead.
     */
    public abstract ItemStack onPick();

    /** Called when a neighboring block has changed state */
    public void onNeighborChange(Vec3i neighbor) {
    }

    /** Marks block for re-render on the client or sends an update packet on the server. */
    public void markDirty() {
        internal.markDirty();
    }

    /** @return the data that would be written to disk on world save */
    public TagCompound getData() {
        TagCompound data = new TagCompound();
        internal.writeToNBT(data.internal);
        return data;
    }

    /** Implement to support inventory capabilities */
    public IInventory getInventory(Facing side) {
        return null;
    }

    /** Implement to support Tank capabilities */
    public ITank getTank(Facing side) {
        return null;
    }

    /** Implement to support energy capabilities */
    public IEnergy getEnergy(Facing side) {
        return null;
    }

    /** @return false if the in-progress break should be cancelled */
    public boolean tryBreak(Player player) {
        return true;
    }

    /** @return Bounding Box (offset from origin) */
    public IBoundingBox getBoundingBox() {
        return BlockType.defaultBox;
    }

    /** @return Bounding Box (offset from origin) */
    public IBoundingBox getRenderBoundingBox() {
        return getBoundingBox();
    }

    /** @return Max render distance for this entity */
    public double getRenderDistance() {
        return 4096.0D; // MC default
    }

    /**
     * Implement only when defining a new type of internal TileEntity
     * @param id BlockTypeEntity identifier
     * @return A new instance of the custom TileEntity
     */
    protected TileEntity supplier(Identifier id) {
        return new TileEntity(id);
    }
}
