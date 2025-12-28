package cam72cam.mod.block;

import net.minecraft.block.Block;
import net.minecraft.block.Block.SoundType;

/**
 * Wraps minecraft's material enum
 */
public enum Material {
    METAL(net.minecraft.block.material.Material.iron, Block.soundTypeMetal),
    WOOL(net.minecraft.block.material.Material.carpet, Block.soundTypeCloth),
    GRASS(net.minecraft.block.material.Material.grass, Block.soundTypeGrass),
    DIRT(net.minecraft.block.material.Material.ground, Block.soundTypeGravel),
    WOOD(net.minecraft.block.material.Material.wood, Block.soundTypeWood),
    STONE(net.minecraft.block.material.Material.rock, Block.soundTypeStone),
    LEAF(net.minecraft.block.material.Material.leaves, Block.soundTypeGrass),
    PLANT(net.minecraft.block.material.Material.plants, Block.soundTypeGrass),
    VINE(net.minecraft.block.material.Material.vine, Block.soundTypeGrass),
    SAND(net.minecraft.block.material.Material.sand, Block.soundTypeSand),
    GLASS(net.minecraft.block.material.Material.glass, Block.soundTypeGlass),
    ICE(net.minecraft.block.material.Material.ice, Block.soundTypeGlass),
    /** Will melt under high sky light*/
    SNOW(net.minecraft.block.material.Material.craftedSnow, Block.soundTypeSnow),
    CLAY(net.minecraft.block.material.Material.clay, Block.soundTypeSnow),
    ;

    final net.minecraft.block.material.Material internal;
    final SoundType soundType;

    Material(net.minecraft.block.material.Material internal, SoundType soundType) {
        this.internal = internal;
        this.soundType = soundType;
    }
}
