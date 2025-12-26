package cam72cam.mod.block;

import net.minecraft.world.level.block.SoundType;

/**
 * Wraps minecraft's material enum
 */
public enum Material {
    METAL(net.minecraft.world.level.material.Material.METAL, SoundType.METAL),
    WOOL(net.minecraft.world.level.material.Material.CLOTH_DECORATION, SoundType.WOOL),
    GRASS(net.minecraft.world.level.material.Material.GRASS, SoundType.GRASS),
    DIRT(net.minecraft.world.level.material.Material.DIRT, SoundType.GRAVEL),
    WOOD(net.minecraft.world.level.material.Material.WOOD, SoundType.WOOD),
    STONE(net.minecraft.world.level.material.Material.STONE, SoundType.STONE),
    LEAF(net.minecraft.world.level.material.Material.LEAVES, SoundType.GRASS),
    PLANT(net.minecraft.world.level.material.Material.PLANT, SoundType.GRASS),
    VINE(net.minecraft.world.level.material.Material.REPLACEABLE_PLANT, SoundType.VINE),
    SAND(net.minecraft.world.level.material.Material.SAND, SoundType.SAND),
    GLASS(net.minecraft.world.level.material.Material.GLASS, SoundType.GLASS),
    ICE(net.minecraft.world.level.material.Material.ICE, SoundType.GLASS),
    /** Will melt under high sky light*/
    SNOW(net.minecraft.world.level.material.Material.SNOW, SoundType.SNOW),
    CLAY(net.minecraft.world.level.material.Material.CLAY, SoundType.STONE),
    ;

    final net.minecraft.world.level.material.Material internal;
    final SoundType soundType;

    Material(net.minecraft.world.level.material.Material internal, SoundType soundType) {
        this.internal = internal;
        this.soundType = soundType;
    }
}
