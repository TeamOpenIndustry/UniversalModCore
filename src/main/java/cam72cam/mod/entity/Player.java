package cam72cam.mod.entity;

import cam72cam.mod.item.ClickResult;
import cam72cam.mod.item.IInventory;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.text.PlayerMessage;
import cam72cam.mod.util.Facing;
import cam72cam.mod.util.Hand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.WorldSettings;

public class Player extends Entity {
    public final EntityPlayer internal;

    public Player(EntityPlayer player) {
        super(player);
        this.internal = player;
    }

    public ItemStack getHeldItem(Hand hand) {
        return new ItemStack(internal.getHeldItem());
    }

    public void sendMessage(PlayerMessage o) {
        internal.addChatComponentMessage(o.internal);
    }

    public boolean isCrouching() {
        return internal.isSneaking();
    }

    public boolean isCreative() {
        return internal.capabilities.isCreativeMode;
    }

    @Deprecated
    public float getYawHead() {
        return internal.rotationYawHead;
    }

    public void setHeldItem(Hand hand, ItemStack stack) {
        internal.setCurrentItemOrArmor(0, stack.internal);
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

    public ClickResult clickBlock(Hand hand, Vec3i pos, Vec3d hit) {
        net.minecraft.item.ItemStack stack = getHeldItem(hand).internal;
        return ClickResult.from(stack.getItem()
                .onItemUse(stack, internal, getWorld().internal, pos.x, pos.y, pos.z, Facing.DOWN.internal.ordinal(), (float) hit.x, (float) hit.y, (float) hit.z));
    }
}
