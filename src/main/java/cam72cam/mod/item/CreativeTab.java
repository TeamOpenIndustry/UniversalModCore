package cam72cam.mod.item;

import cam72cam.mod.text.TextUtil;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Supplier;

/** Creates/Registers a creative tab for custom items */
public class CreativeTab {
    public ItemGroup internal;

    // TODO expose existing creative tabs as constants to be used by mods

    /** */
    public CreativeTab(String label, Supplier<ItemStack> stack) {
        internal = new ItemGroup(label) {
            @Override
            public net.minecraft.item.ItemStack createIcon() {
                return stack.get().internal;
            }

            @OnlyIn(Dist.CLIENT)
            public ITextComponent getGroupName() {
                return new StringTextComponent(TextUtil.translate("itemGroup." + label));
            }

        };
    }

    /** Wraps minecraft's tabs, don't use directly */
    public CreativeTab(ItemGroup tab) {
        this.internal = tab;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CreativeTab && ((CreativeTab)o).internal == this.internal;
    }
}
