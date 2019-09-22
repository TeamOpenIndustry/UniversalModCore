package cam72cam.mod.item;

import cam72cam.mod.util.TagCompound;
import net.minecraft.inventory.IInvBasic;
import net.minecraft.inventory.InventoryBasic;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

public class ItemStackHandler implements IInventory {
    public ExposedItemStackHandler internal;
    protected BiPredicate<Integer, ItemStack> checkSlot = (integer, itemStack) -> true;

    private class ExposedItemStackHandler extends InventoryBasic implements IInvBasic {
        public ExposedItemStackHandler(int size) {
            super("", false, size);
        }

        @Override
        public void onInventoryChanged(InventoryBasic p_76316_1_) {

        }

        public void load(List<ItemStack> stacks) {
            for (int i = 0; i < stacks.size(); i++) {
                super.setInventorySlotContents(i, stacks.get(i).internal);
            }
        }
    }

    public ItemStackHandler(int size) {
        this.internal = new ExposedItemStackHandler(size) {
            @Override
            public void setInventorySlotContents(int slot, @Nonnull net.minecraft.item.ItemStack stack) {
                if (checkSlot.test(slot, new ItemStack(stack))) {
                    super.setInventorySlotContents(slot, stack.copy());
                    onContentsChanged(slot);
                }
            }
            @Override
            public net.minecraft.item.ItemStack decrStackSize(int slot, int ammount) {
                net.minecraft.item.ItemStack res = super.decrStackSize(slot, ammount);
                onContentsChanged(slot);
                return res;
            }

            @Override
            public void onInventoryChanged(InventoryBasic p_76316_1_) {
                super.onInventoryChanged(p_76316_1_);
                for (int slot = 0; slot < super.getSizeInventory(); slot++) {
                    onContentsChanged(slot);
                }
            }
        };
    }

    public ItemStackHandler() {
        this(1);
    }

    protected void onContentsChanged(int slot) {
        //NOP
    }

    public void setSize(int size) {
        //internal.setSize(inventorySize);
        // TODO 1.7.10 COPY CONTENTS

        this.internal = new ExposedItemStackHandler(size) {
            @Override
            public void setInventorySlotContents(int slot, net.minecraft.item.ItemStack stack) {
                if (checkSlot.test(slot, new ItemStack(stack))) {
                    super.setInventorySlotContents(slot, stack != null ? stack.copy() : null);
                    onContentsChanged(slot);
                }
            }
            @Override
            public net.minecraft.item.ItemStack decrStackSize(int slot, int ammount) {
                net.minecraft.item.ItemStack res = super.decrStackSize(slot, ammount);
                onContentsChanged(slot);
                return res;
            }

            @Override
            public void onInventoryChanged(InventoryBasic p_76316_1_) {
                super.onInventoryChanged(p_76316_1_);
                for (int slot = 0; slot < super.getSizeInventory(); slot++) {
                    onContentsChanged(slot);
                }
            }
        };
    }

    @Override
    public int getSlotCount() {
        return internal.getSizeInventory();
    }

    @Override
    public ItemStack get(int slot) {
        return new ItemStack(internal.getStackInSlot(slot));
    }

    @Override
    public void set(int slot, ItemStack stack) {
        internal.setInventorySlotContents(slot, stack.internal);
    }

    @Override
    public ItemStack insert(int slot, ItemStack stack, boolean simulate) {
        return IInventory.from(internal).insert(slot, stack, simulate);
    }

    @Override
    public ItemStack extract(int slot, int amount, boolean simulate) {
        return IInventory.from(internal).extract(slot, amount, simulate);
    }

    @Override
    public int getLimit(int slot) {
        return IInventory.from(internal).getLimit(slot);
    }

    public TagCompound save() {
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < getSlotCount(); i++) {
            stacks.add(get(i));
        }

        TagCompound data = new TagCompound();
        data.setList("Items", stacks, ItemStack::getTagCompound);
        return data;
    }

    public void load(TagCompound items) {
        if (items.hasKey("Items")) {
            List<ItemStack> inv = items.getList("Items", ItemStack::new);
            setSize(inv.size());
            internal.load(inv);
        }
    }

}
