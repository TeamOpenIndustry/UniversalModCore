package cam72cam.mod.event.mixin;

import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.util.Hand;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {
    //TODO @Inject(method = "onMouseButton", at=@At(value="INVOKE", target = "Lnet/minecraft/client/options/KeyBinding;setKeyPressed;V", ordinal = 0), cancellable = true)
    private void onMouseButton(long long_1, int int_1, int int_2, int int_3, CallbackInfo info) {
        InputUtil.KeyCode code = InputUtil.Type.MOUSE.createFromCode(int_1);
        // Yes, I know primary/secondary are flipped...
        if (MinecraftClient.getInstance().options.keyUse.matchesMouse(code.getKeyCode())) {
            if (!ClientEvents.CLICK.executeCancellable(x -> !x.apply(Hand.PRIMARY))) {
                info.cancel();
                return;
            }
        }
        if (MinecraftClient.getInstance().options.keyAttack.matchesMouse(code.getKeyCode())) {
            if(!ClientEvents.CLICK.executeCancellable(x -> !x.apply(Hand.SECONDARY))) {
                info.cancel();
                return;
            }
        }
    }
}
