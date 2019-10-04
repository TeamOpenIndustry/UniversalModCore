package cam72cam.mod.block.tile;

import cam72cam.mod.ModCore;
import cam72cam.mod.block.BlockEntity;
import cam72cam.mod.block.BlockEntityTickable;
import cam72cam.mod.resource.Identifier;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.Tickable;

import java.util.function.Supplier;

public class TileEntityTickable extends TileEntity implements Tickable {
    protected TileEntityTickable(Supplier<? extends BlockEntity> ctr) {
        super(ctr);
    }

    public static BlockEntityType<? extends TileEntity> register(Identifier id, Supplier<BlockEntity> ctr) {
        return register(id, ctr, TileEntityTickable::new);
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
