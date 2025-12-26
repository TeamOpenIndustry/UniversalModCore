package cam72cam.mod.entity;

import cam72cam.mod.item.ItemStack;
import cam72cam.mod.world.World;

/**
 * Represents an item entity in the world, wrapping Minecraft's EntityItem.
 */
public class ItemEntity extends Entity {
    public final net.minecraft.world.entity.item.ItemEntity internal;

    public ItemEntity(net.minecraft.world.entity.item.ItemEntity entity) {
        super(entity);
        this.internal = entity;
    }

    public ItemStack getContent() {
        if (!isValid()) {
            return ItemStack.EMPTY;
        }

        return new ItemStack(internal.getItem());
    }

    public void setContent(ItemStack stack) {
        internal.setItem(stack.internal);
    }

    /**
     * Retrieves the owner of this item entity. Only the owner can pick it up.
     *
     * @return the player allowed to pick up this item, or null if no owner is set
     */
    public Player getOwner() {
        if (!isValid()) {
            return null;
        }

        if (internal.getOwner() == null || this.internal.level.getPlayerByUUID(internal.getOwner()) == null) {
            return null;
        }

        return World.get(this.internal.level)
                    .getEntity(this.internal.level.getPlayerByUUID(internal.getOwner()))
                    .asPlayer();
    }

    /**
     * Sets the owner of this item entity. Only the owner can pick it up.
     *
     * @param player the player to set as the owner
     */
    public void setOwner(Player player) {
        if (isValid()) {
            internal.setOwner(player.internal.getUUID());
        }
    }

    /**
     * Retrieves the player who threw this item entity.
     *
     * @return the player who threw the item, or null if not thrown by a player
     */
    public Player getThrower() {
        if (!isValid()) {
            return null;
        }

        if (internal.getThrower() == null || this.internal.level.getPlayerByUUID(internal.getThrower()) == null) {
            return null;
        }
        return World.get(this.internal.level)
                    .getEntity(this.internal.level.getPlayerByUUID(internal.getThrower()))
                    .asPlayer();
    }

    /**
     * Sets the pickup delay for this item entity.
     *
     * @param ticks the minimum delay in ticks before the item can be picked up
     */
    public void setPickupDelay(int ticks) {
        if (isValid()) {
            internal.setPickUpDelay(ticks);
        }

    }

    /**
     * Prevents this item entity from despawning naturally.
     */
    public void setNoDespawn() {
        if (isValid()) {
            internal.age = -32768;
        }

    }

    /**
     * Return true if this item entity is still in the world and interactable.
     * If false, operations on this entity will be no-ops.
     */
    public boolean isValid() {
        return internal != null && internal.isAlive();
    }
}