package cam72cam.mod.item;

import net.minecraft.creativetab.CreativeTabs;

import java.util.function.Supplier;

/** Creates/Registers a creative tab for custom items */
public class CreativeTab {
    public CreativeTabs internal;

    // TODO expose existing creative tabs as constants to be used by mods

    /** */
    public CreativeTab(String label, Supplier<ItemStack> stack) {
        internal = new CreativeTabs(label) {
            @Override
            public net.minecraft.item.Item getTabIconItem() {
                return stack.get().internal.getItem();
            }

            @Override
            public net.minecraft.item.ItemStack getIconItemStack() {
                return stack.get().internal;
            }
        };
    }

    /** Wraps minecraft's tabs, don't use directly */
    public CreativeTab(CreativeTabs tab) {
        this.internal = tab;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CreativeTab && ((CreativeTab)o).internal == this.internal;
    }
}
