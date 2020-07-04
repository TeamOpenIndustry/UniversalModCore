package cam72cam.mod.item;

import cam72cam.mod.entity.Player;
import cam72cam.mod.serialization.TagCompound;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraftforge.fluids.FluidUtil;

public class ItemStack {
    public static final ItemStack EMPTY = new ItemStack();

    public final net.minecraft.item.ItemStack internal;
    private final Item item;

    private ItemStack() {
        // Empty
        internal = null;
        item = null;
    }
    public ItemStack(net.minecraft.item.ItemStack internal) {
        this.internal = internal;
        this.item = this.internal != null ? internal.getItem() : null;
    }

    public ItemStack(Item item, int count) {
        this(new net.minecraft.item.ItemStack(item, count));
    }

    public ItemStack(TagCompound bedItem) {
        this(net.minecraft.item.ItemStack.loadItemStackFromNBT(bedItem.internal));
    }

    public ItemStack(Item item, int count, int meta) {
        this(new net.minecraft.item.ItemStack(item, count, meta));
    }

    public ItemStack(Block block) {
        this(new net.minecraft.item.ItemStack(block));
    }

    public ItemStack(Block block, int count, int meta) {
        this(new net.minecraft.item.ItemStack(block, count, meta));
    }

    public ItemStack(Item item) {
        this(new net.minecraft.item.ItemStack(item));
    }

    public ItemStack(ItemBase item, int count) {
        this(item.internal, count);
    }

    public ItemStack(String item, int i, int meta) {
        this(Item.getByNameOrId(item), i, meta);
    }

    public TagCompound getTagCompound() {
        if (internal == null) {
            return new TagCompound();
        }

        if (internal.getTagCompound() == null) {
            internal.setTagCompound(new TagCompound().internal);
        }
        return new TagCompound(internal.getTagCompound());
    }

    public void setTagCompound(TagCompound data) {
        if (internal != null) {
            internal.setTagCompound(data.internal);
        }
    }

    public ItemStack copy() {
        return internal != null ? new ItemStack(internal.copy()) : EMPTY;
    }

    public TagCompound toTag() {
        return internal != null ? new TagCompound(internal.serializeNBT()) : new TagCompound();
    }

    public int getCount() {
        return internal != null ? internal.stackSize : 0;
    }

    public void setCount(int count) {
        if (internal != null) {
            internal.stackSize = count;
        }
    }

    public String getDisplayName() {
        return internal != null ? internal.getDisplayName() : "Empty";
    }

    public boolean isEmpty() {
        return internal == null || internal.stackSize == 0;
    }

    public void shrink(int i) {
        if (internal != null) {
            internal.stackSize -= i;
        }
    }

    public boolean equals(ItemStack other) {
        return (other.internal == null && internal == null) ||  internal != null && internal.isItemEqual(other.internal);
    }

    public boolean is(Fuzzy fuzzy) {
        return fuzzy.matches(this);
    }

    public boolean is(ItemBase item) {
        return item != null && item.internal == this.item;
    }

    public boolean isFluidContainer() {
        return FluidUtil.getFluidHandler(internal) != null;
    }

    public boolean isFlammable() {
        return getBurnTime() != 0;
    }

    public int getBurnTime() {
        return TileEntityFurnace.getItemBurnTime(internal);
    }

    public int getLimit() {
        return internal != null ? internal.getMaxStackSize() : 64;
    }

    public boolean isValidTool(ToolType tool) {
        return item != null && item.getToolClasses(internal).contains(tool.toString());
    }

    @Override
    public String toString() {
        return internal != null ? internal.toString() : "Empty";
    }

    public void damageItem(int i, Player player) {
        if (internal != null) {
            internal.damageItem(i, player.internal);
        }
    }

    public void clearTagCompound() {
        internal.setTagCompound(null);
    }
}
