package cam72cam.mod.item;

import net.minecraft.inventory.IInvBasic;
import net.minecraft.inventory.InventoryBasic;

import java.util.ArrayList;
import cam72cam.mod.serialization.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;

/** Standard IInventory implementation */
@TagMapped(ItemStackHandler.TagMapper.class)
public class ItemStackHandler implements IInventory {
    public ExposedItemStackHandler internal;
    protected BiPredicate<Integer, ItemStack> checkSlot = (integer, itemStack) -> true;
    private final List<Consumer<Integer>> onChanged = new ArrayList<>();
    private Function<Integer, Integer> slotLimit = null;


    private class ExposedItemStackHandler extends InventoryBasic implements IInvBasic {
        public ExposedItemStackHandler(int size) {
            super("", false, size);
        }

        public void load(List<ItemStack> stacks) {
            for (int i = 0; i < stacks.size(); i++) {
                super.setInventorySlotContents(i, stacks.get(i).internal);
            }
        }

        @Override
        public void setInventorySlotContents(int slot, net.minecraft.item.ItemStack stack) {
            if (checkSlot.test(slot, new ItemStack(stack))) {
                if (stack != null && stack.stackSize <= 0) {
                    stack = null;
                }
                super.setInventorySlotContents(slot, stack);
                onContentsChanged(slot);
            }
        }
        @Override
        public boolean isItemValidForSlot(int slot, net.minecraft.item.ItemStack stack) {
            return checkSlot.test(slot, new ItemStack(stack));
        }

        @Override
        public net.minecraft.item.ItemStack decrStackSize(int slot, int ammount) {
            net.minecraft.item.ItemStack res = super.decrStackSize(slot, ammount);
            onContentsChanged(slot);
            return res;
        }

        private void onContentsChanged(int slot) {
            onChanged.forEach(f -> f.accept(slot));
        }

        @Override
        public void onInventoryChanged(InventoryBasic p_76316_1_) {
            for (int slot = 0; slot < super.getSizeInventory(); slot++) {
                int finalSlot = slot;
                onChanged.forEach(f -> f.accept(finalSlot));
            }
        }
    }

    public ItemStackHandler(int size) {
        this.internal = new ExposedItemStackHandler(size);
    }

    public ItemStackHandler() {
        this(1);
    }

    /** Get notified any time a stack changes (which slot) */
    public void onChanged(Consumer<Integer> fn) {
        onChanged.add(fn);
    }

    /** Set slot limiter */
    public void setSlotLimit(Function<Integer, Integer> limiter) {
        slotLimit = limiter;
    }

    /** Change the size of the inventory and return items that don't fit anymore */
    public List<ItemStack> setSize(int inventorySize) {
        if (inventorySize == getSlotCount()) {
            return Collections.emptyList();
        }

        List<ItemStack> keep = new ArrayList<>();
        List<ItemStack> extra = new ArrayList<>();
        if (internal.getSizeInventory() > inventorySize) {
            for (int i = 0; i < internal.getSizeInventory(); i++) {
                (i < inventorySize ? keep : extra).add(get(i));
            }
        }
        this.internal = new ExposedItemStackHandler(inventorySize);
        for (int i = 0; i < keep.size(); i++) {
            internal.setInventorySlotContents(i, keep.get(i).internal);
        }
        return extra;
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
        internal.setInventorySlotContents(slot, stack.isEmpty() ? null : stack.internal);
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

    @Deprecated
    public TagCompound save() {
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < getSlotCount(); i++) {
            stacks.add(get(i));
        }

        TagCompound data = new TagCompound();
        data.setList("Items", stacks, ItemStack::toTag);
        return data;
    }

    @Deprecated
    public void load(TagCompound items) {
        if (items.hasKey("Items")) {
            List<ItemStack> inv = items.getList("Items", ItemStack::new);
            setSize(inv.size());
            internal.load(inv);
        }
    }

    public static class TagMapper implements cam72cam.mod.serialization.TagMapper<ItemStackHandler> {
        @Override
        public TagAccessor<ItemStackHandler> apply(Class<ItemStackHandler> type, String fieldName, TagField tag) throws SerializationException {
            Constructor<ItemStackHandler> ctr;
            try {
                ctr = type.getDeclaredConstructor();
                ctr.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new SerializationException("Unable to detect constructor for " + type, e);
            }
            return new TagAccessor<>(
                    (d, o) -> {
                        if (o == null) {
                            d.remove(fieldName);
                            return;
                        }
                        d.set(fieldName, o.save());
                    },
                    (d, w) -> {
                        try {
                            ItemStackHandler o = ctr.newInstance();
                            o.load(d.get(fieldName));
                            return o;
                        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                            throw new SerializationException("Unable to construct item stack handler " + type, e);
                        }
                    }
            );
        }
    }
}
