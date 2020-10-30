package cam72cam.mod.entity;

import cam72cam.mod.item.ClickResult;
import cam72cam.mod.item.IInventory;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.text.PlayerMessage;
import cam72cam.mod.util.Facing;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHand;

/** Wrapper around EntityPlayer */
public class Player extends Entity {
    public final EntityPlayer internal;

    public Player(EntityPlayer player) {
        super(player);
        this.internal = player;
    }

    public ItemStack getHeldItem(Hand hand) {
        return new ItemStack(internal.getHeldItem(hand.internal));
    }

    public void sendMessage(PlayerMessage o) {
        internal.sendMessage(o.internal);
    }

    public boolean isCrouching() {
        return internal.isSneaking();
    }

    public boolean isCreative() {
        return internal.isCreative();
    }

    @Deprecated
    public float getYawHead() {
        return internal.rotationYawHead;
    }

    public void setHeldItem(Hand hand, ItemStack stack) {
        internal.setHeldItem(hand.internal, stack.internal);
    }

    public int getFoodLevel() {
        return internal.getFoodStats().getFoodLevel() + (int)(internal.getFoodStats().getSaturationLevel());
    }

    public void useFood(int i) {
        internal.addExhaustion(i);
    }

    public IInventory getInventory() {
        return IInventory.from(internal.inventory);
    }

    /** Force the player to click a block */
    public ClickResult clickBlock(Hand hand, Vec3i pos, Vec3d hit) {
        return ClickResult.from(getHeldItem(hand).internal.onItemUse(internal, getWorld().internal, pos.internal(), hand.internal, Facing.DOWN.internal, (float) hit.x, (float) hit.y, (float) hit.z));
    }

    /** What direction the player is trying to move and how fast */
    public Vec3d getMovementInput() {
        return new Vec3d(internal.moveStrafing, internal.motionY, internal.moveForward).scale(internal.isSprinting() ? 0.4 : 0.2);
    }

    public enum Hand {
        PRIMARY(EnumHand.MAIN_HAND),
        SECONDARY(EnumHand.OFF_HAND),
        ;

        public final EnumHand internal;

        Hand(EnumHand internal) {
            this.internal = internal;
        }

        public static Hand from(EnumHand hand) {
            switch (hand) {
                case MAIN_HAND:
                    return PRIMARY;
                case OFF_HAND:
                    return SECONDARY;
                default:
                    return null;
            }
        }
    }
}
