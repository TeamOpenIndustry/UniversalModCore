package cam72cam.mod;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import zone.rong.mixinbooter.IEarlyMixinLoader;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.12.2")
public class UMCMixinPlugin implements IFMLLoadingPlugin, IEarlyMixinLoader {
    public UMCMixinPlugin() {
    }

    @Override
    public List<String> getMixinConfigs() {
        return Arrays.asList("mixins.feat.universalmodcore.json", "mixins.fix.universalmodcore.json");
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
