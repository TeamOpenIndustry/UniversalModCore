package cam72cam.mod.mixin.feat.mod_loading;

import cam72cam.mod.ModCore;
import cam72cam.mod.loader.UMCModParser;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.discovery.DirectoryDiscoverer;
import net.minecraftforge.fml.common.discovery.ModCandidate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

@Mixin(DirectoryDiscoverer.class)
public class MixinDirectoryDiscoverer {
    @Inject(method = "discover", at = @At("HEAD"), remap = false, cancellable = true)
    public void discover(ModCandidate candidate, ASMDataTable table, CallbackInfoReturnable<List<ModContainer>> cir) {
        File classes = candidate.getModContainer();
        //Only handle this in dev environment, players should always use jar
        if (classes.getAbsolutePath().contains("build" + File.separator + "classes") && ModCore.isDevelopmentEnvironment()) {
            //Tricky handling!
            //I love LaunchWrapper
            File resources;
            try {
                //Cleanroom
                resources = (File) ModCandidate.class.getMethod("getResourcePathRoot").invoke(candidate);
            } catch (Exception e) {
                //Forge
                resources = new File(classes.getAbsolutePath().replace("classes"+File.separator+"java", "resources"));
            }
            File umcMod = new File(resources, "umc.json");
            if (resources.isDirectory() && umcMod.exists()) {
                try (InputStream stream = Files.newInputStream(umcMod.toPath())) {
                    cir.setReturnValue(Collections.singletonList(UMCModParser.parse(candidate, stream, resources)));
                } catch (Exception e) {
                    throw new RuntimeException("Failed to parse UMC mod", e);
                }
            }
        }
    }
}
