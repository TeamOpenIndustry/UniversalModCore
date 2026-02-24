package cam72cam.mod.event.platform;

import cam72cam.mod.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;
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
        builder.add(new Tag.ElementEntry(key), "universalmodcore_generated");
        map.put(ident, builder);
    }

    public void registerTag(ResourceLocation ident, TagKey<Item> includes) {
        Tag.Builder builder = map.getOrDefault(ident, Tag.Builder.tag());
        //Don't pass in direct tag reference
        builder.add(new Tag.TagEntry(includes.location()), "universalmodcore_generated");
        map.put(ident, builder);
    }

    public void registerTag(ResourceLocation ident, ItemStack itemStack) {
        Tag.Builder builder = map.getOrDefault(ident, Tag.Builder.tag());
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(itemStack.internal().getItem());
        builder.add(new Tag.ElementEntry(key), "universalmodcore_generated");
        map.put(ident, builder);
    }
}