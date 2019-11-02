package cam72cam.mod.block.tile;

import cam72cam.mod.ModCore;
import cam72cam.mod.block.BlockEntity;
import cam72cam.mod.block.BlockEntityTickable;
import cam72cam.mod.resource.Identifier;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.Tickable;

import java.util.function.Supplier;

public class TileEntityTickable extends TileEntity implements Tickable {
    public TileEntityTickable(Identifier id) {
        super(id);
    }

    @Override
    public void tick() {
        BlockEntityTickable tickable = (BlockEntityTickable) instance();
        if (tickable == null) {
            System.out.println("uhhhhh, null tickable?");
            return;
        }
        tickable.update();
    }

    @Override
    public Identifier getName() {
        return new Identifier(ModCore.MODID, "hack_tickable");
    }
}
