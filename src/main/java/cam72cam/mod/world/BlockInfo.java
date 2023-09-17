package cam72cam.mod.world;

import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.serialization.TagMapped;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Optional;

/** Represents a Block in-world and all of it's data (not counting TE) */
@TagMapped(BlockInfo.TagMapper.class)
public class BlockInfo {
    final BlockState internal;

    BlockInfo(BlockState state) {
        this.internal = state;
    }

    public static class TagMapper implements cam72cam.mod.serialization.TagMapper<BlockInfo> {
        @Override
        public TagAccessor<BlockInfo> apply(Class<BlockInfo> type, String fieldName, TagField tag) {
            return new TagAccessor<>(
                    (d, o) -> {
                        if (o == null) {
                            d.remove(fieldName);
                            return;
                        }
                        d.set(fieldName, new TagCompound(NbtUtils.writeBlockState(o.internal)));
                    },
                    info -> new BlockInfo(NbtUtils.readBlockState(new HolderGetter<Block>() {
                        @Override
                        public Optional<Holder.Reference<Block>> get(ResourceKey<Block> p_255645_) {
                            // This is some of the worst OOP spaghetti I've ever encountered...
                            return ForgeRegistries.BLOCKS.getDelegate(p_255645_);
                        }

                        @Override
                        public Optional<HolderSet.Named<Block>> get(TagKey<Block> p_256283_) {
                            return Optional.empty();
                        }
                    }, info.get(fieldName).internal))
            );
        }
    }
}
