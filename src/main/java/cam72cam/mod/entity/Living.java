package cam72cam.mod.entity;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;

/** Wrapper around EntityLiving */
public class Living extends Entity {
    private final LivingEntity living;

    public Living(net.minecraft.entity.LivingEntity entity) {
        super(entity);
        this.living = entity;
    }

    public boolean isLeashedTo(Player player) {
        return living instanceof MobEntity && ((MobEntity) living).isLeashed() && ((MobEntity) living).getLeashHolder().getUUID().equals(player.getUUID());
    }

    public void unleash(Player player) {
        if (living instanceof MobEntity) {
            ((MobEntity)living).dropLeash(true, !player.isCreative());
        }
    }

    public void setLeashHolder(Player player) {
        if (living instanceof MobEntity) {
            ((MobEntity) living).setLeashedTo(player.internal, true);
        }
    }

    public boolean canBeLeashedTo(Player player) {
        return living instanceof MobEntity && ((MobEntity)living).canBeLeashed(player.internal);
    }
}
