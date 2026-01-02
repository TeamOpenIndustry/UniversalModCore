package cam72cam.mod.event.platform;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.tags.Tag;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.eventbus.api.Event;

import java.util.Collection;
import java.util.Map;

public class RegisterBlockTagEvent extends Event {
    private Map<ResourceLocation, Tag.Builder<?>> map;

    public RegisterBlockTagEvent(Map<ResourceLocation, Tag.Builder<?>> map) {
        this.map = map;
    }

    public void registerTag(ResourceLocation ident, Collection<Block> includes) {
        for (Block include : includes) {
            registerTag(ident, include);
        }
    }

    public void registerTag(ResourceLocation ident, Block item) {
        Tag.Builder<Block> builder = (Tag.Builder<Block>) map.getOrDefault(ident, Tag.Builder.create());
        builder.add(item);
        map.put(ident, builder);
    }

    public void registerTag(ResourceLocation ident, Tag<Block> includes) {
        Tag.Builder<Block> builder = (Tag.Builder<Block>) map.getOrDefault(ident, Tag.Builder.create());
        builder.add(includes);
        map.put(ident, builder);
    }
}
