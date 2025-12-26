package cam72cam.mod;

import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.IncompatibleEnvironmentException;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class UMCMixinPlugin implements ITransformationService {
    public UMCMixinPlugin() {
        MixinBootstrap.init();
        MixinExtrasBootstrap.init();
        Mixins.addConfiguration("mixins.universalmodcore.json");
    }

    @Nonnull
    @Override
    public String name() {
        return "UniversalModCoreMixinInitializer";
    }

    @Override
    public void initialize(IEnvironment iEnvironment) {

    }

    @Override
    public void beginScanning(IEnvironment iEnvironment) {

    }

    @Override
    public void onLoad(IEnvironment iEnvironment, Set<String> set) throws IncompatibleEnvironmentException {

    }

    @Nonnull
    @Override
    public List<ITransformer> transformers() {
        return Collections.emptyList();
    }
}
