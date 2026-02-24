package cam72cam.mod.event.platform;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.Tag;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collection;
import java.util.Map;

/**
 * Fired when block tag datapacks are reloaded
 */
public class RegisterBlockTagEvent extends Event implements IModBusEvent {
    private final Map<ResourceLocation, Tag.Builder> map;

    public RegisterBlockTagEvent(Map<ResourceLocation, Tag.Builder> map) {
        this.map = map;
    }

    public void registerTag(ResourceLocation ident, Collection<Block> includes) {
        for (Block include : includes) {
            registerTag(ident, include);
        }
    }

    public void registerTag(ResourceLocation ident, Block block) {
        Tag.Builder builder = map.getOrDefault(ident, Tag.Builder.tag());
        builder.add(new Tag.ElementEntry(ForgeRegistries.BLOCKS.getKey(block)), "universalmodcore_generated");
        map.put(ident, builder);
    }

    public void registerTag(ResourceLocation ident, Tag.Named<Block> includes) {
        Tag.Builder builder = map.getOrDefault(ident, Tag.Builder.tag());
        builder.add(new Tag.TagEntry(includes.getName()), "universalmodcore_generated");
        map.put(ident, builder);
    }
}