package cam72cam.mod.sound;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

public enum StandardSound {
    // Partial list only
    BLOCK_ANVIL_PLACE(SoundEvents.ANVIL_LAND),
    BLOCK_FIRE_EXTINGUISH(SoundEvents.FIRE_EXTINGUISH);

    final SoundEvent event;

    StandardSound(SoundEvent event) {
        this.event = event;
    }
}
