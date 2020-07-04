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

@TagMapped(ItemStackHandler.TagMapper.class)
public class ItemStackHandler implements IInventory {
    public final net.minecraftforge.items.ItemStackHandler internal;
    protected BiPredicate<Integer, ItemStack> checkSlot = (integer, itemStack) -> true;
    private final List<Consumer<Integer>> onChanged = new ArrayList<>();
    private Function<Integer, Integer> slotLimit = null;


    public ItemStackHandler(int size) {
        this.internal = new net.minecraftforge.items.ItemStackHandler(size) {
            @Override
            public void setStackInSlot(int slot, @Nonnull net.minecraft.item.ItemStack stack) {
                if (checkSlot.test(slot, new ItemStack(stack))) {
                    super.setStackInSlot(slot, stack);
                }
            }

            @Override
            @Nonnull
            public net.minecraft.item.ItemStack insertItem(int slot, @Nonnull net.minecraft.item.ItemStack stack, boolean simulate) {
                return checkSlot.test(slot, new ItemStack(stack)) ? super.insertItem(slot, stack.copy(), simulate) : stack;
            }

            @Override
            public int getSlotLimit(int slot) {
                return slotLimit == null ? super.getSlotLimit(slot) : Math.min(super.getSlotLimit(slot), slotLimit.apply(slot));
            }

            @Override
            protected void onContentsChanged(int slot) {
                super.onContentsChanged(slot);
                ItemStackHandler.this.onContentsChanged(slot);
            }
        };
    }

    public ItemStackHandler() {
        this(1);
    }

    public void onChanged(Consumer<Integer> fn) {
        onChanged.add(fn);
    }

    @Deprecated
    protected void onContentsChanged(int slot) {
        onChanged.forEach(f -> f.accept(slot));
    }

    public void setSlotLimit(Function<Integer, Integer> limiter) {
        slotLimit = limiter;
    }

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
        internal.setStackInSlot(slot, stack.internal);
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
        return internal.getSlotLimit(slot);
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
