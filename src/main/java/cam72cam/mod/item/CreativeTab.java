package cam72cam.mod.item;

import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Creates/Registers a creative tab for custom items */
public class CreativeTab {
    public ItemGroup internal;

    List<Consumer<List<net.minecraft.item.ItemStack>>> items = new ArrayList<>();

    // TODO expose existing creative tabs as constants to be used by mods

    /** */
    public CreativeTab(String label, Supplier<ItemStack> stack) {
        internal = FabricItemGroupBuilder
                .create(new Identifier(label.split("\\.", 2)[0], label.split("\\.",2)[1]))
                .icon(() -> stack.get().internal)
                .appendItems(list -> items.forEach(x -> x.accept(list))).build();
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
