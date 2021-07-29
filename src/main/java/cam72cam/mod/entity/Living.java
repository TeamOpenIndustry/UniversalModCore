package cam72cam.mod.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** Wrapper around EntityLiving */
public class Living extends Entity {
    private final LivingEntity living;

    public Living(net.minecraft.world.entity.LivingEntity entity) {
        super(entity);
        this.living = entity;
    }

    public boolean isLeashedTo(Player player) {
        return living instanceof Mob && ((Mob) living).isLeashed() && ((Mob) living).getLeashHolder().getUUID().equals(player.getUUID());
    }

    public void unleash(Player player) {
        if (living instanceof Mob) {
            ((Mob)living).dropLeash(true, !player.isCreative());
        }
    }

    public void setLeashHolder(Player player) {
        if (living instanceof Mob) {
            ((Mob) living).setLeashedTo(player.internal, true);
        }
    }

    public boolean canBeLeashedTo(Player player) {
        return living instanceof Mob && ((Mob)living).canBeLeashed(player.internal);
    }
}
