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

    public void registerTag(ResourceLocation ident, Block block) {
        //Safe casting verified by event poster
        Tag.Builder<Block> builder = (Tag.Builder<Block>) map.getOrDefault(ident, Tag.Builder.create());
        builder.add(block);
        map.put(ident, builder);
    }

    public void registerTag(ResourceLocation ident, Tag<Block> includes) {
        //Safe casting verified by event poster
        Tag.Builder<Block> builder = (Tag.Builder<Block>) map.getOrDefault(ident, Tag.Builder.create());
        includes.getEntries().forEach(builder::add);
        map.put(ident, builder);
    }
}