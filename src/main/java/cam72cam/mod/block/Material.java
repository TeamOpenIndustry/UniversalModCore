package cam72cam.mod.block;

import net.minecraft.block.Block;
import net.minecraft.block.Block.SoundType;

public enum Material {
    METAL(net.minecraft.block.material.Material.iron, Block.soundTypeMetal),
    WOOL(net.minecraft.block.material.Material.carpet, Block.soundTypeCloth),
    ;

    protected final net.minecraft.block.material.Material internal;
    protected final SoundType soundType;

    Material(net.minecraft.block.material.Material internal, SoundType soundType) {
        this.internal = internal;
        this.soundType = soundType;
    }
}
