package cam72cam.mod.block;

import net.minecraft.world.level.block.SoundType;

/**
 * Wraps minecraft's material enum
 *
 * TODO pull in the rest of the options here that are availible across all supported MC versions
 */
public enum Material {
    METAL(net.minecraft.world.level.material.Material.METAL, SoundType.METAL),
    WOOL(net.minecraft.world.level.material.Material.CLOTH_DECORATION, SoundType.WOOL),
    ;

    protected final net.minecraft.world.level.material.Material internal;
    protected final SoundType soundType;

    Material(net.minecraft.world.level.material.Material internal, SoundType soundType) {
        this.internal = internal;
        this.soundType = soundType;
    }
}
