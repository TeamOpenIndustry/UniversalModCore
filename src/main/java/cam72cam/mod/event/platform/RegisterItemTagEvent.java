package cam72cam.mod.event.platform;

import cam72cam.mod.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.tags.Tag;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.eventbus.api.Event;

import java.util.Collection;
import java.util.Map;

/**
 * Fired when item tag datapacks are reloaded
 */
public class RegisterItemTagEvent extends Event {
    private final Map<ResourceLocation, Tag.Builder<?>> map;

    public RegisterItemTagEvent(Map<ResourceLocation, Tag.Builder<?>> map) {
        this.map = map;
    }

    public void registerTag(ResourceLocation ident, Collection<Item> includes) {
        for (Item include : includes) {
            registerTag(ident, include);
        }
    }

    public void registerTag(ResourceLocation ident, Item item) {
        //Safe casting verified by event poster
        Tag.Builder<Item> builder = (Tag.Builder<Item>) map.getOrDefault(ident, Tag.Builder.create());
        builder.add(item);
        map.put(ident, builder);
    }

    public void registerTag(ResourceLocation ident, Tag<Item> includes) {
        //Safe casting verified by event poster
        Tag.Builder<Item> builder = (Tag.Builder<Item>) map.getOrDefault(ident, Tag.Builder.create());
        //Don't pass in direct tag reference
        builder.add(new Tag.TagEntry<>(includes.getId()));
        map.put(ident, builder);
    }

    public void registerTag(ResourceLocation ident, ItemStack itemStack) {
        //Safe casting verified by event poster
        Tag.Builder<Item> builder = (Tag.Builder<Item>) map.getOrDefault(ident, Tag.Builder.create());
        builder.add(itemStack.internal.getItem());
        map.put(ident, builder);
    }
}