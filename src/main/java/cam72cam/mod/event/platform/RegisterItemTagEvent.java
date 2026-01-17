package cam72cam.mod.event.platform;

import net.minecraft.item.Item;
import net.minecraft.tags.Tag;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.eventbus.api.Event;

import java.util.Collection;
import java.util.Map;

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
        Tag.Builder<Item> builder = (Tag.Builder<Item>) map.getOrDefault(ident, Tag.Builder.create());
        builder.add(item);
        map.put(ident, builder);
    }

    public void registerTag(ResourceLocation ident, Tag<Item> includes) {
        Tag.Builder<Item> builder = (Tag.Builder<Item>) map.getOrDefault(ident, Tag.Builder.create());
        includes.getEntries().forEach(builder::add);
        map.put(ident, builder);
    }
}
