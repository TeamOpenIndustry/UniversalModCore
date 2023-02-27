package cam72cam.mod.sound;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Player;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.resource.Identifier;
import io.netty.util.internal.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.*;
import net.minecraft.client.audio.ISound.AttenuationType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import paulscode.sound.CommandObject;
import paulscode.sound.SoundSystem;

import java.net.URL;
import java.util.function.Supplier;

class ClientSound extends PositionedSound implements ISound, ITickableSound {
    private final static float dopplerScale = 0.05f;
    private Vec3d position;
    private Vec3d velocity;
    private float currentPitch;

    private final float attenuationDistance;
    private final float scale;
    private float currentVolume;

    private SoundEventAccessor accessor;

    protected ClientSound(ResourceLocation soundId, SoundCategory categoryIn, boolean repeats, float attenuationDistance, float scale) {
        super(soundId, categoryIn.category);
        this.volume = 1;
        this.pitch = 1;
        this.repeat = repeats;

        this.attenuationDistance = attenuationDistance;
        this.scale = scale;

        this.sound = new Sound(getSoundLocation().toString(), 1, 1,  1, Sound.Type.FILE, false) {
            @Override
            public ResourceLocation getSoundAsOggLocation() {
                return soundId;
            }
        };
        this.accessor = new SoundEventAccessor(soundId, null); // TODO translation
        this.accessor.addSound(sound);
    }

    @Override
    public SoundEventAccessor createAccessor(SoundHandler handler) {
        return this.accessor;
    }

    @Override
    public void play(Vec3d pos) {
        setPosition(pos);
        if (Minecraft.getMinecraft().getSoundHandler().sndManager.playingSounds.containsValue(this)) {
            return;
        }

        float vol = this.volume;
        // Hack in attenuation distance
        this.volume = attenuationDistance / 16F;
        Minecraft.getMinecraft().getSoundHandler().playSound(this);
        this.volume = vol;
    }

    @Override
    public void stop() {
        Minecraft.getMinecraft().getSoundHandler().stopSound(this);
    }

    @Override
    @Deprecated
    public void update() {
    }

    @Override
    public void terminate() {
        stop();
    }

    @Override
    public void setPosition(Vec3d pos) {
        this.position = pos;
        this.xPosF = (float) pos.x;
        this.yPosF = (float) pos.y;
        this.zPosF = (float) pos.z;
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
    public boolean isPlaying() {
        return Minecraft.getMinecraft().getSoundHandler().isSoundPlaying(this);
    }

    @Override
    @Deprecated
    public void updateBaseSoundLevel(float baseSoundMultiplier) {
    }

    @Override
    @Deprecated
    public void reload() {
    }

    @Override
    @Deprecated
    public void disposable() {
    }

    @Override
    @Deprecated
    public boolean isDisposable() {
        return false;
    }

    @Override
    public boolean isDonePlaying() {
        float dampenLevel = 1;
        if (MinecraftClient.getPlayer().getRiding() != null) {
            dampenLevel = MinecraftClient.getPlayer().getRiding().getRidingSoundModifier();
        }

        this.volume = currentVolume * this.scale * dampenLevel;

        if (position == null || velocity == null) {
            pitch = currentPitch / scale;
        } else {
            //Doppler shift

            Player player = MinecraftClient.getPlayer();
            Vec3d ppos = player.getPosition();
            Vec3d nextPpos = ppos.add(player.getVelocity());

            Vec3d nextPos = this.position.add(velocity);

            double origDist = ppos.subtract(position).length();
            double newDist = nextPpos.subtract(nextPos).length();

            float appliedPitch = currentPitch;
            if (origDist > newDist) {
                appliedPitch *= 1 + (origDist - newDist) * dopplerScale;
            } else {
                appliedPitch *= 1 - (newDist - origDist) * dopplerScale;
            }
            pitch = appliedPitch / scale;
        }


        return false;
    }
}