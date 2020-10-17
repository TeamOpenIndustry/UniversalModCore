package cam72cam.mod.entity.custom;

import cam72cam.mod.entity.boundingbox.IBoundingBox;
import net.minecraft.util.math.AxisAlignedBB;

public interface ICollision {
    ICollision NOP = () -> IBoundingBox.ORIGIN;

    static ICollision get(Object o) {
        if (o instanceof ICollision) {
            return (ICollision) o;
        }
        return NOP;
    }

    /** Collision Bounding Box */
    IBoundingBox getCollision();
}
