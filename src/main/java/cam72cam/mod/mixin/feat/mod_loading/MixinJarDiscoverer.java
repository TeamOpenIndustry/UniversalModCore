package cam72cam.mod.mixin.feat.mod_loading;

import cam72cam.mod.loader.UMCModContainer;
import cam72cam.mod.loader.UMCModParser;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.discovery.JarDiscoverer;
import net.minecraftforge.fml.common.discovery.ModCandidate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

@Mixin(JarDiscoverer.class)
public class MixinJarDiscoverer {
    @Inject(method = "discover", at = @At("HEAD"), remap = false, cancellable = true)
    public void discover(ModCandidate candidate, ASMDataTable table, CallbackInfoReturnable<List<ModContainer>> cir) {
        try (JarFile file = new JarFile(candidate.getModContainer())) {
            ZipEntry entry = file.getEntry("umc.json");
            if (entry != null) {
                try (InputStream stream = file.getInputStream(entry)) {
                    UMCModContainer container = UMCModParser.parse(candidate, stream);
                    cir.setReturnValue(Collections.singletonList(container));
                } catch (Exception e) {
                    throw new RuntimeException("Failed to parse UMC mod", e);
                }
            }
        } catch (Exception e) {
            FMLLog.log.warn("Zip file {} failed to read properly, it will be ignored", candidate.getModContainer().getName(), e);
            cir.setReturnValue(Collections.emptyList());
        }
    }
}
