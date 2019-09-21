package cam72cam.mod.item;

import net.minecraft.creativetab.CreativeTabs;

import java.util.function.Supplier;

public class CreativeTab {
    public CreativeTabs internal;

    public CreativeTab(String label, Supplier<ItemStack> stack) {
        internal = new CreativeTabs(label) {
            @Override
            public net.minecraft.item.Item getTabIconItem() {
                return stack.get().internal.getItem();
            }
        };
    }

    public CreativeTab(CreativeTabs tab) {
        this.internal = tab;
    }

    public boolean equals(CreativeTab tab) {
        return tab.internal == this.internal;
    }
}
