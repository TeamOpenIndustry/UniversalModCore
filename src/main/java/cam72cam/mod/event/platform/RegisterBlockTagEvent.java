package cam72cam.mod.event.platform;

import net.minecraft.block.Block;
import net.minecraft.tags.ITag;
import net.minecraft.tags.Tag;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.lifecycle.IModBusEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collection;
import java.util.Map;

/**
 * Fired when block tag datapacks are reloaded
 */
public class RegisterBlockTagEvent extends Event implements IModBusEvent {
    private final Map<ResourceLocation, ITag.Builder> map;

    public RegisterBlockTagEvent(Map<ResourceLocation, ITag.Builder> map) {
        this.map = map;
    }

    public void registerTag(ResourceLocation ident, Collection<Block> includes) {
        for (Block include : includes) {
            registerTag(ident, include);
        }
    }

    public void registerTag(ResourceLocation ident, Block block) {
        Tag.Builder builder = map.getOrDefault(ident, Tag.Builder.tag());
        builder.add(new ITag.ItemEntry(ForgeRegistries.BLOCKS.getKey(block)), "universalmodcore_generated");
        map.put(ident, builder);
    }

    public void registerTag(ResourceLocation ident, ITag.INamedTag<Block> includes) {
        Tag.Builder builder = map.getOrDefault(ident, Tag.Builder.tag());
        builder.add(new ITag.TagEntry(includes.getName()), "universalmodcore_generated");
        map.put(ident, builder);
    }
}