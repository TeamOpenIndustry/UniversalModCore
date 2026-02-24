package cam72cam.mod.event.platform;

import cam72cam.mod.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.tags.ITag;
import net.minecraft.tags.Tag;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.lifecycle.IModBusEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collection;
import java.util.Map;

/**
 * Fired when item tag datapacks are reloaded
 */
public class RegisterItemTagEvent extends Event implements IModBusEvent {
    private final Map<ResourceLocation, Tag.Builder> map;

    public RegisterItemTagEvent(Map<ResourceLocation, Tag.Builder> map) {
        this.map = map;
    }

    public void registerTag(ResourceLocation ident, Collection<Item> includes) {
        for (Item include : includes) {
            registerTag(ident, include);
        }
    }

    public void registerTag(ResourceLocation ident, Item item) {
        Tag.Builder builder = map.getOrDefault(ident, Tag.Builder.tag());
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        builder.add(new ITag.ItemEntry(key), "universalmodcore_generated");
        map.put(ident, builder);
    }

    public void registerTag(ResourceLocation ident, ITag.INamedTag<Item> includes) {
        Tag.Builder builder = map.getOrDefault(ident, Tag.Builder.tag());
        //Don't pass in direct tag reference
        builder.add(new Tag.TagEntry(includes.getName()), "universalmodcore_generated");
        map.put(ident, builder);
    }

    public void registerTag(ResourceLocation ident, ItemStack itemStack) {
        Tag.Builder builder = map.getOrDefault(ident, Tag.Builder.tag());
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(itemStack.internal.getItem());
        builder.add(new ITag.ItemEntry(key), "universalmodcore_generated");
        map.put(ident, builder);
    }
}