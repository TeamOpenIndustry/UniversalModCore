package cam72cam.mod.sound;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Player;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.world.World;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public class Audio {
    @Environment(EnvType.CLIENT)
    private static ModSoundManager soundManager;

    /** Used to wire up event handlers, do not use directly */
    @Environment(EnvType.CLIENT)
    public static void registerClientCallbacks() {
        ClientEvents.TICK.subscribe(() -> {
            if (soundManager == null) {
                soundManager = new ModSoundManager();
            }

            World world = null;
            // This can fire while in the main menu, we need to be careful about that
            if (MinecraftClient.isReady()) {
                world = MinecraftClient.getPlayer().getWorld();
                soundManager.tick();
            }

            if (world == null && soundManager != null && soundManager.hasSounds()) {
                soundManager.stop();
            }
        });

        ClientEvents.SOUND_LOAD.subscribe(() -> {
            if (soundManager == null) {
                soundManager = new ModSoundManager();
            } else {
                soundManager.handleReload(false);
            }
        });

        CommonEvents.World.LOAD.subscribe(world -> soundManager.handleReload(true));

        CommonEvents.World.UNLOAD.subscribe(world -> soundManager.stop());
    }

    /** Play a built-in sound (Client side only) */
    public static void playSound(World world, Vec3d pos, StandardSound sound, SoundCategory category, float volume, float pitch) {
        world.internal.playSound(pos.x, pos.y, pos.z, sound.event, category.category, volume, pitch, false);
    }

    /** Play a built-in sound (Client side only) */
    public static void playSound(World world, Vec3i pos, StandardSound sound, SoundCategory category, float volume, float pitch) {
        playSound(world, new Vec3d(pos), sound, category, volume, pitch);
    }

    /** Create a custom sound */
    public static ISound newSound(Identifier oggLocation, boolean repeats, float attenuationDistance, float scale) {
        return soundManager.createSound(oggLocation, repeats, attenuationDistance, scale);
    }

    /** Hack to increase the number of sounds that can be played at a time */
    public static void setSoundChannels(int max) {
        //SoundSystemConfig.setNumberNormalChannels(Math.max(SoundSystemConfig.getNumberNormalChannels(), max));
    }
}
