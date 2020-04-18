package cam72cam.mod.block;

import cam72cam.mod.block.tile.TileEntity;
import cam72cam.mod.energy.IEnergy;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.fluid.ITank;
import cam72cam.mod.item.IInventory;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.serialization.SerializationException;
import cam72cam.mod.util.Facing;
import cam72cam.mod.util.Hand;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.world.World;

public abstract class BlockEntity {
    public TileEntity internal;
    public World world;
    public Vec3i pos;

    public void load(TagCompound nbt) throws SerializationException {
    }

    public void save(TagCompound nbt) throws SerializationException {
    }

    public void writeUpdate(TagCompound nbt) throws SerializationException {
    }

    public void readUpdate(TagCompound nbt) throws SerializationException {
    }

    public void onBreak() {
    }

    public boolean onClick(Player player, Hand hand, Facing facing, Vec3d hit) {
        return false;
    }

    public abstract ItemStack onPick();

    public void onNeighborChange(Vec3i neighbor) {
    }

    public double getHeight() {
        return 1;
    }

    public void markDirty() {
        internal.markDirty();
    }

    public TagCompound getData() {
        TagCompound data = new TagCompound();
        internal.save(data);
        return data;
    }

    public IInventory getInventory(Facing side) {
        return null;
    }

    public ITank getTank(Facing side) {
        return null;
    }

    public IEnergy getEnergy(Facing side) {
        return null;
    }

    public boolean tryBreak(Player player) {
        return true;
    }

    public IBoundingBox getBoundingBox() {
        return null;
    }

    public double getRenderDistance() {
        return 4096.0D; // MC default
    }

    protected TileEntity supplier(Identifier id) {
        return new TileEntity(id);
    }
}
