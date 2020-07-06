package cam72cam.mod.block.tile;

import cam72cam.mod.math.Vec3i;
import cam72cam.mod.net.Packet;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.TagField;

public class BlockEntityUpdatePacket extends Packet {
    @TagField
    private Vec3i pos;
    @TagField
    private TagCompound data;

    public BlockEntityUpdatePacket() {
    }

    public BlockEntityUpdatePacket(TileEntity entity) {
        pos = new Vec3i(entity.getPos());
        data = entity.getUpdateTag();
    }

    @Override
    protected void handle() {
        net.minecraft.block.entity.BlockEntity te = getWorld().internal.getBlockEntity(pos.internal);
        if (te instanceof TileEntity) {
            ((TileEntity)te).handleUpdateTag(data);
        }
    }
}
