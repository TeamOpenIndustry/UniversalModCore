package cam72cam.mod.event.platform;

import net.minecraft.resources.*;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.lifecycle.IModBusEvent;

/**
 * Fired when datapacks are reloaded, useful when injecting your dynamic datapack implementations.
 */
public class LoadDatapackEvent extends Event implements IModBusEvent {
    private final ResourcePackList infos;

    public LoadDatapackEvent(ResourcePackList infos) {
        this.infos = infos;
    }

    public void addDataPack(IResourcePack pack) {
        infos.addPackFinder((consumer, p_230230_2_) -> {
            consumer.accept(new ResourcePackInfo(pack.getName(),
                    true,
                    () -> pack,
                    new StringTextComponent(""),
                    new StringTextComponent(""),
                    PackCompatibility.COMPATIBLE,
                    ResourcePackInfo.Priority.TOP,
                    true,
                    IPackNameDecorator.BUILT_IN,
                    false));
        });
    }
}
