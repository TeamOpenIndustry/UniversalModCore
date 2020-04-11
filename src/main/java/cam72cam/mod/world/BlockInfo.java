package cam72cam.mod.world;

import cam72cam.mod.util.TagCompound;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtHelper;

public class BlockInfo {
    final BlockState internal;

    BlockInfo(BlockState state) {
        this.internal = state;
    }

    public BlockInfo(TagCompound info) {
        internal = NbtHelper.toBlockState(info.internal);
    }

    public TagCompound toNBT() {
        return new TagCompound(NbtHelper.fromBlockState(internal));
    }
}
