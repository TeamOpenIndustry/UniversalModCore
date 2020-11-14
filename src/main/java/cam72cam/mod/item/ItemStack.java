package cam72cam.mod.item;

import alexiil.mc.lib.attributes.fluid.FluidAttributes;
import cam72cam.mod.entity.Player;
import cam72cam.mod.serialization.TagCompound;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.fabricmc.fabric.api.tools.FabricToolTags;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ShovelItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

/** Wrapper around Minecraft ItemStack (Item, count, NBT) */
public class ItemStack {
    public static final ItemStack EMPTY = new ItemStack(net.minecraft.item.ItemStack.EMPTY);

    public final net.minecraft.item.ItemStack internal;

    /** Wrap Minecraft ItemStack.  Do not use directly */
    public ItemStack(net.minecraft.item.ItemStack internal) {
        this.internal = internal;
    }

    /** Deserialize from tag */
    public ItemStack(TagCompound nbt) {
        this(net.minecraft.item.ItemStack.fromTag(nbt.internal));
    }

    /** Construct from customItem */
    public ItemStack(CustomItem item, int i) {
        this(new net.minecraft.item.ItemStack(item.internal, i));
    }

    @Deprecated
    public ItemStack(String item, int i, int meta) {
        this(new net.minecraft.item.ItemStack(Registry.ITEM.get(new Identifier(item)), i));
    }

    /** Tag attached to this stack */
    public TagCompound getTagCompound() {
        if (internal.getTag() == null) {
            internal.setTag(new TagCompound().internal);
        }
        return new TagCompound(internal.getTag());
    }

    /** Tag attached to this stack */
    public void setTagCompound(TagCompound data) {
        internal.setTag(data.internal);
    }

    public ItemStack copy() {
        return new ItemStack(internal.copy());
    }

    /** Serialize */
    public TagCompound toTag() {
        return new TagCompound(internal.toTag(new CompoundTag()));
    }

    /** Items in this stack */
    public int getCount() {
        return internal.getCount();
    }

    /** Set the items in this stack */
    public void setCount(int count) {
        internal.setCount(count);
    }

    /** Human readable name */
    public String getDisplayName() {
        return internal.getName().getString();
    }

    /** Is count zero? */
    public boolean isEmpty() {
        return internal.isEmpty();
    }

    /** Reduce stack size by i */
    public void shrink(int i) {
        internal.decrement(i);
    }

    /** Compares: item, count, damage, data */
    @Override
    public boolean equals(Object other) {
        return other instanceof ItemStack && net.minecraft.item.ItemStack.areEqualIgnoreDamage(internal, ((ItemStack)other).internal);
    }


    /** Compares: item, damage */
    public boolean is(ItemStack stack) {
        return net.minecraft.item.ItemStack.areItemsEqual(internal, stack.internal);
    }

    public boolean is(Fuzzy fuzzy) {
        return fuzzy.matches(this);
    }

    public boolean is(CustomItem item) {
        return item.internal == this.internal.getItem();
    }

    /** Is a bucket or similar item */
    public boolean isFluidContainer() {
        return FluidAttributes.GROUPED_INV.getFirstOrNull(internal) != null;
    }

    /** Can be burnt in a furnace */
    public boolean isFlammable() {
        return getBurnTime() != 0;
    }

    /** Ticks item will burn in a furnace (Make sure you multiply by count to get total burn time) */
    public int getBurnTime() {
        return internal.getItem() == null ? 0 : FuelRegistry.INSTANCE.get(internal.getItem()) == null ? 0 : FuelRegistry.INSTANCE.get(internal.getItem());
    }

    /** Max count of the stack */
    public int getLimit() {
        return internal.getMaxCount();
    }

    /** Is the item this type of tool? */
    public boolean isValidTool(ToolType tool) {
        switch (tool) {
            case PICKAXE:
                return FabricToolTags.PICKAXES.contains(internal.getItem());
            case AXE:
                return FabricToolTags.AXES.contains(internal.getItem());
            case SHOVEL:
                return FabricToolTags.SHOVELS.contains(internal.getItem());
        }
        return false;
    }

    @Override
    public String toString() {
        return internal.toString();
    }

    /** Increase the damage counter on the item by the player */
    public void damageItem(int i, Player player) {
        internal.damage(i, player.internal, e -> e.sendToolBreakStatus(player.internal.getActiveHand()));
    }

    /** Completely null out the tag compound */
    public void clearTagCompound() {
        internal.setTag(null);
    }
}
