package cam72cam.mod.sound;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Player;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.resource.Identifier;
import io.netty.util.internal.ThreadLocalRandom;
import net.minecraft.client.audio.ISound.AttenuationType;
import net.minecraft.util.math.MathHelper;
import paulscode.sound.CommandObject;
import paulscode.sound.SoundSystem;

import java.net.URL;
import java.util.function.Supplier;

class ClientSound implements ISound {
    private final static float dopplerScale = 0.05f;
    private String id;
    private final Supplier<SoundSystem> sndSystem;
    private final URL resource;
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
    private int lastUsed = 0;

    ClientSound(Supplier<SoundSystem> soundSystem, Identifier oggLocation, URL resource, float baseSoundMultiplier, boolean repeats, float attenuationDistance, float scale) {
        this.sndSystem = soundSystem;
        this.resource = resource;
        this.baseSoundMultiplier = baseSoundMultiplier;
        this.repeats = repeats;
        this.oggLocation = oggLocation;
        this.attenuationDistance = attenuationDistance;
        this.scale = scale;
    }

    void init() {
        if (oggLocation == null) {
            return;
        }
        id = MathHelper.getRandomUUID(ThreadLocalRandom.current()).toString();
        sndSystem.get().newSource(false, id, resource, oggLocation.toString(), repeats, 0f, 0f, 0f, AttenuationType.LINEAR.getTypeInt(), attenuationDistance);
    }

    @Override
    public void play(Vec3d pos) {
        stop();

        if (id == null) {
            init();
        }

        this.setPosition(pos);
        update();

        if (repeats || currentPos == null || !MinecraftClient.isReady()) {
            sndSystem.get().play(id);
        } else if (MinecraftClient.getPlayer().getPosition().distanceTo(currentPos) < this.attenuationDistance * 1.1) {
            sndSystem.get().play(id);
        }
    }

    @Override
    public void stop() {
        if (isPlaying()) {
            sndSystem.get().stop(id);
        }
    }

    @Override
    public void terminate() {
        if (id == null) {
            return;
        }
        stop();
        sndSystem.get().removeSource(id);
        id = null;
    }

    @Override
    public void update() {
        if (id == null) {
            init();
        }

        MinecraftClient.startProfiler("irSound");
        SoundSystem snd = sndSystem.get();
        //(float)Math.sqrt(Math.sqrt(scale()))

        float vol = currentVolume * baseSoundMultiplier * scale;
        snd.CommandQueue(new CommandObject(CommandObject.SET_VOLUME, id, vol));

        if (currentPos != null) {
            snd.CommandQueue(new CommandObject(CommandObject.SET_POSITION, id, (float) currentPos.x, (float) currentPos.y, (float) currentPos.z));
        }

        if (currentPos == null || velocity == null) {
            snd.CommandQueue(new CommandObject(CommandObject.SET_PITCH, id, currentPitch / scale));
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

            sndSystem.get().setPitch(id, appliedPitch / scale);
            snd.CommandQueue(new CommandObject(CommandObject.SET_PITCH, id, appliedPitch / scale));
        }

        MinecraftClient.endProfiler();

        snd.interruptCommandThread();
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
        if (id == null) {
            return false;
        }

        return sndSystem.get().playing(id);
    }

    @Override
    public void reload() {
        // Force re-create sound
        id = null;
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
