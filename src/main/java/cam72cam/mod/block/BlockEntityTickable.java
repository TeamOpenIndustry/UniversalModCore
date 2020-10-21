package cam72cam.mod.block;

import cam72cam.mod.block.tile.TileEntity;
import cam72cam.mod.block.tile.TileEntityTickable;
import cam72cam.mod.resource.Identifier;

/** Wraps BlockEntity and exposes an update function which is called every tick */
public abstract class BlockEntityTickable extends BlockEntity {
    /** Called every tick */
    public abstract void update();

    protected TileEntity supplier(Identifier id) {
        return new TileEntityTickable(id);
    }
}
