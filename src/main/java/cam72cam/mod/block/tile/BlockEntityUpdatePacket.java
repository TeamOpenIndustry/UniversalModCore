package cam72cam.mod.block.tile;

import cam72cam.mod.block.BlockEntity;
import cam72cam.mod.net.Packet;

public class BlockEntityUpdatePacket extends Packet {
    public BlockEntityUpdatePacket() {
    }

    public BlockEntityUpdatePacket(TileEntity entity) {
        data.setVec3i("pos", entity.pos);
        data.set("data", entity.getUpdateTag());
    }

    @Override
    protected void handle() {
        BlockEntity entity = getPlayer().getWorld().getBlockEntity(data.getVec3i("pos"), BlockEntity.class);
        if (entity != null) {
            entity.internal.handleUpdateTag(data.get("data"));
        }
    }
}
