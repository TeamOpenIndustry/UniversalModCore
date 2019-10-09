package cam72cam.mod.event.mixin;

import cam72cam.mod.text.CustomTranslations;
import net.minecraft.util.Language;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Language.class)
public class LanguageMixin {
    @Inject(method = "getTranslation", at=@At("RETURN"), cancellable = true)
    public void getTranslation(String string_1, CallbackInfoReturnable info) {
        if (info.getReturnValue() == string_1) {
            if (CustomTranslations.getTranslations().containsKey(string_1)) {
                info.setReturnValue(CustomTranslations.getTranslations().get(string_1));
            }
        }
    }

    @Inject(method = "hasTranslation", at=@At("RETURN"), cancellable = true)
    public void hasTranslation(String string_1, CallbackInfoReturnable info) {
        if (!info.getReturnValueZ()) {
            if (CustomTranslations.getTranslations().containsKey(string_1)) {
                info.setReturnValue(true);
            }
        }
    }
}
