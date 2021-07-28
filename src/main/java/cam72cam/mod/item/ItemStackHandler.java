package cam72cam.mod.item;

import cam72cam.mod.serialization.*;

import javax.annotation.Nonnull;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;

/** Standard IInventory implementation */
@TagMapped(ItemStackHandler.TagMapper.class)
public class ItemStackHandler implements IInventory {
    public final ExposedItemStackHandler internal;
    protected BiPredicate<Integer, ItemStack> checkSlot = (integer, itemStack) -> true;
    private final List<Consumer<Integer>> onChanged = new ArrayList<>();
    private Function<Integer, Integer> slotLimit = null;


    private class ExposedItemStackHandler extends net.minecraftforge.items.ItemStackHandler {
        public ExposedItemStackHandler(int size) {
            super(size);
        }

        @Override
        public int getStackLimit(int slot, net.minecraft.item.ItemStack stack) {
            return super.getStackLimit(slot, stack);
        }
    }

    public ItemStackHandler(int size) {
        this.internal = new ExposedItemStackHandler(size) {
            @Override
            public void setStackInSlot(int slot, net.minecraft.item.ItemStack stack) {
                if (checkSlot.test(slot, new ItemStack(stack))) {
                    if (stack != null && stack.stackSize <= 0) {
                        stack = null;
                    }
                    super.setStackInSlot(slot, stack);
                }
            }

            @Override
            @Nonnull
            public net.minecraft.item.ItemStack insertItem(int slot, @Nonnull net.minecraft.item.ItemStack stack, boolean simulate) {
                return checkSlot.test(slot, new ItemStack(stack)) ? super.insertItem(slot, stack.copy(), simulate) : stack;
            }

            @Override
            protected void onContentsChanged(int slot) {
                super.onContentsChanged(slot);
                onChanged.forEach(f -> f.accept(slot));
            }
        };
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
        if (internal.getSlots() > inventorySize) {
            for (int i = 0; i < internal.getSlots(); i++) {
                (i < inventorySize ? keep : extra).add(get(i));
            }
        }
        internal.setSize(inventorySize);
        for (int i = 0; i < keep.size(); i++) {
            internal.setStackInSlot(i, keep.get(i).internal);
        }
        return extra;
    }

    @Override
    public int getSlotCount() {
        return internal.getSlots();
    }

    @Override
    public ItemStack get(int slot) {
        return new ItemStack(internal.getStackInSlot(slot));
    }

    @Override
    public void set(int slot, ItemStack stack) {
        if (stack.internal != null) {
            internal.setStackInSlot(slot, stack.internal);
        } else if (internal.getStackInSlot(slot) != null){
            internal.extractItem(slot, internal.getStackInSlot(slot).stackSize, false);
        }
    }

    @Override
    public ItemStack insert(int slot, ItemStack stack, boolean simulate) {
        return new ItemStack(internal.insertItem(slot, stack.internal, simulate));
    }

    @Override
    public ItemStack extract(int slot, int amount, boolean simulate) {
        return new ItemStack(internal.extractItem(slot, amount, simulate));
    }

    @Override
    public int getLimit(int slot) {
        return internal.getStackLimit(slot, internal.getStackInSlot(slot));
    }

    @Deprecated
    public TagCompound save() {
        return new TagCompound(internal.serializeNBT());
    }

    @Deprecated
    public void load(TagCompound items) {
        internal.deserializeNBT(items.internal);
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
                        d.set(fieldName, new TagCompound(o.internal.serializeNBT()));
                    },
                    (d, w) -> {
                        try {
                            ItemStackHandler o = ctr.newInstance();
                            o.internal.deserializeNBT(d.get(fieldName).internal);
                            return o;
                        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                            throw new SerializationException("Unable to construct item stack handler " + type, e);
                        }
                    }
            );
        }
    }
}
