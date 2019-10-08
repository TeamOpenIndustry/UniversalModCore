package cam72cam.mod.event.mixin;

import cam72cam.mod.event.ClientEvents;
import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

@Mixin(SoundSystem.class)
public class SoundSystemMixin {

    @Inject(method = "reloadSounds", at=@At("RETURN"))
    public void reloadSounds() {
        ClientEvents.SOUND_LOAD.execute(Runnable::run);
    }
}
