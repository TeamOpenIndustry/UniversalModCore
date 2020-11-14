package cam72cam.mod.sound;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.resource.Identifier;
import net.minecraft.client.sound.SoundSystem;

import java.util.ArrayList;
import java.util.List;

/** We have our own sound manager that wraps the minecraft internal sound manager to fix some bugs/limitations */
class ModSoundManager {
    private List<ISound> sounds = new ArrayList<>();
    private float lastSoundLevel;
    private final SoundCategory category = SoundCategory.AMBIENT;
    private SoundSystem cachedSnd;

    ModSoundManager() {
        lastSoundLevel = net.minecraft.client.MinecraftClient.getInstance().options.getSoundVolume(category.category);
    }

    public ISound createSound(Identifier oggLocation, boolean repeats, float attenuationDistance, float scale) {
        ClientSound snd = new ClientSound(oggLocation, lastSoundLevel, repeats, attenuationDistance, scale);
        this.sounds.add(snd);
        return snd;
    }

    /** Called by modcore every tick to update all known sounds */
    void tick() {
        float dampenLevel = 1;
        if (MinecraftClient.getPlayer().getRiding() != null) {
            dampenLevel = MinecraftClient.getPlayer().getRiding().getRidingSoundModifier();
        }

        float newSoundLevel = net.minecraft.client.MinecraftClient.getInstance().options.getSoundVolume(category.category) * dampenLevel;
        if (newSoundLevel != lastSoundLevel) {
            lastSoundLevel = newSoundLevel;
            for (ISound sound : this.sounds) {
                sound.updateBaseSoundLevel(lastSoundLevel);
            }
        }


        if (MinecraftClient.getPlayer().getTickCount() % 20 == 0) {
            // Clean up disposed sounds

            List<ISound> toRemove = new ArrayList<>();
            for (ISound sound : this.sounds) {
                if (sound.isDisposable() && !sound.isPlaying()) {
                    toRemove.add(sound);
                }
            }

            for (ISound sound : toRemove) {
                sounds.remove(sound);
                sound.stop();
                sound.terminate();
            }
        }
    }

    boolean hasSounds() {
        return this.sounds.size() != 0;
    }

    void stop() {
        for (ISound sound : this.sounds) {
            sound.stop();
            sound.terminate();
        }
        this.sounds = new ArrayList<>();
    }

    void handleReload(boolean soft) {
        if (soft) {
            for (ISound sound : this.sounds) {
                sound.stop();
            }
        }
        this.cachedSnd = null;
        for (ISound sound : this.sounds) {
            sound.reload();
        }
    }
}
