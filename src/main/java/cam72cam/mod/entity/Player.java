package cam72cam.mod.entity;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.item.IInventory;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.net.Packet;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.text.PlayerMessage;
import cam72cam.mod.util.Facing;
import net.minecraft.entity.player.EntityPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Wrapper around EntityPlayer */
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

    public void sendActionBarMessage(PlayerMessage o){
        internal.sendStatusMessage(o.internal);
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

    /** Force the player to click a block */
    public ClickResult clickBlock(Hand hand, Vec3i pos, Vec3d hit) {
        net.minecraft.item.ItemStack stack = getHeldItem(hand).internal;
        return ClickResult.from(stack.getItem()
                .onItemUse(stack, internal, getWorld().internal, pos.x, pos.y, pos.z, Facing.DOWN.internal.ordinal(), (float) hit.x, (float) hit.y, (float) hit.z));
    }

    private static final Map<UUID, Vec3d> serverMovement = new HashMap<>();
    /** What direction the player is trying to move and how fast */
    public Vec3d getMovementInput() {
        return serverMovement.getOrDefault(getUUID(), Vec3d.ZERO);
    }

    public boolean hasPermission(PermissionAction action) {
        return !action.opRequired || internal.canCommandSenderUseCommand(2, "");
    }

    public static class PermissionAction {
        private final String node;
        private final boolean opRequired;

        private PermissionAction(String node, boolean opRequiredDefault) {
            this.node = node;
            this.opRequired = opRequiredDefault;
        }
    }

    public static PermissionAction registerAction(String name, String description, boolean opRequiredDefault) {
        //PermissionAPI.registerNode(name, opRequiredDefault ? DefaultPermissionLevel.OP : DefaultPermissionLevel.ALL, description);
        return new PermissionAction(name, opRequiredDefault);
    }

    public enum Hand {
        PRIMARY,
        SECONDARY,
        ;
    }



    private static Vec3d posCache;
    /** Internal registration, do not use */
    public static void registerClientEvents() {
        ClientEvents.TICK.subscribe(() -> {
            if (MinecraftClient.isReady()) {
                EntityPlayer internal = MinecraftClient.getPlayer().internal;
                Vec3d movement = new Vec3d(internal.moveStrafing, internal.motionY, internal.moveForward).scale(internal.isSprinting() ? 0.4 : 0.2);
                if (!movement.equals(posCache)) {
                    posCache = movement;
                    serverMovement.put(MinecraftClient.getPlayer().getUUID(), movement);
                    new MovementSync(movement).sendToServer();
                }
            }
        });
    }


    /** 1.7.10 does not track player movement server side */
    public static class MovementSync extends Packet {
        @TagField
        private Vec3d movement;

        public MovementSync() {
            // Reflection
        }

        public MovementSync(Vec3d movement) {
            this.movement = movement;
        }

        @Override
        protected void handle() {
            if (this.getPlayer() != null) {
                serverMovement.put(this.getPlayer().getUUID(), movement);
                new MovementSync2EB(this.getPlayer().getUUID(), movement).sendToAll();
            }
        }
    }

    public static class MovementSync2EB extends Packet {
        @TagField
        private Vec3d movement;
        @TagField
        private UUID player;

        public MovementSync2EB() {
            // Reflection
        }

        public MovementSync2EB(UUID player, Vec3d movement) {
            this.player = player;
            this.movement = movement;
        }

        @Override
        protected void handle() {
            if (!MinecraftClient.getPlayer().getUUID().equals(player)) {
                serverMovement.put(player, movement);
            }
        }
    }
}
