package cam72cam.umc.api.entity.custom;

import cam72cam.umc.api.entity.boundingbox.IBoundingBox;

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
