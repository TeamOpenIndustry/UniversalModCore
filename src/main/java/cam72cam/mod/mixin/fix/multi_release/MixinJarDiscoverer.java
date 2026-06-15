package cam72cam.mod.mixin.fix.multi_release;

import com.llamalad7.mixinextras.sugar.Local;
import cpw.mods.fml.common.discovery.JarDiscoverer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.regex.Matcher;
import java.util.zip.ZipEntry;

@Mixin(JarDiscoverer.class)
public class MixinJarDiscoverer {
    //Skip Java9+ classes in Multi-Release as Forge can't process them
//    @WrapOperation(method = "discover", at = @At(value = "INVOKE", target = "Ljava/util/regex/Matcher;matches()Z"), remap = false, require = 0)
    //TODO Change back to @WrapOperation on 1.11/1.10 once MixinBooter is fixed
    @Redirect(method = "discover", at = @At(value = "INVOKE", target = "Ljava/util/regex/Matcher;matches()Z"), remap = false)
    private boolean checkClass(Matcher instance, @Local(name = "ze") ZipEntry ze) {
        if (instance.matches()) {
            return !ze.getName().endsWith("module-info.class")
                   && !ze.getName().startsWith("META-INF/versions/");
        }
        return false;
    }
}
