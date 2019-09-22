package cam72cam.mod.sound;

public enum SoundCategory {
    MASTER(net.minecraft.client.audio.SoundCategory.MASTER),
    MUSIC(net.minecraft.client.audio.SoundCategory.MUSIC),
    RECORDS(net.minecraft.client.audio.SoundCategory.RECORDS),
    WEATHER(net.minecraft.client.audio.SoundCategory.WEATHER),
    BLOCKS(net.minecraft.client.audio.SoundCategory.BLOCKS),
    HOSTILE(net.minecraft.client.audio.SoundCategory.PLAYERS),
    NEUTRAL(net.minecraft.client.audio.SoundCategory.PLAYERS),
    PLAYERS(net.minecraft.client.audio.SoundCategory.PLAYERS),
    AMBIENT(net.minecraft.client.audio.SoundCategory.AMBIENT),
    VOICE(net.minecraft.client.audio.SoundCategory.PLAYERS),
    ;

    final net.minecraft.client.audio.SoundCategory internal;

    SoundCategory(net.minecraft.client.audio.SoundCategory category) {
        internal = category;
    }
}
