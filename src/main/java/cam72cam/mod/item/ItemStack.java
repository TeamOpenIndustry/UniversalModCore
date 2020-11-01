package cam72cam.mod.item;

import cam72cam.mod.entity.Player;
import cam72cam.mod.serialization.TagCompound;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.IFluidContainerItem;

/** Wrapper around Minecraft ItemStack (Item, count, NBT) */
public class ItemStack {
    public static final ItemStack EMPTY = new ItemStack((net.minecraft.item.ItemStack)null);

    public final net.minecraft.item.ItemStack internal;

    /** Wrap Minecraft ItemStack.  Do not use directly */
    public ItemStack(net.minecraft.item.ItemStack internal) {
        this.internal = internal;
    }

    /** Deserialize from tag */
    public ItemStack(TagCompound nbt) {
        this(net.minecraft.item.ItemStack.loadItemStackFromNBT(nbt.internal));
    }

    /** Construct from customItem */
    public ItemStack(CustomItem item, int i) {
        this(new net.minecraft.item.ItemStack(item.internal, i));
    }

    @Deprecated
    public ItemStack(String item, int i, int meta) {
        this(new net.minecraft.item.ItemStack((Item) Item.itemRegistry.getObject(item), i, meta));
    }

    /** Tag attached to this stack */
    public TagCompound getTagCompound() {
        if (internal == null) {
            return new TagCompound();
        }

        if (internal.getTagCompound() == null) {
            internal.setTagCompound(new TagCompound().internal);
        }
        return new TagCompound(internal.getTagCompound());
    }

    /** Tag attached to this stack */
    public void setTagCompound(TagCompound data) {
        if (internal != null) {
            internal.setTagCompound(data.internal);
        }
    }

    public ItemStack copy() {
        return internal != null ? new ItemStack(internal.copy()) : EMPTY;
    }

    /** Serialize */
    public TagCompound toTag() {
        TagCompound data = new TagCompound();
        if (internal != null) {
            internal.writeToNBT(data.internal);
        }
        return data;
    }

    /** Items in this stack */
    public int getCount() {
        return internal != null ? internal.stackSize : 0;
    }

    /** Set the items in this stack */
    public void setCount(int count) {
        if (internal != null) {
            internal.stackSize = count;
        }
    }

    /** Human readable name */
    public String getDisplayName() {
        return internal != null ? internal.getDisplayName() : "Empty";
    }

    /** Is count zero? */
    public boolean isEmpty() {
        return internal == null || internal.stackSize == 0;
    }

    /** Reduce stack size by i */
    public void shrink(int i) {
        if (internal != null) {
            internal.stackSize -= i;
        }
    }

    /** Compares: item, count, damage, data */
    @Override
    public boolean equals(Object other) {
        return other instanceof ItemStack && net.minecraft.item.ItemStack.areItemStacksEqual(internal, ((ItemStack)other).internal);
    }


    /** Compares: item, damage */
    public boolean is(ItemStack stack) {
        return internal == stack.internal || internal != null && internal.isItemEqual(stack.internal);
    }

    public boolean is(Fuzzy fuzzy) {
        return fuzzy.matches(this);
    }

    public boolean is(CustomItem item) {
        return internal != null && item != null && item.internal == this.internal.getItem();
    }

    /** Is a bucket or similar item */
    public boolean isFluidContainer() {
        return internal != null && (internal.getItem() instanceof IFluidContainerItem || FluidContainerRegistry.isContainer(internal));
    }

    /** Can be burnt in a furnace */
    public boolean isFlammable() {
        return getBurnTime() != 0;
    }

    /** Ticks item will burn in a furnace (Make sure you multiply by count to get total burn time) */
    public int getBurnTime() {
        return TileEntityFurnace.getItemBurnTime(internal);
    }

    /** Max count of the stack */
    public int getLimit() {
        return internal != null ? internal.getMaxStackSize() : 64;
    }

    /** Is the item this type of tool? */
    public boolean isValidTool(ToolType tool) {
        return internal != null && internal.getItem().getToolClasses(internal).contains(tool.toString());
    }

    @Override
    public String toString() {
        return internal != null ? internal.toString() : "Empty";
    }

    /** Increase the damage counter on the item by the player */
    public void damageItem(int i, Player player) {
        if (internal != null) {
            internal.damageItem(i, player.internal);
        }
    }

    /** Completely null out the tag compound */
    public void clearTagCompound() {
        internal.setTagCompound(null);
    }
}
