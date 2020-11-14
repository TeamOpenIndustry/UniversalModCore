package cam72cam.mod;

import cam72cam.mod.resource.Data;
import cam72cam.mod.resource.Identifier;
import net.fabricmc.api.DedicatedServerModInitializer;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ModCoreServer implements DedicatedServerModInitializer {
    public ModCoreServer() {
        ModCore.proxy.enableServer();
        Identifier.registerSupplier(location -> {
            URL url = this.getClass().getResource(Data.pathString(location, true));
            if (url == null) {
                return Collections.emptyList();
            }
            List<InputStream> streams = new ArrayList<>();
            streams.add(this.getClass().getResourceAsStream(Data.pathString(location, true)));
            return streams;
        });
        ModCore.mods.forEach(m -> m.serverEvent(ModEvent.CONSTRUCT));
    }
    @Override
    public void onInitializeServer() {
        ModCore.instance.preInit();
        ModCore.instance.postInit();
    }
}
