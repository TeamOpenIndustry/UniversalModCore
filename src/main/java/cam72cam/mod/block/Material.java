package cam72cam.mod.block;

import net.minecraft.block.SoundType;

/**
 * Wraps minecraft's material enum
 */
public enum Material {
    METAL(net.minecraft.block.material.Material.IRON, SoundType.METAL),
    WOOL(net.minecraft.block.material.Material.CARPET, SoundType.CLOTH),
    GRASS(net.minecraft.block.material.Material.GRASS, SoundType.PLANT),
    DIRT(net.minecraft.block.material.Material.GROUND, SoundType.GROUND),
    WOOD(net.minecraft.block.material.Material.WOOD, SoundType.WOOD),
    STONE(net.minecraft.block.material.Material.ROCK, SoundType.STONE),
    LEAF(net.minecraft.block.material.Material.LEAVES, SoundType.PLANT),
    PLANT(net.minecraft.block.material.Material.PLANTS, SoundType.PLANT),
    VINE(net.minecraft.block.material.Material.VINE, SoundType.PLANT),
    SAND(net.minecraft.block.material.Material.SAND, SoundType.SAND),
    GLASS(net.minecraft.block.material.Material.GLASS, SoundType.GLASS),
    ICE(net.minecraft.block.material.Material.ICE, SoundType.GLASS),
    /** Will melt under high sky light*/
    SNOW(net.minecraft.block.material.Material.CRAFTED_SNOW, SoundType.SNOW),
    CLAY(net.minecraft.block.material.Material.CLAY, SoundType.STONE),
    ;

    final net.minecraft.block.material.Material internal;
    final SoundType soundType;

    Material(net.minecraft.block.material.Material internal, SoundType soundType) {
        this.internal = internal;
        this.soundType = soundType;
    }
}
