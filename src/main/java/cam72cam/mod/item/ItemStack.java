package cam72cam.mod.item;

import cam72cam.mod.entity.Player;
import cam72cam.mod.serialization.TagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.ApiStatus;

/** Wrapper around Minecraft ItemStack (Item, count, NBT) */
public class ItemStack {
    public static final ItemStack EMPTY = new ItemStack(net.minecraft.item.ItemStack.EMPTY);

    @ApiStatus.Internal
    public final net.minecraft.item.ItemStack internal;

    /** Wrap Minecraft ItemStack.  Do not use directly */
    public ItemStack(net.minecraft.item.ItemStack internal) {
        this.internal = internal;
    }

    /** Deserialize from tag */
    public ItemStack(TagCompound nbt) {
        this(net.minecraft.item.ItemStack.read(nbt.internal));
    }

    /** Construct from customItem */
    public ItemStack(CustomItem item, int i) {
        this(new net.minecraft.item.ItemStack(item.internal, i));
    }

    @Deprecated
    public ItemStack(String item, int i, int meta) {
        this(new net.minecraft.item.ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(item)), i));
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
        return internal.getDisplayName().getString();
    }

    /** Is count zero? */
    public boolean isEmpty() {
        return internal.isEmpty();
    }

    /** Reduce stack size by i */
    public void shrink(int i) {
        internal.shrink(i);
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
        return FluidUtil.getFluidHandler(internal).isPresent();
    }

    /** Can be burnt in a furnace */
    public boolean isFlammable() {
        return getBurnTime() != 0;
    }

    /** Ticks item will burn in a furnace (Make sure you multiply by count to get total burn time) */
    public int getBurnTime() {
        return ForgeHooks.getBurnTime(internal);
    }

    /** Max count of the stack */
    public int getLimit() {
        return internal.getMaxStackSize();
    }

    /** Is the item this type of tool? */
    public boolean isValidTool(ToolType tool) {
        return internal.getItem().getToolTypes(internal).contains(tool.internal);
    }

    /** Increase the damage counter on the item by the player */
    public void damageItem(int i, Player player) {
        internal.damageItem(i, player.internal, (s) -> {});
    }

    /**
     * Return the remainder that should be left in inventory when this stack's content is consumed, or {@link ItemStack#EMPTY} if this item doesn't have anything to leave
     * <p>
     * Think about milk bucket when crafting cakes
     */
    public ItemStack getRemainder() {
        if (internal.getItem().hasContainerItem(internal)) {
            return new ItemStack(internal.getItem().getContainerItem(internal));
        }
        return EMPTY;
    }

    /** Completely null out the tag compound */
    @ApiStatus.Internal
    public void clearTagCompound() {
        internal.setTag(null);
    }

    @Override
    public String toString() {
        return internal.toString();
    }

    /** Compares: item, count, damage, data */
    @Override
    public boolean equals(Object other) {
        return other instanceof ItemStack && net.minecraft.item.ItemStack.areItemStacksEqual(internal, ((ItemStack)other).internal);
    }

}
