package cam72cam.mod.sound;

public enum SoundCategory {
    MASTER(net.minecraft.sound.SoundCategory.MASTER),
    MUSIC(net.minecraft.sound.SoundCategory.MUSIC),
    RECORDS(net.minecraft.sound.SoundCategory.RECORDS),
    WEATHER(net.minecraft.sound.SoundCategory.WEATHER),
    BLOCKS(net.minecraft.sound.SoundCategory.BLOCKS),
    HOSTILE(net.minecraft.sound.SoundCategory.HOSTILE),
    NEUTRAL(net.minecraft.sound.SoundCategory.NEUTRAL),
    PLAYERS(net.minecraft.sound.SoundCategory.PLAYERS),
    AMBIENT(net.minecraft.sound.SoundCategory.AMBIENT),
    VOICE(net.minecraft.sound.SoundCategory.VOICE),
    ;

    final net.minecraft.sound.SoundCategory category;

    SoundCategory(net.minecraft.sound.SoundCategory category) {
        this.category = category;
    }
}
