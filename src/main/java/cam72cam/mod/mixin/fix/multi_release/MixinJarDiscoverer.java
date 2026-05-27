package cam72cam.mod.mixin.fix.multi_release;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraftforge.fml.common.discovery.JarDiscoverer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.regex.Matcher;
import java.util.zip.ZipEntry;

@Mixin(JarDiscoverer.class)
public class MixinJarDiscoverer {
    //Skip Java9+ classes in Multi-Release as Forge can't process them
    @WrapOperation(method = "findClassesASM", at = @At(value = "INVOKE", target = "Ljava/util/regex/Matcher;matches()Z"), remap = false, require = 0)
    private boolean checkClass(Matcher instance, Operation<Boolean> original, @Local(name = "ze") ZipEntry ze) {
        if (original.call(instance)) {
            return !ze.getName().endsWith("module-info.class")
                   && !ze.getName().startsWith("META-INF/versions/");
        }
        return false;
    }
}
