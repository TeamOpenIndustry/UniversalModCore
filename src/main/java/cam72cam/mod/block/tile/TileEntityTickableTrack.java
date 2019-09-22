package cam72cam.mod.block.tile;

import cam72cam.mod.ModCore;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.util.ITrack;
import net.minecraft.util.Vec3;

public class TileEntityTickableTrack extends TileEntityTickable implements trackapi.lib.ITrack {

    public TileEntityTickableTrack() {
        super();
    }

    public TileEntityTickableTrack(Identifier id) {
        super(id);
    }

    private trackapi.lib.ITrack track() {
        return instance() instanceof ITrack ? ((ITrack) instance()).to() : null;
    }

    @Override
    public double getTrackGauge() {
        return track() != null ? track().getTrackGauge() : 0;
    }

    @Override
    public Vec3 getNextPosition(Vec3 pos, Vec3 mot) {
        return track() != null ? track().getNextPosition(pos, mot) : pos;
    }

    @Override
    public Identifier getName() {
        return new Identifier(ModCore.MODID, "tile_track");
    }
}
