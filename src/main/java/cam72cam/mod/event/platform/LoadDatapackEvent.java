package cam72cam.mod.event.platform;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.flag.FeatureFlagSet;
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
        infos.addPackFinder((consumer) -> {
            consumer.accept(Pack.create(pack.packId(),
                                        Component.literal(""),
                                        true,
                                        s -> pack,
                                        new Pack.Info(Component.literal(""), 13, FeatureFlagSet.of()),
                                        PackType.SERVER_DATA,
                                        Pack.Position.TOP,
                                        true,
                                        PackSource.DEFAULT
            ));
        });
    }
}
