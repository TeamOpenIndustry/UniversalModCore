package cam72cam.mod.entity;

import cam72cam.mod.item.ItemStack;
import cam72cam.mod.world.World;

/**
 * Represents an item entity in the world, wrapping Minecraft's EntityItem.
 */
public class ItemEntity extends Entity {
    public net.minecraft.entity.item.EntityItem internal;

    public ItemEntity(net.minecraft.entity.item.EntityItem entity) {
        super(entity);
        this.internal = entity;
    }

    public ItemStack getContent() {
        return new ItemStack(internal.getItem());
    }

    /**
     * Retrieves the owner of this item entity. Only the owner can pick it up.
     *
     * @return the player allowed to pick up this item, or null if no owner is set
     */
    public Player getOwner() {
        if (internal.getOwner() == null || this.internal.getEntityWorld().getPlayerEntityByName(internal.getOwner()) == null) {
            return null;
        }
        return World.get(this.internal.world)
                    .getEntity(this.internal.getEntityWorld()
                                            .getPlayerEntityByName(internal.getOwner())).asPlayer();
    }

    /**
     * Sets the owner of this item entity. Only the owner can pick it up.
     *
     * @param player the player to set as the owner
     */
    public void setOwner(Player player) {
        internal.setOwner(player.internal.getName());
    }

    /**
     * Retrieves the player who threw this item entity.
     *
     * @return the player who threw the item, or null if not thrown by a player
     */
    public Player getThrower() {
        if (internal.getThrower() == null || this.internal.getEntityWorld().getPlayerEntityByName(internal.getThrower()) == null) {
            return null;
        }
        return World.get(this.internal.world)
                    .getEntity(this.internal.getEntityWorld()
                                            .getPlayerEntityByName(internal.getThrower())).asPlayer();
    }

    /**
     * Sets the pickup delay for this item entity.
     *
     * @param ticks the minimum delay in ticks before the item can be picked up
     */
    public void setPickupDelay(int ticks) {
        internal.setPickupDelay(ticks);
    }

    /**
     * Prevents this item entity from despawning naturally.
     */
    public void setNoDespawn() {
        internal.setNoDespawn();
    }
}