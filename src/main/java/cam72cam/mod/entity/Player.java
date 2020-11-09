package cam72cam.mod.entity;

import cam72cam.mod.item.ClickResult;
import cam72cam.mod.item.IInventory;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.text.PlayerMessage;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.Direction;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockRayTraceResult;

/** Wrapper around EntityPlayer */
public class Player extends Entity {
    public final PlayerEntity internal;

    public Player(PlayerEntity player) {
        super(player);
        this.internal = player;
    }

    public ItemStack getHeldItem(Hand hand) {
        return new ItemStack(internal.getHeldItem(hand.internal));
    }

    public void sendMessage(PlayerMessage o) {
        internal.sendMessage(o.internal, Util.DUMMY_UUID);
    }

    public boolean isCrouching() {
        return internal.isCrouching();
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
        return ClickResult.from(getHeldItem(hand).internal.onItemUse(new ItemUseContext(internal, hand.internal, new BlockRayTraceResult(hit.internal(), Direction.DOWN, pos.internal(), false))));
    }

    /** What direction the player is trying to move and how fast */
    public Vec3d getMovementInput() {
        return new Vec3d(internal.moveStrafing, internal.moveVertical, internal.moveForward).scale(internal.isSprinting() ? 0.4 : 0.2);
    }

    public enum Hand {
        PRIMARY(net.minecraft.util.Hand.MAIN_HAND),
        SECONDARY(net.minecraft.util.Hand.OFF_HAND),
        ;

        public final net.minecraft.util.Hand internal;

        Hand(net.minecraft.util.Hand internal) {
            this.internal = internal;
        }

        public static Hand from(net.minecraft.util.Hand hand) {
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
