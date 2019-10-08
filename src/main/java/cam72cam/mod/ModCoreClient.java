package cam72cam.mod;

import cam72cam.mod.resource.Identifier;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.resource.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ModCoreClient implements ClientModInitializer {
    public ModCoreClient() {
        ModCore.proxy.enableClient();
        ModCore.instance.mods.forEach(m -> m.clientEvent(ModEvent.CONSTRUCT));
    }

    @Override
    public void onInitializeClient() {
        Identifier.registerSupplier(identifier -> {
            List<InputStream> res = new ArrayList<>();
            try {
                for (Resource resource : net.minecraft.client.MinecraftClient.getInstance().getResourceManager().getAllResources(identifier.internal)) {
                    res.add(resource.getInputStream());
                }
            } catch (java.io.FileNotFoundException ex) {
                // Ignore
            } catch (IOException e) {
                ModCore.catching(e);
            }
            return res;
        });
    }
}
