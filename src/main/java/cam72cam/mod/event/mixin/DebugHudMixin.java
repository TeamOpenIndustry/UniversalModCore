package cam72cam.mod.event.mixin;

import cam72cam.mod.event.ClientEvents;
import net.minecraft.client.gui.hud.DebugHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(DebugHud.class)
public class DebugHudMixin {
    @Inject(at = @At("RETURN"), method = "getRightText")
    protected void getLeftText(CallbackInfoReturnable<List<String>> info) {
        ClientEvents.RENDER_DEBUG.execute(x -> x.accept(info.getReturnValue()));
    }

    @Inject(at = @At("HEAD"), method = "renderHotbar")
    protected void renderHotbar(float float_1) {
        ClientEvents.RENDER_OVERLAY.execute(x -> x.accept(float_1));
    }
}
