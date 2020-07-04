package cam72cam.mod.world;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.serialization.TagMapped;

@TagMapped(BlockInfo.TagMapper.class)
public class BlockInfo {
    final Block internal;
    final int internalMeta;

    BlockInfo(Block block, int meta) {
        internal = block;
        internalMeta = meta;
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
                        TagCompound data = new TagCompound();
                        if (o.internal != null) {
                            data.setString("block", Block.blockRegistry.getNameForObject(o.internal));
                            data.setInteger("meta", o.internalMeta);
                        }
                        d.set(fieldName, data);
                    },
                    info -> {
                        if (!info.hasKey("block")) {
                            return new BlockInfo(Blocks.air, 0);
                        }
                        return new BlockInfo(Block.getBlockFromName(info.getString("block")), info.getInteger("meta"));
                    }
            );
        }
    }
}
