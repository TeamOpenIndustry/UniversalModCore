package cam72cam.mod.mixin.fix.sodium_dev_Environment;

import net.neoforged.neoforge.client.config.NeoForgeClientConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(NeoForgeClientConfig.class)
public class MixinNeoForgeClientConfig {
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/neoforged/fml/loading/FMLLoader;isProduction()Z"))
    public boolean red() {
        // Set default config to true to make sodium runnable
        return true;
    }
}
