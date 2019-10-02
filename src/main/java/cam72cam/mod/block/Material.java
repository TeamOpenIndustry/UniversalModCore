package cam72cam.mod.block;


import net.minecraft.sound.BlockSoundGroup;

public enum Material {
    METAL(net.minecraft.block.Material.METAL, BlockSoundGroup.METAL),
    WOOL(net.minecraft.block.Material.CARPET, BlockSoundGroup.WOOL),
    ;

    protected final net.minecraft.block.Material internal;
    protected final BlockSoundGroup soundType;

    Material(net.minecraft.block.Material internal, BlockSoundGroup soundType) {
        this.internal = internal;
        this.soundType = soundType;
    }
}
