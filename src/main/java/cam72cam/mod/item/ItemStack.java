package cam72cam.mod.item;

import alexiil.mc.lib.attributes.AttributeProviderItem;
import alexiil.mc.lib.attributes.fluid.FluidAttributes;
import alexiil.mc.lib.attributes.fluid.mixin.api.IBucketItem;
import cam72cam.mod.entity.Player;
import cam72cam.mod.util.TagCompound;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.FurnaceBlockEntity;
import net.minecraft.item.BucketItem;
import net.minecraft.item.Item;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ItemStack {
    public static final ItemStack EMPTY = new ItemStack(net.minecraft.item.ItemStack.EMPTY);

    public final net.minecraft.item.ItemStack internal;
    private final Item item;

    public ItemStack(net.minecraft.item.ItemStack internal) {
        this.internal = internal;
        this.item = internal.getItem();
    }

    public ItemStack(Item item, int count) {
        this(new net.minecraft.item.ItemStack(item, count));
    }

    public ItemStack(TagCompound bedItem) {
        this(net.minecraft.item.ItemStack.fromTag(bedItem.internal));
    }

    public ItemStack(Item item, int count, int meta) {
        this(new net.minecraft.item.ItemStack(item, count));
        internal.setDamage(meta);
    }

    public ItemStack(Block block) {
        this(new net.minecraft.item.ItemStack(block));
    }

    public ItemStack(Block block, int count, int meta) {
        this(new net.minecraft.item.ItemStack(block, count));
        internal.setDamage(meta);
    }

    public ItemStack(Item item) {
        this(new net.minecraft.item.ItemStack(item));
    }

    public ItemStack(ItemBase item, int count) {
        this(item.internal, count);
    }

    public ItemStack(String item, int i, int meta) {
        this(Registry.ITEM.get(new Identifier(item)), i, meta);
    }

    public TagCompound getTagCompound() {
        if (internal.getTag() == null) {
            internal.setTag(new TagCompound().internal);
        }
        return new TagCompound(internal.getTag());
    }

    public void setTagCompound(TagCompound data) {
        internal.setTag(data.internal);
    }

    public ItemStack copy() {
        return new ItemStack(internal.copy());
    }

    public TagCompound toTag() {
        return new TagCompound(internal.toTag(new CompoundTag()));
    }

    public int getCount() {
        return internal.getCount();
    }

    public void setCount(int count) {
        internal.setCount(count);
    }

    public String getDisplayName() {
        return internal.getName().getString();
    }

    public boolean isEmpty() {
        return internal.isEmpty();
    }

    public void shrink(int i) {
        internal.decrement(i);
    }

    public boolean equals(ItemStack other) {
        return internal.isItemEqual(other.internal);
    }

    public boolean is(Fuzzy fuzzy) {
        return fuzzy.matches(this);
    }

    public boolean is(ItemBase item) {
        return item.internal == this.item;
    }

    public boolean isFluidContainer() {
        return FluidAttributes.FIXED_INV.getFirstOrNull(internal) != null;
    }

    public boolean isFlammable() {
        return getBurnTime() != 0;
    }

    public int getBurnTime() {
        return item == null ? 0 : FuelRegistry.INSTANCE.get(item) == null ? 0 : FuelRegistry.INSTANCE.get(item);
    }

    public int getLimit() {
        return internal.getMaxCount();
    }

    public boolean isValidTool(ToolType tool) {
        switch (tool) {
            case SHOVEL:
                return item instanceof ShovelItem;
        }
        return false;
    }

    @Override
    public String toString() {
        return internal.toString();
    }

    public void damageItem(int i, Player player) {
        internal.damage(i, player.internal, e -> e.sendToolBreakStatus(player.internal.getActiveHand()));
    }

    public void clearTagCompound() {
        internal.setTag(null);
    }
}
