package cam72cam.mod.block.tile;

import cam72cam.mod.ModCore;
import cam72cam.mod.block.BlockEntity;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.util.ITrack;
import net.minecraft.block.entity.BlockEntityType;

import java.util.function.Supplier;

public class TileEntityTickableTrack extends TileEntityTickable implements trackapi.lib.ITrack {

    public TileEntityTickableTrack(Supplier<? extends BlockEntity> ctr) {
        super(ctr);
    }

    public static BlockEntityType<? extends TileEntity> register(Identifier id, Supplier<BlockEntity> ctr) {
        return register(id, ctr, TileEntityTickable::new);
    }


    private trackapi.lib.ITrack track() {
        return instance() instanceof ITrack ? ((ITrack) instance()).to() : null;
    }

    @Override
    public double getTrackGauge() {
        return track() != null ? track().getTrackGauge() : 0;
    }

    @Override
    public net.minecraft.util.math.Vec3d getNextPosition(net.minecraft.util.math.Vec3d pos, net.minecraft.util.math.Vec3d mot) {
        return track() != null ? track().getNextPosition(pos, mot) : pos;
    }

    @Override
    public Identifier getName() {
        return new Identifier(ModCore.MODID, "tile_track");
    }
}
