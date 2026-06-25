package cam72cam.umc.api.world;

import cam72cam.umc.api.serialization.TagCompound;
import cam72cam.umc.api.serialization.TagField;
import cam72cam.umc.api.serialization.TagMapped;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;

/** Represents a Block in-world and all of it's data (not counting TE) */
@TagMapped(BlockInfo.TagMapper.class)
public class BlockInfo {
    final IBlockState internal;

    BlockInfo(IBlockState state) {
        this.internal = state;
    }

    public static class TagMapper implements cam72cam.umc.api.serialization.TagMapper<BlockInfo> {
        @Override
        public TagAccessor<BlockInfo> apply(Class<BlockInfo> type, String fieldName, TagField tag) {
            return new TagAccessor<>(
                    (d, o) -> {
                        if (o == null) {
                            d.remove(fieldName);
                            return;
                        }
                        d.set(fieldName, new TagCompound(NBTUtil.writeBlockState(new NBTTagCompound(), o.internal)));
                    },
                    info -> new BlockInfo(NBTUtil.readBlockState(info.get(fieldName).internal))
            );
        }
    }
}
