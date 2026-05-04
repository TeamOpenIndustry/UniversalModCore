package cam72cam.mod.item;

import cam72cam.mod.entity.Player;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.util.RegistryUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.fluids.FluidUtil;

import java.util.function.Supplier;

/** Wrapper around Minecraft ItemStack (Item, count, NBT) */
public class ItemStack {
    public static final ItemStack EMPTY = new ItemStack(net.minecraft.world.item.ItemStack.EMPTY);

    private final Supplier<net.minecraft.world.item.ItemStack> supplier;
    private net.minecraft.world.item.ItemStack internalLazy;

    /** Wrap Minecraft ItemStack.  Do not use directly */
    public ItemStack(net.minecraft.world.item.ItemStack internal) {
        this.internalLazy = internal;
        this.supplier = null;
    }

    /** Deserialize from tag */
    public ItemStack(TagCompound nbt) {
        this(net.minecraft.world.item.ItemStack.parseOptional(RegistryUtil.getRegistry(),
                                                              (nbt.hasKey("id") && nbt.getString("id").equals("minecraft:air")) ? new CompoundTag() : nbt.internal));
        if (nbt.hasKey("tag")) {
            CompoundTag merged = getTagCompound().internal.merge(nbt.get("tag").internal);
            internal().set(DataComponents.CUSTOM_DATA, CustomData.of(merged));
        }
    }

    /** Construct from customItem */
    public ItemStack(CustomItem item, int i) {
        supplier = () -> item.internal == null ?
                net.minecraft.world.item.ItemStack.EMPTY : //1.20.1 (-2) This will break things...
                new net.minecraft.world.item.ItemStack(item.internal, i);
    }

    @Deprecated
    public ItemStack(String item, int i, int meta) {
        this(new net.minecraft.world.item.ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(item)), i));
    }

    public net.minecraft.world.item.ItemStack internal() {
        if (internalLazy == null) {
            internalLazy = supplier.get();
        }
        return internalLazy;
    }

    /** Tag attached to this stack */
    public TagCompound getTagCompound() {
        if (!internal().has(DataComponents.CUSTOM_DATA)) {
            internal().set(DataComponents.CUSTOM_DATA, CustomData.of(new CompoundTag()));
        }
        CustomData customData = internal().get(DataComponents.CUSTOM_DATA);
        return new TagCompound(customData == null ? new CompoundTag() : customData.getUnsafe());
    }

    /** Tag attached to this stack */
    public void setTagCompound(TagCompound data) {
        internal().set(DataComponents.CUSTOM_DATA, CustomData.of(data.internal));
    }

    public ItemStack copy() {
        return new ItemStack(internal().copy());
    }

    /** Serialize */
    public TagCompound toTag() {
        if (internal().isEmpty()) {
            return new TagCompound();
        }
        return new TagCompound((CompoundTag) internal().save(RegistryUtil.getRegistry()));
    }

    /** Items in this stack */
    public int getCount() {
        return internal().getCount();
    }

    /** Set the items in this stack */
    public void setCount(int count) {
        internal().setCount(count);
    }

    /** Human readable name */
    public String getDisplayName() {
        return internal().getHoverName().getString();
    }

    /** Is count zero? */
    public boolean isEmpty() {
        return internal().isEmpty();
    }

    /** Reduce stack size by i */
    public void shrink(int i) {
        internal().shrink(i);
    }

    /** Compares: item, count, damage, data */
    @Override
    public boolean equals(Object other) {
        return other instanceof ItemStack && net.minecraft.world.item.ItemStack.matches(internal(), ((ItemStack)other).internal());
    }


    /** Compares: item, damage */
    public boolean is(ItemStack stack) {
        return net.minecraft.world.item.ItemStack.isSameItem(internal(), stack.internal());
    }

    public boolean is(Fuzzy fuzzy) {
        return fuzzy.matches(this);
    }

    public boolean is(CustomItem item) {
        return item.internal == this.internal().getItem();
    }

    /** Is a bucket or similar item */
    public boolean isFluidContainer() {
        return FluidUtil.getFluidHandler(internal()).isPresent();
    }

    /** Can be burnt in a furnace */
    public boolean isFlammable() {
        return getBurnTime() != 0;
    }

    /** Ticks item will burn in a furnace (Make sure you multiply by count to get total burn time) */
    public int getBurnTime() {
        return internal().getBurnTime(RecipeType.SMELTING);
    }

    /** Max count of the stack */
    public int getLimit() {
        return internal().getMaxStackSize();
    }

    /** Is the item this type of tool? */
    public boolean isValidTool(ToolType tool) {
        return internal().getItem().canPerformAction(internal(), tool.internal);
    }

    @Override
    public String toString() {
        return internal().toString();
    }

    /** Increase the damage counter on the item by the player */
    public void damageItem(int i, Player player) {
        internal().hurtAndBreak(i, (ServerLevel) player.internal.level(), player.internal, (s) -> {});
    }

    /** Completely null out the tag compound */
    public void clearTagCompound() {
        internal().set(DataComponents.CUSTOM_DATA, null);
    }
}
