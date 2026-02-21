package cam72cam.mod.event.platform;

import net.minecraft.block.Block;
import net.minecraft.tags.Tag;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.eventbus.api.Event;

import java.util.Collection;
import java.util.Map;

/**
 * Fired when block tag datapacks are reloaded
 */
public class RegisterBlockTagEvent extends Event {
    private final Map<ResourceLocation, Tag.Builder<?>> map;

    public RegisterBlockTagEvent(Map<ResourceLocation, Tag.Builder<?>> map) {
        this.map = map;
    }

    public void registerTag(ResourceLocation ident, Collection<Block> includes) {
        for (Block include : includes) {
            registerTag(ident, include);
        }
    }

    @SuppressWarnings("unchecked")
    public void registerTag(ResourceLocation ident, Block block) {
        Tag.Builder<Block> builder = (Tag.Builder<Block>) map.getOrDefault(ident, Tag.Builder.create());
        builder.add(block);
        map.put(ident, builder);
    }

    @SuppressWarnings("unchecked")
    public void registerTag(ResourceLocation ident, Tag<Block> includes) {
        Tag.Builder<Block> builder = (Tag.Builder<Block>) map.getOrDefault(ident, Tag.Builder.create());
        builder.add(new Tag.TagEntry<>(includes.getId()));
        map.put(ident, builder);
    }
}