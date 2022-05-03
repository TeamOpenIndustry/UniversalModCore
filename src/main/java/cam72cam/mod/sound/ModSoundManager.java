package cam72cam.mod.sound;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.ModCore;
import cam72cam.mod.resource.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.lang3.tuple.Pair;
import paulscode.sound.SoundSystem;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** We have our own sound manager that wraps the minecraft internal sound manager to fix some bugs/limitations */
class ModSoundManager {
    private final SoundManager manager;
    private Supplier<SoundSystem> soundSystem;
    private List<ISound> sounds = new ArrayList<>();
    private float lastSoundLevel;
    private final SoundCategory category = SoundCategory.AMBIENT;
    private SoundSystem cachedSnd;

    ModSoundManager(SoundManager manager) {
        this.manager = manager;

        initSoundSystem();

        lastSoundLevel = Minecraft.getMinecraft().gameSettings.getSoundLevel(category.internal());
    }

    private void initSoundSystem() {
        for (String fname : new String[]{"field_148620_e", "sndSystem"}) {
            try {
                Field sndSystemField = SoundManager.class.getDeclaredField(fname);
                sndSystemField.setAccessible(true);
                this.soundSystem = () -> {
                    try {
                        if (cachedSnd == null) {
                            cachedSnd = (paulscode.sound.SoundSystem) sndSystemField.get(manager);
                        }
                        return cachedSnd;
                    } catch (Exception e) {
                        ModCore.catching(e);
                        return null;
                    }
                };
                return;
            } catch (Exception e) {
                ModCore.catching(e);
            }
        }
    }

    ISound createSound(Identifier oggLocation, Audio.InputTransformer data, boolean repeats, float attenuationDistance, float scale) {
        SoundSystem sndSystem = this.soundSystem.get();
        if (sndSystem == null) {
            return null;
        }

        ClientSound snd = new ClientSound(this.soundSystem, oggLocation, getURLForSoundResource(oggLocation, data), lastSoundLevel, repeats, attenuationDistance, scale);
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

        float newSoundLevel = Minecraft.getMinecraft().gameSettings.getSoundLevel(category.internal()) * dampenLevel;
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
        this.cachedSnd = null;
        for (ISound sound : this.sounds) {
            sound.reload();
        }
    }
}
