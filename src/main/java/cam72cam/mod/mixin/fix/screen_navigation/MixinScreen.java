package cam72cam.mod.mixin.fix.screen_navigation;

import cam72cam.mod.gui.screen.ScreenBuilder;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Bypass vanilla's arrow/tab button handling, make it aligned with 1.12.2
 */
@Mixin(Screen.class)
public class MixinScreen {
    @Inject(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/events/AbstractContainerEventHandler;nextFocusPath(Lnet/minecraft/client/gui/navigation/FocusNavigationEvent;)Lnet/minecraft/client/gui/ComponentPath;", ordinal = 0), cancellable = true)
    public void check(int p_96552_, int p_96553_, int p_96554_, CallbackInfoReturnable<Boolean> cir) {
        //If the event is created, cancel it
        //We don't want widget focusing controlled by keys, especially when using textfield
        Screen self = (Screen) (Object) this;
        if (self instanceof ScreenBuilder) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
}
