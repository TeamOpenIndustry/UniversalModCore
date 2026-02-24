package cam72cam.mod.event.platform;

import cam72cam.mod.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagLoader;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Fired when item tag datapacks are reloaded
 */
public class RegisterItemTagEvent extends Event implements IModBusEvent {
    private final Map<ResourceLocation, List<TagLoader.EntryWithSource>> map;

    public RegisterItemTagEvent(Map<ResourceLocation, List<TagLoader.EntryWithSource>> map) {
        this.map = map;
    }

    public void registerTag(ResourceLocation ident, Collection<Item> includes) {
        for (Item include : includes) {
            registerTag(ident, include);
        }
    }

    public void registerTag(ResourceLocation ident, Item item) {
        List<TagLoader.EntryWithSource> builder = map.getOrDefault(ident, new ArrayList<>());
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        builder.add(new TagLoader.EntryWithSource(TagEntry.element(key), "universalmodcore_generated"));
        map.put(ident, builder);
    }

    public void registerTag(ResourceLocation ident, TagKey<Item> includes) {
        List<TagLoader.EntryWithSource> builder = map.getOrDefault(ident, new ArrayList<>());
        //Don't pass in direct tag reference
        builder.add(new TagLoader.EntryWithSource(TagEntry.tag(includes.location()), "universalmodcore_generated"));
        map.put(ident, builder);
    }

    public void registerTag(ResourceLocation ident, ItemStack itemStack) {
        List<TagLoader.EntryWithSource> builder = map.getOrDefault(ident, new ArrayList<>());
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(itemStack.internal().getItem());
        builder.add(new TagLoader.EntryWithSource(TagEntry.element(key), "universalmodcore_generated"));
        map.put(ident, builder);
    }
}