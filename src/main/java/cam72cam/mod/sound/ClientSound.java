package cam72cam.mod.sound;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Player;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.resource.Identifier;
import net.minecraft.client.sound.*;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.lwjgl.openal.AL10;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.nio.ByteBuffer;

class ClientSound implements ISound {
    private final static float dopplerScale = 0.05f;
    private final boolean repeats;
    private final Identifier oggLocation;
    private final float attenuationDistance;
    private Vec3d currentPos;
    private Vec3d velocity;
    private float currentPitch = 1;
    private float currentVolume = 1;
    private float baseSoundMultiplier;
    private final float scale;
    private boolean disposable = false;
    private int id;
    private int lastUsed = 0;
    private StaticSound sound;

    ClientSound(Identifier oggLocation, float baseSoundMultiplier, boolean repeats, float attenuationDistance, float scale) {
        this.baseSoundMultiplier = baseSoundMultiplier;
        this.repeats = repeats;
        this.oggLocation = oggLocation;
        this.attenuationDistance = attenuationDistance;
        this.scale = scale;

        id = -1;
    }

    private boolean checkErr() {
        int i = AL10.alGetError();
        boolean err = i != 0;
        if (err) {
            System.out.println("OpenAL Error " + i + " : " + ExceptionUtils.getStackTrace(new Throwable()));
        }
        return err;
    }

    private void init() {
        if (oggLocation == null) {
            return;
        }

        id = AL10.alGenSources();
        if (checkErr()) {
            id = -1;
            return;
        }
        try {
            OggAudioStream stream = new OggAudioStream(oggLocation.getResourceStream());
            AudioFormat fmt = stream.getFormat();
            int sizeBytes = (int) ((fmt.getSampleSizeInBits() * fmt.getChannels() * fmt.getSampleRate())/8);
            ByteBuffer buffer = stream.method_19720(sizeBytes);
            sound = new StaticSound(buffer, fmt);
            for (int i = 0; i< 4; i++) {
                sound.takeStreamBufferPointer().ifPresent(bufferId -> {
                    AL10.alSourceQueueBuffers(id, bufferId);
                    checkErr();
                });
            }
            stream.close();
        } catch (IOException e) {
            throw new RuntimeException("Sound not found: " + oggLocation);
        }

        if (repeats) {
            AL10.alSourcei(id, AL10.AL_LOOPING, 1);
            checkErr();
        }
    }

    @Override
    public void play(Vec3d pos) {
        stop();

        if (id == -1) {
            init();
        }

        this.setPosition(pos);
        update();

        if (repeats || currentPos == null || !MinecraftClient.isReady()) {
            AL10.alSourcePlay(id);
        } else if (MinecraftClient.getPlayer().getPosition().distanceTo(currentPos) < this.attenuationDistance * 1.1) {
            AL10.alSourcePlay(id);
        }
    }

    @Override
    public void stop() {
        if (id != -1) {
            AL10.alSourceStop(id);
        }
    }

    @Override
    public void terminate() {
        if (id == -1) {
            return;
        }

        // Force stop
        AL10.alSourceStop(id);
        checkErr();

        // Dealloc buffer (todo keep around?)
        sound.close();

        // Is this the same as the above call? net.minecraft.client.audio.SoundSource#func_216427_k
        int i = AL10.alGetSourcei(id, AL10.AL_BUFFERS_PROCESSED);
        if (i > 0) {
            int[] aint = new int[i];
            AL10.alSourceUnqueueBuffers(id, aint);
            checkErr();
            AL10.alDeleteBuffers(aint);
            checkErr();
        }

        // Actually delete the buffer
        AL10.alDeleteSources(id);

        checkErr();
        id = -1;
    }

    Vec3d lastPos;
    float lastPitch = -1;

    @Override
    public void update() {
        MinecraftClient.startProfiler("irSound");
        //(float)Math.sqrt(Math.sqrt(scale()))

        float vol = currentVolume * baseSoundMultiplier * scale;
        vol *= 1-Math.min(0.99, Math.max(0.01, currentPos.subtract(MinecraftClient.getPlayer().getPosition()).length() / attenuationDistance));
        AL10.alSourcef(this.id, AL10.AL_GAIN, vol);

        if (currentPos != null && !currentPos.equals(lastPos)) {
            AL10.alSourcefv(this.id, AL10.AL_POSITION, new float[]{(float)currentPos.x, (float)currentPos.y, (float)currentPos.z});
            lastPos = currentPos;
        }

        float newPitch = currentPitch / scale;
        if (currentPos == null || velocity == null) {
        } else {
            //Doppler shift

            Player player = MinecraftClient.getPlayer();
            Vec3d ppos = player.getPosition();
            Vec3d nextPpos = ppos.add(player.getVelocity());

            Vec3d nextPos = this.currentPos.add(velocity);

            double origDist = ppos.subtract(currentPos).length();
            double newDist = nextPpos.subtract(nextPos).length();

            float appliedPitch = currentPitch;
            if (origDist > newDist) {
                appliedPitch *= 1 + (origDist - newDist) * dopplerScale;
            } else {
                appliedPitch *= 1 - (newDist - origDist) * dopplerScale;
            }

            newPitch = appliedPitch / scale;
        }

        if (lastPitch != newPitch) {
            AL10.alSourcef(this.id, AL10.AL_PITCH, newPitch);
            lastPitch = newPitch;
        }


        MinecraftClient.endProfiler();
    }

    @Override
    public void setPosition(Vec3d pos) {
        this.currentPos = pos;
    }

    @Override
    public void setPitch(float f) {
        this.currentPitch = f;
    }

    @Override
    public void setVelocity(Vec3d vel) {
        this.velocity = vel;
    }

    @Override
    public void setVolume(float f) {
        this.currentVolume = f;
    }

    @Override
    public void updateBaseSoundLevel(float baseSoundMultiplier) {
        this.baseSoundMultiplier = baseSoundMultiplier;
    }

    @Override
    public boolean isPlaying() {
        return AL10.alGetSourcei(id, AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING;
    }

    @Override
    public void reload() {
        // Force re-create sound
        // TODO 1.14.4
    }

    @Override
    public void disposable() {
        disposable = true;
    }

    void tick() {
        lastUsed -= 1;

        if (isPlaying()) {
            lastUsed = 20;
        }

        if (lastUsed == 0) {
            terminate();
        }
    }

    @Override
    public boolean isDisposable() {
        return disposable;
    }
}
