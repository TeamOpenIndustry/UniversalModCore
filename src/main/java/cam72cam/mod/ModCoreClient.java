package cam72cam.mod;

import net.fabricmc.api.ClientModInitializer;

public class ModCoreClient implements ClientModInitializer {
    public ModCoreClient() {
        ModCore.proxy.enableClient();
        ModCore.instance.mods.forEach(m -> m.clientEvent(ModEvent.CONSTRUCT));
    }

    @Override
    public void onInitializeClient() {
    }
}
