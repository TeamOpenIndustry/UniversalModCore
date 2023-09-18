package cam72cam.mod.entity;

import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.item.IInventory;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.text.PlayerMessage;
import net.minecraft.Util;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionTypes;

import static net.minecraft.world.InteractionHand.*;

/** Wrapper around EntityPlayer */
public class Player extends Entity {
    public final net.minecraft.world.entity.player.Player internal;

    public Player(net.minecraft.world.entity.player.Player player) {
        super(player);
        this.internal = player;
    }

    public ItemStack getHeldItem(Hand hand) {
        return new ItemStack(internal.getItemInHand(hand.internal));
    }

    public void sendMessage(PlayerMessage o) {
        internal.displayClientMessage(o.internal, false);
    }

    public boolean isCrouching() {
        return internal.isCrouching();
    }

    public boolean isCreative() {
        return internal.isCreative();
    }

    @Deprecated
    public float getYawHead() {
        return internal.yHeadRot;
    }

    public void setHeldItem(Hand hand, ItemStack stack) {
        internal.setItemInHand(hand.internal, stack.internal());
    }

    public int getFoodLevel() {
        return internal.getFoodData().getFoodLevel() + (int)(internal.getFoodData().getSaturationLevel());
    }

    public void useFood(int i) {
        internal.causeFoodExhaustion(i);
    }

    public IInventory getInventory() {
        return IInventory.from(internal.getInventory());
    }

    /** Force the player to click a block */
    public ClickResult clickBlock(Hand hand, Vec3i pos, Vec3d hit) {
        return ClickResult.from(getHeldItem(hand).internal().useOn(new UseOnContext(internal, hand.internal, new BlockHitResult(hit.internal(), Direction.DOWN, pos.internal(), false))));
    }

    /** What direction the player is trying to move and how fast */
    public Vec3d getMovementInput() {
        return new Vec3d(internal.xxa, internal.yya, internal.zza).scale(internal.isSprinting() ? 0.4 : 0.2);
    }

    public boolean hasPermission(PermissionAction action) {
        // TODO 1.18.2 true is probably overly permissive
        return internal instanceof ServerPlayer ? PermissionAPI.getPermission((ServerPlayer) internal, action.node) : true;
    }

    public static class PermissionAction {
        private final PermissionNode<Boolean> node;

        private PermissionAction(String node, boolean opRequiredDefault) {
            this.node = new PermissionNode<>("permission", node, PermissionTypes.BOOLEAN, ((player, playerUUID, context) -> !opRequiredDefault || player != null && player.server.getPlayerList().isOp(player.getGameProfile())));
            CommonEvents.Permissions.NODES.subscribe(event -> event.addNodes(this.node));
        }
    }

    public static PermissionAction registerAction(String name, String description, boolean opRequiredDefault) {
        return new PermissionAction(name, opRequiredDefault);
    }

    public enum Hand {
        PRIMARY(MAIN_HAND),
        SECONDARY(OFF_HAND),
        ;

        public final InteractionHand internal;

        Hand(InteractionHand internal) {
            this.internal = internal;
        }

        public static Hand from(InteractionHand hand) {
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
