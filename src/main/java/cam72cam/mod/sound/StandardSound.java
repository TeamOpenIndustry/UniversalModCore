package cam72cam.mod.sound;

import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;

public enum StandardSound {
    // Partial list only
    BLOCK_ANVIL_PLACE(SoundEvents.ANVIL_LAND),
    BLOCK_FIRE_EXTINGUISH(SoundEvents.FIRE_EXTINGUISH);

    final SoundEvent event;

    StandardSound(SoundEvent event) {
        this.event = event;
    }
}
