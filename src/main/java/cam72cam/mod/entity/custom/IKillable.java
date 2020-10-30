package cam72cam.mod.entity.custom;

import cam72cam.mod.entity.DamageType;
import cam72cam.mod.entity.Entity;

public interface IKillable {
    IKillable NOP = new IKillable() {
        @Override
        public void onDamage(DamageType type, Entity source, float amount, boolean bypassesArmor) {

        }

        @Override
        public void onRemoved() {

        }
    };

    static IKillable get(Object o) {
        if (o instanceof IKillable) {
            return (IKillable) o;
        }
        return NOP;
    }

    /** Called when damage is attempted at this entity */
    void onDamage(DamageType type, Entity source, float amount, boolean bypassesArmor);

    /** Called when the entity is killed and removed from the world */
    void onRemoved();
}
