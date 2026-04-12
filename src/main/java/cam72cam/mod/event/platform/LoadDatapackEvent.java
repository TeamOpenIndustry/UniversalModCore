package cam72cam.mod.event.platform;

import net.minecraft.resources.*;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.eventbus.api.Event;

import java.util.Map;

/**
 * Fired when datapacks are reloaded, useful when injecting your dynamic datapack implementations.
 */
public class LoadDatapackEvent extends Event {
    private final ResourcePackList<ResourcePackInfo> infos;

    public LoadDatapackEvent(ResourcePackList<ResourcePackInfo> infos) {
        this.infos = infos;
    }

    public void addDataPack(IResourcePack pack) {
        infos.addPackFinder(new IPackFinder() {
            @Override
            public <T extends ResourcePackInfo> void addPackInfosToMap(Map<String, T> nameToPackMap, ResourcePackInfo.IFactory<T> packInfoFactory) {
                //noinspection unchecked
                nameToPackMap.put(pack.getName(), (T) new ResourcePackInfo(pack.getName(),
                        true,
                        () -> pack,
                        new StringTextComponent(""),
                        new StringTextComponent(""),
                        PackCompatibility.COMPATIBLE,
                        ResourcePackInfo.Priority.TOP,
                        true,
                        true));
            }
        });
    }
}
