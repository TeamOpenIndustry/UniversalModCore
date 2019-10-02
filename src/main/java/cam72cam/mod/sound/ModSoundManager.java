package cam72cam.mod.sound;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.resource.Identifier;
import net.minecraft.client.sound.SoundSystem;

import java.util.ArrayList;
import java.util.List;

public class ModSoundManager {
    private List<ISound> sounds = new ArrayList<>();
    private float lastSoundLevel;
    private SoundCategory category = SoundCategory.AMBIENT;
    private SoundSystem cachedSnd;

    public ModSoundManager() {
        lastSoundLevel = net.minecraft.client.MinecraftClient.getInstance().options.getSoundVolume(category.category);
    }

    public ISound createSound(Identifier oggLocation, boolean repeats, float attenuationDistance, float scale) {
        ClientSound snd = new ClientSound(oggLocation, lastSoundLevel, repeats, attenuationDistance, scale);
        this.sounds.add(snd);
        return snd;
    }

    public void tick() {
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

            List<ISound> toRemove = new ArrayList<ISound>();
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

    public boolean hasSounds() {
        return this.sounds.size() != 0;
    }

    public void stop() {
        for (ISound sound : this.sounds) {
            sound.stop();
            sound.terminate();
        }
        this.sounds = new ArrayList<ISound>();
    }

    public void handleReload(boolean soft) {
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
