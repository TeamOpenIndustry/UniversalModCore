package cam72cam.mod.sound;

import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

public enum StandardSound {
    // Partial list only
    BLOCK_ANVIL_PLACE(SoundEvents.BLOCK_ANVIL_PLACE),
    BLOCK_FIRE_EXTINGUISH(SoundEvents.BLOCK_FIRE_EXTINGUISH);

    final SoundEvent event;

    StandardSound(SoundEvent event) {
        this.event = event;
    }
}
