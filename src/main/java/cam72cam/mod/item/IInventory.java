package cam72cam.mod.item;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.FixedItemInv;
import alexiil.mc.lib.attributes.item.compat.FixedInventoryVanillaWrapper;

public interface IInventory {
    static IInventory from(FixedItemInv inv) {
        return new IInventory() {
            @Override
            public int getSlotCount() {
                return inv.getSlotCount();
            }

            @Override
            public ItemStack get(int slot) {
                return new ItemStack(inv.getInvStack(slot));
            }

            @Override
            public void set(int slot, ItemStack itemStack) {
                inv.setInvStack(slot, itemStack.internal, Simulation.ACTION);
            }

            @Override
            public ItemStack insert(int slot, ItemStack itemStack, boolean simulate) {
                return new ItemStack(inv.getSlot(slot).attemptInsertion(itemStack.internal, simulate ? Simulation.SIMULATE : Simulation.ACTION));
            }

            @Override
            public ItemStack extract(int slot, int amount, boolean simulate) {
                return new ItemStack(inv.getSlot(slot).attemptExtraction(stack -> true, amount, simulate ? Simulation.SIMULATE : Simulation.ACTION));
            }

            @Override
            public int getLimit(int slot) {
                return inv.getSlot(slot).getMaxAmount(inv.getInvStack(slot));
            }

        };
    }

    static IInventory from(net.minecraft.inventory.Inventory inventory) {
        return from(new FixedInventoryVanillaWrapper(inventory));
    }

    int getSlotCount();

    ItemStack get(int slot);

    void set(int slot, ItemStack itemStack);

    ItemStack insert(int slot, ItemStack itemStack, boolean simulate);

    ItemStack extract(int slot, int amount, boolean simulate);

    int getLimit(int slot);

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

    default void transferAllFrom(IInventory from) {
        from.transferAllTo(this);
    }
}
