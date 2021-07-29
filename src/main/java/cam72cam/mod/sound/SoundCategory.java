package cam72cam.mod.sound;

import net.minecraft.sounds.SoundSource;

public enum SoundCategory {
    MASTER(SoundSource.MASTER),
    MUSIC(SoundSource.MUSIC),
    RECORDS(SoundSource.RECORDS),
    WEATHER(SoundSource.WEATHER),
    BLOCKS(SoundSource.BLOCKS),
    HOSTILE(SoundSource.HOSTILE),
    NEUTRAL(SoundSource.NEUTRAL),
    PLAYERS(SoundSource.PLAYERS),
    AMBIENT(SoundSource.AMBIENT),
    VOICE(SoundSource.VOICE),
    ;

    final SoundSource category;

    SoundCategory(SoundSource category) {
        this.category = category;
    }
}
