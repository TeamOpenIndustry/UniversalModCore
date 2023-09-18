package cam72cam.mod.block;

import net.minecraft.world.level.block.SoundType;

/**
 * Wraps minecraft's material enum
 *
 * TODO pull in the rest of the options here that are availible across all supported MC versions
 */
public enum Material {
    METAL(net.minecraft.world.level.material.MapColor.METAL, SoundType.METAL),
    WOOL(net.minecraft.world.level.material.MapColor.WOOL, SoundType.WOOL),
    ;

    protected final net.minecraft.world.level.material.MapColor internal;
    protected final SoundType soundType;

    Material(net.minecraft.world.level.material.MapColor internal, SoundType soundType) {
        this.internal = internal;
        this.soundType = soundType;
    }
}
