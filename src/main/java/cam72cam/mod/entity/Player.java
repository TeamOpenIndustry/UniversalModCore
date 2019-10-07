package cam72cam.mod.entity;

import cam72cam.mod.gui.IScreen;
import cam72cam.mod.gui.ScreenBuilder;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.item.IInventory;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.text.PlayerMessage;
import cam72cam.mod.util.Facing;
import cam72cam.mod.util.Hand;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.hit.BlockHitResult;

public class Player extends Entity {
    public final PlayerEntity internal;

    public Player(PlayerEntity player) {
        super(player);
        this.internal = player;
    }

    public ItemStack getHeldItem(Hand hand) {
        return new ItemStack(internal.getStackInHand(hand.internal));
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
        return internal.headYaw;
    }

    public void setHeldItem(Hand hand, ItemStack stack) {
        internal.setStackInHand(hand.internal, stack.internal);
    }

    public int getFoodLevel() {
        return internal.getHungerManager().getFoodLevel();
    }

    public void setFoodLevel(int i) {
        internal.getHungerManager().setFoodLevel(i);
    }

    public IInventory getInventory() {
        return IInventory.from(internal.inventory);
    }

    public ClickResult clickBlock(Hand hand, Vec3i pos, Vec3d hit) {
        return ClickResult.from(getHeldItem(hand).internal.useOnBlock(new ItemUsageContext(internal, hand.internal, new BlockHitResult(hit.internal, Facing.DOWN.internal, pos.internal, false))));
    }

    @Environment(EnvType.CLIENT)
    public void openGui(IScreen screen) {
        MinecraftClient.getInstance().openScreen(new ScreenBuilder(screen));
    }
}
