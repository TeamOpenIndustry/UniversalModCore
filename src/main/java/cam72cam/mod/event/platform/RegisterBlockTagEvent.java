package cam72cam.mod.event.platform;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagLoader;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Fired when block tag datapacks are reloaded
 */
public class RegisterBlockTagEvent extends Event implements IModBusEvent {
    private final Map<ResourceLocation, List<TagLoader.EntryWithSource>> map;

    public RegisterBlockTagEvent(Map<ResourceLocation, List<TagLoader.EntryWithSource>> map) {
        this.map = map;
    }

    public void registerTag(ResourceLocation ident, Collection<Block> includes) {
        for (Block include : includes) {
            registerTag(ident, include);
        }
    }

    public void registerTag(ResourceLocation ident, Block block) {
        List<TagLoader.EntryWithSource> builder = map.getOrDefault(ident, new ArrayList<>());
        builder.add(new TagLoader.EntryWithSource(TagEntry.element(BuiltInRegistries.BLOCK.getKey(block)), "universalmodcore_generated"));
        map.put(ident, builder);
    }

    public void registerTag(ResourceLocation ident, TagKey<Block> includes) {
        List<TagLoader.EntryWithSource> builder = map.getOrDefault(ident, new ArrayList<>());
        builder.add(new TagLoader.EntryWithSource(TagEntry.tag(includes.location()), "universalmodcore_generated"));
        map.put(ident, builder);
    }
}