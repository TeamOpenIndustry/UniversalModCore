package cam72cam.mod.world;

import cam72cam.mod.util.TagCompound;
import net.minecraft.block.Block;

public class BlockInfo {
    final Block internal;
    final int internalMeta;

    BlockInfo(Block block, int meta) {
        internal = block;
        internalMeta = meta;
    }

    public BlockInfo(TagCompound info) {
        internal = Block.getBlockFromName(info.getString("block"));
        internalMeta = info.getInteger("meta");
    }

    public TagCompound toNBT() {
        TagCompound data = new TagCompound();
        data.setString("block", internal.getUnlocalizedName());
        data.setInteger("meta", internalMeta);
        return data;
    }
}
