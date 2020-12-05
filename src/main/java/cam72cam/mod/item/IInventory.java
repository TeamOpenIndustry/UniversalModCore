package cam72cam.mod.item;

import net.minecraftforge.items.IItemHandlerModifiable;

public interface IInventory {
    /** Wraps MC construct.  Do not use */
    static IInventory from(IItemHandlerModifiable inv) {
        return new IInventory() {
            @Override
            public int getSlotCount() {
                return inv.getSlots();
            }

            @Override
            public ItemStack get(int slot) {
                return new ItemStack(inv.getStackInSlot(slot));
            }

            @Override
            public void set(int slot, ItemStack itemStack) {
                inv.setStackInSlot(slot, itemStack.internal);
            }

            @Override
            public ItemStack insert(int slot, ItemStack itemStack, boolean simulate) {
                return new ItemStack(inv.insertItem(slot, itemStack.internal, simulate));
            }

            @Override
            public ItemStack extract(int slot, int amount, boolean simulate) {
                return new ItemStack(inv.extractItem(slot, amount, simulate));
            }

            @Override
            public int getLimit(int slot) {
                return get(slot).getLimit();
            }

        };
    }

    /** Wraps MC construct.  Do not use */
    static IInventory from(net.minecraft.inventory.IInventory inventory) {
        return new IInventory() {
            @Override
            public int getSlotCount() {
                return inventory.getSizeInventory();
            }

            @Override
            public ItemStack get(int slot) {
                return new ItemStack(inventory.getStackInSlot(slot));
            }

            @Override
            public void set(int slot, ItemStack itemStack) {
                inventory.setInventorySlotContents(slot, itemStack.internal);
            }

            @Override
            public ItemStack insert(int slot, ItemStack itemStack, boolean simulate) {
                if (itemStack.isEmpty()) {
                    return itemStack;
                }

                net.minecraft.item.ItemStack current = inventory.getStackInSlot(slot);

                if (current == null || current.stackSize == 0) {
                    if (!inventory.isItemValidForSlot(slot, itemStack.internal)) {
                        return itemStack;
                    }
                    if (!simulate) {
                        set(slot, itemStack);
                    }
                    return ItemStack.EMPTY;
                }


                if (!itemStack.internal.isItemEqual(current)) {
                    return itemStack;
                }
                if (!net.minecraft.item.ItemStack.areItemStackTagsEqual(itemStack.internal, current)) {
                    return itemStack;
                }

                int space = current.getMaxStackSize() - current.stackSize;
                if (space >= 0) {
                    return itemStack;
                }

                int toMove = Math.min(space, itemStack.getCount());
                if (!simulate) {
                    ItemStack copy = itemStack.copy();
                    copy.setCount(toMove);
                    set(slot, copy);
                }

                ItemStack remainder = new ItemStack(itemStack.internal);
                remainder.setCount(itemStack.getCount() - toMove);
                return remainder;
            }

            @Override
            public ItemStack extract(int slot, int amount, boolean simulate) {
                net.minecraft.item.ItemStack backup = inventory.getStackInSlot(slot).copy();
                net.minecraft.item.ItemStack output = inventory.decrStackSize(slot, amount);
                if (simulate) {
                    inventory.setInventorySlotContents(slot, backup);
                }
                return new ItemStack(output);
            }

            @Override
            public int getLimit(int slot) {
                return 0;
            }
        };
    }

    /** Total size of this inventory */
    int getSlotCount();

    /** Stack in the slot (usually not a copy) */
    ItemStack get(int slot);

    /** Set the stack in this slot to the given stack */
    void set(int slot, ItemStack itemStack);

    /** Insert as much of itemStack as possible into slot, return remainder */
    ItemStack insert(int slot, ItemStack itemStack, boolean simulate);

    /** Extract as much of slot as possible, up to amount */
    ItemStack extract(int slot, int amount, boolean simulate);

    /** Max size of stack in slot */
    int getLimit(int slot);

    /** Try to move all items in this inventory into another inventory */
    default void transferAllTo(IInventory to) {
        for (int fromSlot = 0; fromSlot < this.getSlotCount(); fromSlot++) {
            ItemStack stack = this.get(fromSlot);
            int origCount = stack.getCount();

            if (stack.isEmpty()) {
                continue;
            }

            for (int toSlot = 0; toSlot < to.getSlotCount(); toSlot++) {
                stack = to.insert(toSlot, stack, false);
                if (stack.isEmpty()) {
                    break;
                }
            }

            if (origCount != stack.getCount()) {
                this.set(fromSlot, stack);
            }
        }
    }

    /** Try to move all items from another inventory into this inventory */
    default void transferAllFrom(IInventory from) {
        from.transferAllTo(this);
    }
}
