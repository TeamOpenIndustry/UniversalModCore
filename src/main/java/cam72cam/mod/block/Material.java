package cam72cam.mod.block;

import net.minecraft.block.SoundType;

/**
 * Wraps minecraft's material enum
 */
public enum Material {
    METAL(net.minecraft.block.material.Material.METAL, SoundType.METAL),
    WOOL(net.minecraft.block.material.Material.CLOTH_DECORATION, SoundType.WOOL),
    GRASS(net.minecraft.block.material.Material.GRASS, SoundType.GRASS),
    DIRT(net.minecraft.block.material.Material.DIRT, SoundType.GRAVEL),
    WOOD(net.minecraft.block.material.Material.WOOD, SoundType.WOOD),
    STONE(net.minecraft.block.material.Material.STONE, SoundType.STONE),
    LEAF(net.minecraft.block.material.Material.LEAVES, SoundType.GRASS),
    PLANT(net.minecraft.block.material.Material.PLANT, SoundType.GRASS),
    VINE(net.minecraft.block.material.Material.REPLACEABLE_PLANT, SoundType.VINE),
    SAND(net.minecraft.block.material.Material.SAND, SoundType.SAND),
    GLASS(net.minecraft.block.material.Material.GLASS, SoundType.GLASS),
    ICE(net.minecraft.block.material.Material.ICE, SoundType.GLASS),
    /** Will melt under high sky light*/
    SNOW(net.minecraft.block.material.Material.SNOW, SoundType.SNOW),
    CLAY(net.minecraft.block.material.Material.CLAY, SoundType.STONE),
    ;

    final net.minecraft.block.material.Material internal;
    final SoundType soundType;

    Material(net.minecraft.block.material.Material internal, SoundType soundType) {
        this.internal = internal;
        this.soundType = soundType;
    }
}
