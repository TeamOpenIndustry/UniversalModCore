package cam72cam.mod.sound;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.world.World;
import paulscode.sound.SoundSystemConfig;

public class Audio {
    /** Play a built-in sound (Client side only) */
    public static void playSound(World world, Vec3d pos, StandardSound sound, SoundCategory category, float volume, float pitch) {
        world.internal.playSound(pos.x, pos.y, pos.z, sound.event, category.category, volume, pitch, false);
    }

    /** Play a built-in sound (Client side only) */
    public static void playSound(World world, Vec3i pos, StandardSound sound, SoundCategory category, float volume, float pitch) {
        playSound(world, new Vec3d(pos), sound, category, volume, pitch);
    }

    /** Create a custom sound */
    public static ISound newSound(Identifier oggLocation, SoundCategory category, boolean repeats, float attenuationDistance, float scale) {
        return new ClientSound(oggLocation.internal, category, repeats, attenuationDistance, scale);
    }

    /** Hack to increase the number of sounds that can be played at a time */
    public static void setSoundChannels(int max) {
        SoundSystemConfig.setNumberNormalChannels(Math.max(SoundSystemConfig.getNumberNormalChannels(), max));
    }
}
