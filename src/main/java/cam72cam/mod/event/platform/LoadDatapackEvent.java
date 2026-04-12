package cam72cam.mod.event.platform;

import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;

/**
 * Fired when datapacks are reloaded, useful when injecting your dynamic datapack implementations.
 */
public class LoadDatapackEvent extends Event implements IModBusEvent {
    private final PackRepository infos;

    public LoadDatapackEvent(PackRepository infos) {
        this.infos = infos;
    }

    public void addDataPack(PackResources pack) {
        infos.addPackFinder((consumer, p_230230_2_) -> {
            consumer.accept(new Pack(pack.getName(),
                                     true,
                                     () -> pack,
                                     new TextComponent(""),
                                     new TextComponent(""),
                                     PackCompatibility.COMPATIBLE,
                                     Pack.Position.TOP,
                                     true,
                                     PackSource.DEFAULT,
                                     true));
        });
    }
}
