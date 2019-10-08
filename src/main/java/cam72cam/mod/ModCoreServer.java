package cam72cam.mod;

import net.fabricmc.api.DedicatedServerModInitializer;

public class ModCoreServer implements DedicatedServerModInitializer {
    public ModCoreServer() {
        ModCore.proxy.enableServer();
        ModCore.instance.mods.forEach(m -> m.serverEvent(ModEvent.CONSTRUCT));
    }
    @Override
    public void onInitializeServer() {

    }
}
