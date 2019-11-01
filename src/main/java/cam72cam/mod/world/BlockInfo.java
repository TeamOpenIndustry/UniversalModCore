package cam72cam.mod.world;

import cam72cam.mod.util.TagCompound;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

public class BlockInfo {
    final Block internal;
    final int internalMeta;

    BlockInfo(Block block, int meta) {
        internal = block;
        internalMeta = meta;
    }

    public BlockInfo(TagCompound info) {
        if (info.hasKey("block")) {
            internal = Block.getBlockFromName(info.getString("block"));
            internalMeta = info.getInteger("meta");
        } else {
            internal = Blocks.air;
            internalMeta = 0;
        }
    }

    public TagCompound toNBT() {
        TagCompound data = new TagCompound();
        if (internal != null) {
            data.setString("block", Block.blockRegistry.getNameForObject(internal));
            data.setInteger("meta", internalMeta);
        }
        return data;
    }
}
