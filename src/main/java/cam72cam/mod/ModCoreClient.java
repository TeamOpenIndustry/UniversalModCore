package cam72cam.mod;

import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.resource.Identifier;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.resource.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static cam72cam.mod.ModCore.proxy;
import static cam72cam.mod.event.ClientEvents.MODEL_BAKE;
import static cam72cam.mod.event.ClientEvents.REGISTER_ENTITY;

public class ModCoreClient implements ClientModInitializer {
    public ModCoreClient() {
        proxy.enableClient();

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

        ModCore.mods.forEach(m -> m.clientEvent(ModEvent.CONSTRUCT));
    }

    @Override
    public void onInitializeClient() {
        //ModCore.instance.actualInit();
        ClientEvents.registerClientEvents();

        System.out.println("CRAPPPPP");

        REGISTER_ENTITY.execute(Runnable::run);
        MODEL_BAKE.execute(Runnable::run);
    }
}
