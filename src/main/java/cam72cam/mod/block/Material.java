package cam72cam.mod.block;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/**
 * Wraps minecraft's material enum
 */
public enum Material {
    METAL(MapColor.METAL, SoundType.METAL),
    WOOL(MapColor.WOOL, SoundType.WOOL),
    GRASS(MapColor.GRASS, SoundType.GRASS),
    DIRT(MapColor.DIRT, SoundType.GRAVEL),
    WOOD(MapColor.WOOD, SoundType.WOOD),
    STONE(MapColor.STONE, SoundType.STONE),
    LEAF(MapColor., SoundType.GRASS),
    PLANT(MapColor.PLANT, SoundType.GRASS),
    VINE(MapColor.REPLACEABLE_PLANT, SoundType.VINE),
    SAND(MapColor.SAND, SoundType.SAND),
    GLASS(MapColor.GLASS, SoundType.GLASS),
    ICE(MapColor.ICE, SoundType.GLASS),
    /** Will melt under high sky light*/
    SNOW(MapColor.SNOW, SoundType.SNOW),
    CLAY(MapColor.CLAY, SoundType.STONE),
    ;

    final MapColor internal;
    final SoundType soundType;

    Material(MapColor internal, SoundType soundType) {
        this.internal = internal;
        this.soundType = soundType;
    }
}
