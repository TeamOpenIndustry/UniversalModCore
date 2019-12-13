package cam72cam.mod;

import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.resource.Identifier;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ResourceType;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static cam72cam.mod.ModCore.proxy;
import static cam72cam.mod.event.ClientEvents.MODEL_BAKE;
import static cam72cam.mod.event.ClientEvents.REGISTER_ENTITY;

public class ModCoreClient implements ClientModInitializer {
    static List<ResourcePack> list_1;
    public ModCoreClient() {
        proxy.enableClient();

        Identifier.registerSupplier(identifier -> {
            if (list_1 == null) {
                list_1 = MinecraftClient.getInstance().getResourcePackManager().getEnabledProfiles().stream().map(ResourcePackProfile::createResourcePack).collect(Collectors.toList());
            }


            List<InputStream> res = new ArrayList<>();
            list_1.forEach(x -> {
                try {
                    res.add(x.open(ResourceType.CLIENT_RESOURCES, identifier.internal));
                } catch (IOException ignored) {
                }
            });
            return res;
        });

        ModCore.instance.mods.forEach(m -> m.clientEvent(ModEvent.CONSTRUCT));
    }

    @Override
    public void onInitializeClient() {
        //ModCore.instance.actualInit();
        ClientEvents.registerClientEvents();

        REGISTER_ENTITY.execute(Runnable::run);
        MODEL_BAKE.execute(Runnable::run);
    }
}
