package cam72cam.mod.entity;

import net.minecraft.entity.mob.MobEntity;

/** Wrapper around EntityLiving */
public class Living extends Entity {
    private final MobEntity living;

    public Living(MobEntity entity) {
        super(entity);
        this.living = entity;
    }

    public boolean isLeashedTo(Player player) {
        return living.isLeashed() && living.getHoldingEntity().getUuid().equals(player.getUUID());
    }

    public void unleash(Player player) {
        living.detachLeash(true, !player.isCreative());
    }

    public void setLeashHolder(Player player) {
        living.attachLeash(player.internal, true);
    }

    public boolean canBeLeashedTo(Player player) {
        return living.canBeLeashedBy(player.internal);
    }
}
