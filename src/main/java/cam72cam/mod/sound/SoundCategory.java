package cam72cam.mod.sound;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public enum SoundCategory {
    MASTER,
    MUSIC,
    RECORDS,
    WEATHER,
    BLOCKS,
    HOSTILE,
    NEUTRAL,
    PLAYERS,
    AMBIENT,
    VOICE,
    ;

    @SideOnly(Side.CLIENT)
    public net.minecraft.client.audio.SoundCategory internal() {
        switch (this) {
            case MASTER:
                return net.minecraft.client.audio.SoundCategory.MASTER;
            case MUSIC:
                return net.minecraft.client.audio.SoundCategory.MUSIC;
            case RECORDS:
                return net.minecraft.client.audio.SoundCategory.RECORDS;
            case WEATHER:
                return net.minecraft.client.audio.SoundCategory.WEATHER;
            case BLOCKS:
                return net.minecraft.client.audio.SoundCategory.BLOCKS;
            case HOSTILE:
                return net.minecraft.client.audio.SoundCategory.PLAYERS;
            case NEUTRAL:
                return net.minecraft.client.audio.SoundCategory.PLAYERS;
            case PLAYERS:
                return net.minecraft.client.audio.SoundCategory.PLAYERS;
            case AMBIENT:
                return net.minecraft.client.audio.SoundCategory.AMBIENT;
            case VOICE:
                return net.minecraft.client.audio.SoundCategory.PLAYERS;
        }
        throw new RuntimeException("unreachable");
    }
}
