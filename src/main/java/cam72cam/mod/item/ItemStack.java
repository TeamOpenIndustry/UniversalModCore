package cam72cam.mod.item;

import cam72cam.mod.entity.Player;
import cam72cam.mod.serialization.TagCompound;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.registries.ForgeRegistries;

/** Wrapper around Minecraft ItemStack (Item, count, NBT) */
public class ItemStack {
    public static final ItemStack EMPTY = new ItemStack(net.minecraft.world.item.ItemStack.EMPTY);

    public final net.minecraft.world.item.ItemStack internal;

    /** Wrap Minecraft ItemStack.  Do not use directly */
    public ItemStack(net.minecraft.world.item.ItemStack internal) {
        this.internal = internal;
    }

    /** Deserialize from tag */
    public ItemStack(TagCompound nbt) {
        this(net.minecraft.world.item.ItemStack.of(nbt.internal));
    }

    /** Construct from customItem */
    public ItemStack(CustomItem item, int i) {
        this(new net.minecraft.world.item.ItemStack(item.internal, i));
    }

    @Deprecated
    public ItemStack(String item, int i, int meta) {
        this(new net.minecraft.world.item.ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(item)), i));
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
        return internal.getHoverName().getString();
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
        return other instanceof ItemStack && net.minecraft.world.item.ItemStack.matches(internal, ((ItemStack)other).internal);
    }


    /** Compares: item, damage */
    public boolean is(ItemStack stack) {
        return net.minecraft.world.item.ItemStack.isSame(internal, stack.internal);
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
        return ForgeHooks.getBurnTime(internal, RecipeType.SMELTING);
    }

    /** Max count of the stack */
    public int getLimit() {
        return internal.getMaxStackSize();
    }

    /** Is the item this type of tool? */
    public boolean isValidTool(ToolType tool) {
        return internal.getItem().canPerformAction(internal, tool.internal);
    }

    @Override
    public String toString() {
        return internal.toString();
    }

    /** Increase the damage counter on the item by the player */
    public void damageItem(int i, Player player) {
        internal.hurtAndBreak(i, player.internal, (s) -> {});
    }

    /** Completely null out the tag compound */
    public void clearTagCompound() {
        internal.setTag(null);
    }
}
