package cam72cam.mod.sound;

import cam72cam.mod.math.Vec3d;

/** Interface representing a sound object.  We may implement a server side sound at some point in the future */
public interface ISound {
    /** Start playing the sound at this position */
    void play(Vec3d pos);

    /** Stop playing the sound */
    void stop();

    void setPosition(Vec3d pos);

    void setPitch(float f);

    /** Doppler shift */
    void setVelocity(Vec3d vel);

    void setVolume(float f);

    boolean isPlaying();
}
