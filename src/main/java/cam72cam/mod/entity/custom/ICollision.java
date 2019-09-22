package cam72cam.mod.entity.custom;

import cam72cam.mod.entity.boundingbox.IBoundingBox;
import net.minecraft.util.AxisAlignedBB;

public interface ICollision {
    ICollision NOP = () -> IBoundingBox.from(AxisAlignedBB.getBoundingBox(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D));

    static ICollision get(Object o) {
        if (o instanceof ICollision) {
            return (ICollision) o;
        }
        return NOP;
    }

    IBoundingBox getCollision();
}
