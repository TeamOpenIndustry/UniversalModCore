package cam72cam.mod.sound;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.resource.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.util.SoundCategory;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.List;

/** We have our own sound manager that wraps the minecraft internal sound manager to fix some bugs/limitations */
class ModSoundManager {
    private final SoundManager manager;
    private List<ISound> sounds = new ArrayList<>();
    private float lastSoundLevel;
    private final SoundCategory category = SoundCategory.AMBIENT;

    ModSoundManager(SoundManager manager) {
        this.manager = manager;
        lastSoundLevel = Minecraft.getMinecraft().gameSettings.getSoundLevel(category);
    }

    private void initSoundSystem() {
    }

    ISound createSound(Identifier oggLocation, Audio.InputTransformer data, boolean repeats, float attenuationDistance, float scale) {
        ClientSound snd = new ClientSound(() -> manager.sndSystem, oggLocation, getURLForSoundResource(oggLocation, data), lastSoundLevel, repeats, attenuationDistance, scale);
        this.sounds.add(snd);

        return snd;
    }

    private URL getURLForSoundResource(Identifier internal, Audio.InputTransformer data) {
        // Duplicate MC's getUrlForSoundResource but inject our own resources as well

        String s = String.format("%s:%s:%s", "mcsounddomain", internal.getDomain(), internal.getPath());
        URLStreamHandler urlstreamhandler = new URLStreamHandler()
        {
            protected URLConnection openConnection(URL p_openConnection_1_)
            {
                return new URLConnection(p_openConnection_1_)
                {
                    public void connect() {
                    }
                    public InputStream getInputStream() throws IOException
                    {
                        return data.getStream(internal);
                    }
                };
            }
        };

        try
        {
            return new URL(null, s, urlstreamhandler);
        }
        catch (MalformedURLException var4)
        {
            throw new Error("TODO: Sanely handle url exception! :D");
        }
    }

    /** Called by modcore every tick to update all known sounds */
    void tick() {
        float dampenLevel = 1;
        if (MinecraftClient.getPlayer().getRiding() != null) {
            dampenLevel = MinecraftClient.getPlayer().getRiding().getRidingSoundModifier();
        }

        float newSoundLevel = Minecraft.getMinecraft().gameSettings.getSoundLevel(category) * dampenLevel;
        if (newSoundLevel != lastSoundLevel) {
            lastSoundLevel = newSoundLevel;
            for (ISound sound : this.sounds) {
                sound.updateBaseSoundLevel(lastSoundLevel);
            }
        }


        if (MinecraftClient.getPlayer().getTickCount() % 20 == 0) {
            // Clean up disposed sounds

            this.sounds.removeIf(sound -> sound.isDisposable() && !sound.isPlaying());
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
