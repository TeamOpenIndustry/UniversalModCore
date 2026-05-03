package cam72cam.mod.item;

import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.mixin.accessor.ACreativeModeTab;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Creates/Registers a creative tab for custom items */
public class CreativeTab {
    private static ResourceKey<CreativeModeTab> lastTab = CreativeModeTabs.SPAWN_EGGS;

    public CreativeModeTab internal;

    // TODO expose existing creative tabs as constants to be used by mods

    public List<CustomItem> inject = new ArrayList<>();

    /** */
    public CreativeTab(String label, Supplier<ItemStack> stack) {
        ResourceKey<CreativeModeTab> key = lastTab;
        RegistryObject<CreativeModeTab> register = CommonEvents.Item.CREATIVE_TAB.register(label, () -> {
            CreativeModeTab.Builder builder = CreativeModeTab.builder();
            builder.title(Component.translatable("itemGroup." + label));
            builder.icon(() -> stack.get().internal());
            builder.displayItems((params, output) -> {
                for (CustomItem customItem : inject) {
                    for (ItemStack itemVariant : customItem.getItemVariants(CreativeTab.this)) {
                        output.accept(itemVariant.internal());
                    }
                }
            });
            builder.withTabsBefore(key);

            internal = builder.build();
            ACreativeModeTab.from(internal).setUMCTab();
            return internal;
        });
        lastTab = register.getKey();
    }

    /** Wraps minecraft's tabs, don't use directly */
    public CreativeTab(CreativeModeTab tab) {
        this.internal = tab;
    }

    /*
    @Override
    public boolean equals(Object o) {
        return o instanceof CreativeTab && ((CreativeTab)o).internal == this.internal;
    }*/
}
