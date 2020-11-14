package cam72cam.mod.block.tile;

import cam72cam.mod.ModCore;
import cam72cam.mod.block.BlockEntity;
import cam72cam.mod.block.BlockEntityTickable;
import cam72cam.mod.resource.Identifier;
import net.minecraft.util.Tickable;
/**
 * TileEntityTickable is an internal class which wraps TileEntity and implements ITickable.  Is paired with
 * BlockEntityTickable.
 *
 * If you need to create a standard tile entity and wound up here, take a look at BlockEntityTickable instead.
 *
 * @see BlockEntityTickable
 */
public class TileEntityTickable extends TileEntity implements Tickable {

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
    public TileEntityTickable(Identifier id) {
        super(id);
    }

    private BlockEntityTickable tickable;
    @Override
    public void tick() {
        if (tickable == null) {
            tickable = (BlockEntityTickable) instance();
            if (tickable == null) {
                ModCore.debug("uhhhhh, null tickable?");
                return;
            }
        }
        tickable.update();
    }
}
