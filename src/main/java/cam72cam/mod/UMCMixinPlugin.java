package cam72cam.mod;

//import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import cpw.mods.fml.relauncher.CoreModManager;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;

import javax.annotation.Nullable;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.7.10")
public class UMCMixinPlugin implements IFMLLoadingPlugin {
    public UMCMixinPlugin() {
        MixinBootstrap.init();
//        MixinExtrasBootstrap.init();
        Mixins.addConfiguration("mixins.feat.universalmodcore.json");
        Mixins.addConfiguration("mixins.fix.universalmodcore.json");

        CodeSource codeSource = this.getClass().getProtectionDomain().getCodeSource();
        if (codeSource != null) {
            URL location = codeSource.getLocation();
            try {
                File file = new File(location.toURI());
                if (file.isFile()) {
                    CoreModManager.getReparseableCoremods().add(file.getName());
                }
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {

    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
