package cam72cam.mod.mixin.fix.multi_release;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraftforge.fml.loading.moddiscovery.AbstractJarFileLocator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Mixin(AbstractJarFileLocator.class)
public class MixinJarDiscoverer {
    //Skip Java9+ classes in Multi-Release as Forge can't process them
    @Inject(method = "lambda$scanFile$1", at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;forEach(Ljava/util/function/Consumer;)V"), remap = false, require = 0)
    private static void checkClass(Consumer<Path> pathConsumer, Path path, CallbackInfo ci, @Local(name = "files") Stream<Path> files) {
        files.filter(p -> !p.toString().endsWith("module-info.class") && !p.toString().startsWith("META-INF/versions/"));
    }
}
