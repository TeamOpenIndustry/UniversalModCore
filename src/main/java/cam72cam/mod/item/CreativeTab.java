package cam72cam.mod.item;

import net.minecraft.world.item.CreativeModeTab;

import java.util.function.Supplier;

/** Creates/Registers a creative tab for custom items */
public class CreativeTab {
    public CreativeModeTab internal;

    // TODO expose existing creative tabs as constants to be used by mods

    /** */
    public CreativeTab(String label, Supplier<ItemStack> stack) {
        internal = new CreativeModeTab(label) {
            @Override
            public net.minecraft.world.item.ItemStack makeIcon() {
                return stack.get().internal;
            }
        };
    }

    /** Wraps minecraft's tabs, don't use directly */
    public CreativeTab(CreativeModeTab tab) {
        this.internal = tab;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CreativeTab && ((CreativeTab)o).internal == this.internal;
    }
}
