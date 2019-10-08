package cam72cam.mod.event.mixin;

import cam72cam.mod.event.ClientEvents;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(at = @At("HEAD"), method = "renderHotbar")
    protected void renderHotbar(float float_1, CallbackInfo info) {
        ClientEvents.RENDER_OVERLAY.execute(x -> x.accept(float_1));
    }
}
