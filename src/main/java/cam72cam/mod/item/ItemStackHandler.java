package cam72cam.mod.item;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;
import alexiil.mc.lib.attributes.item.impl.FullFixedItemInv;
import cam72cam.mod.util.TagCompound;
import net.minecraft.nbt.CompoundTag;

import java.util.function.BiPredicate;

public class ItemStackHandler implements IInventory {
    public ItemInv internal;
    protected BiPredicate<Integer, ItemStack> checkSlot = (integer, itemStack) -> true;

    protected class ItemInv extends FullFixedItemInv {
        public ItemInv(int invSize) {
            super(invSize);

            this.addListener((inv, slot, previous, current) -> {
                onContentsChanged(slot);
            }, () -> {});
        }

        @Override
        public boolean isItemValidForSlot(int slot, net.minecraft.item.ItemStack item) {
            return checkSlot.test(slot, new ItemStack(item));
        }

        @Override
        public ItemFilter getFilterForSlot(int slot) {
            return item -> checkSlot.test(slot, new ItemStack(item));
        }
    }

    public ItemStackHandler(int size) {
        this.internal = new ItemInv(size);
    }

    public ItemStackHandler() {
        this(1);
    }

    protected void onContentsChanged(int slot) {
        //NOP
    }

    public void setSize(int inventorySize) {
        // TODO more efficient / less error prone
        CompoundTag data = internal.toTag();
        internal = new ItemInv(inventorySize);
        internal.fromTag(data);
    }

    @Override
    public int getSlotCount() {
        return internal.getSlotCount();
    }

    @Override
    public ItemStack get(int slot) {
        return new ItemStack(internal.getInvStack(slot));
    }

    @Override
    public void set(int slot, ItemStack stack) {
        internal.setInvStack(slot, stack.internal, Simulation.ACTION);
    }

    @Override
    public ItemStack insert(int slot, ItemStack stack, boolean simulate) {
        return new ItemStack(internal.getSlot(slot).attemptInsertion(stack.internal, simulate ? Simulation.SIMULATE : Simulation.ACTION));
    }

    @Override
    public ItemStack extract(int slot, int amount, boolean simulate) {
        return new ItemStack(internal.getSlot(slot).attemptAnyExtraction(amount, simulate ? Simulation.SIMULATE : Simulation.ACTION));
    }

    @Override
    public int getLimit(int slot) {
        return internal.getSlot(slot).getMaxAmount(internal.getInvStack(slot));
    }

    public TagCompound save() {
        return new TagCompound(internal.toTag());
    }

    public void load(TagCompound items) {
        setSize(items.internal.getList("slots", new CompoundTag().getType()).size());
        internal.fromTag(items.internal);
    }

}
