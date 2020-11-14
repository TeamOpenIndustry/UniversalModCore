package cam72cam.mod.item;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;
import alexiil.mc.lib.attributes.item.impl.FullFixedItemInv;
import net.minecraft.nbt.CompoundTag;
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
    public ItemInv internal;
    protected BiPredicate<Integer, ItemStack> checkSlot = (integer, itemStack) -> true;
    private final List<Consumer<Integer>> onChanged = new ArrayList<>();
    private Function<Integer, Integer> slotLimit = null;


    protected class ItemInv extends FullFixedItemInv {
        public ItemInv(int invSize) {
            super(invSize);

            this.addListener((inv, slot, previous, current) -> {
                onChanged.forEach(f -> f.accept(slot));
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

        @Override
        public int getMaxAmount(int slot, net.minecraft.item.ItemStack stack) {
            return slotLimit == null ? super.getMaxAmount(slot, stack) : Math.min(super.getMaxAmount(slot, stack), slotLimit.apply(slot));
        }
    }

    public ItemStackHandler(int size) {
        this.internal = new ItemInv(size);
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
        if (internal.getSlotCount() > inventorySize) {
            for (int i = 0; i < internal.getSlotCount(); i++) {
                (i < inventorySize ? keep : extra).add(get(i));
            }
        }
        internal = new ItemInv(inventorySize);
        for (int i = 0; i < keep.size(); i++) {
            internal.forceSetInvStack(i, keep.get(i).internal);
        }
        return extra;
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

    @Deprecated
    public TagCompound save() {
        return new TagCompound(internal.toTag());
    }

    @Deprecated
    public void load(TagCompound items) {
        setSize(items.internal.getList("slots", new CompoundTag().getType()).size());
        internal.fromTag(items.internal);
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
                        d.set(fieldName, new TagCompound(o.internal.toTag()));
                    },
                    (d, w) -> {
                        try {
                            ItemStackHandler o = ctr.newInstance();
                            o.internal.fromTag(d.get(fieldName).internal);
                            return o;
                        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                            throw new SerializationException("Unable to construct item stack handler " + type, e);
                        }
                    }
            );
        }
    }
}
