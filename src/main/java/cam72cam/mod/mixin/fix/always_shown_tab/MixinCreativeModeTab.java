package cam72cam.mod.mixin.fix.always_shown_tab;

import cam72cam.mod.mixin.accessor.ACreativeModeTab;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CreativeModeTab.class)
public class MixinCreativeModeTab implements ACreativeModeTab {
    @Unique
    private boolean isUMCTab = false;

    @Unique
    @Override
    public void setUMCTab() {
        this.isUMCTab = true;
    }

    @Inject(method = "shouldDisplay", at = @At("HEAD"), cancellable = true)
    private void shouldDisplay(CallbackInfoReturnable<Boolean> cir) {
        if (isUMCTab) {
            cir.setReturnValue(true);
        }
    }
}
