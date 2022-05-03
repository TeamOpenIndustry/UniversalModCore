package cam72cam.mod.sound;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.resource.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.util.SoundCategory;

import java.util.ArrayList;
import java.util.List;

/** We have our own sound manager that wraps the minecraft internal sound manager to fix some bugs/limitations */
public class ModSoundManager {
    private List<ISound> sounds = new ArrayList<ISound>();
    private float lastSoundLevel;
    private SoundCategory category = SoundCategory.AMBIENT;

    public ModSoundManager() {
        lastSoundLevel = Minecraft.getInstance().gameSettings.getSoundLevel(category);
    }

    public ISound createSound(Identifier oggLocation, Audio.InputTransformer data, boolean repeats, float attenuationDistance, float scale) {
        ClientSound snd = new ClientSound(oggLocation, data, lastSoundLevel, repeats, attenuationDistance, scale);
        this.sounds.add(snd);
        return snd;
    }

    /** Called by modcore every tick to update all known sounds */
    public void tick() {
        float dampenLevel = 1;
        if (MinecraftClient.getPlayer().getRiding() != null) {
            dampenLevel = MinecraftClient.getPlayer().getRiding().getRidingSoundModifier();
        }

        float newSoundLevel = Minecraft.getInstance().gameSettings.getSoundLevel(category) * dampenLevel;
        if (newSoundLevel != lastSoundLevel) {
            lastSoundLevel = newSoundLevel;
            for (ISound sound : this.sounds) {
                sound.updateBaseSoundLevel(lastSoundLevel);
            }
        }

        if (MinecraftClient.getPlayer().getTickCount() % 20 == 0) {
            // Clean up disposed sounds

            for (ISound sound : new ArrayList<>(this.sounds)) {
                if (sound.isDisposable() && !sound.isPlaying()) {
                    sounds.remove(sound);
                    sound.stop();
                    sound.terminate();
                }
            }
        }

        for (ISound sound : this.sounds) {
            if (sound instanceof ClientSound) {
                ((ClientSound) sound).tick();
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
        for (ISound sound : this.sounds) {
            sound.reload();
        }
    }
}
