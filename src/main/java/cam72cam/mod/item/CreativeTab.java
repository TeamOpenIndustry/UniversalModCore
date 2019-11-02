package cam72cam.mod.item;

import cam72cam.mod.text.TextUtil;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.minecraft.item.ItemGroup;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class CreativeTab {
    public ItemGroup internal;

    List<Consumer<List<net.minecraft.item.ItemStack>>> items = new ArrayList<>();

    public CreativeTab(String label, Supplier<ItemStack> stack) {
        internal = FabricItemGroupBuilder
                .create(new Identifier(label.split("\\.", 2)[0], label.split("\\.",2)[1]))
                .icon(() -> stack.get().internal)
                .appendItems(list -> items.forEach(x -> x.accept(list))).build();
    }


    public CreativeTab(ItemGroup tab) {
        this.internal = tab;
    }

    public boolean equals(CreativeTab tab) {
        return tab.internal == this.internal;
    }
}
