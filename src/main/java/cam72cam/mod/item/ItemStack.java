package cam72cam.mod.item;

import cam72cam.mod.entity.Player;
import cam72cam.mod.serialization.TagCompound;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraftforge.fluids.FluidUtil;

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
        this(new net.minecraft.item.ItemStack(nbt.internal));
    }

    /** Construct from customItem */
    public ItemStack(CustomItem item, int i) {
        this(new net.minecraft.item.ItemStack(item.internal, i));
    }

    @Deprecated
    public ItemStack(String item, int i, int meta) {
        this(new net.minecraft.item.ItemStack(Item.getByNameOrId(item), i, meta));
    }

    /** Tag attached to this stack */
    public TagCompound getTagCompound() {
        if (internal.getTagCompound() == null) {
            internal.setTagCompound(new TagCompound().internal);
        }
        return new TagCompound(internal.getTagCompound());
    }

    /** Tag attached to this stack */
    public void setTagCompound(TagCompound data) {
        internal.setTagCompound(data.internal);
    }

    public ItemStack copy() {
        return new ItemStack(internal.copy());
    }

    /** Serialize */
    public TagCompound toTag() {
        return new TagCompound(internal.serializeNBT());
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
        return internal.getDisplayName();
    }

    /** Is count zero? */
    public boolean isEmpty() {
        return internal.isEmpty();
    }

    /** Reduce stack size by i */
    public void shrink(int i) {
        internal.shrink(i);
    }

    /** Compares: item, count, damage, data */
    @Override
    public boolean equals(Object other) {
        return other instanceof ItemStack && net.minecraft.item.ItemStack.areItemStacksEqual(internal, ((ItemStack)other).internal);
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
        return FluidUtil.getFluidHandler(internal) != null;
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
        return internal.getMaxStackSize();
    }

    /** Is the item this type of tool? */
    public boolean isValidTool(ToolType tool) {
        return internal.getItem().getToolClasses(internal).contains(tool.toString());
    }

    @Override
    public String toString() {
        return internal.toString();
    }

    /** Increase the damage counter on the item by the player */
    public void damageItem(int i, Player player) {
        internal.damageItem(i, player.internal);
    }

    /** Completely null out the tag compound */
    public void clearTagCompound() {
        internal.setTagCompound(null);
    }
}
