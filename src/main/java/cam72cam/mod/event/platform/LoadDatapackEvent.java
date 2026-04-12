package cam72cam.mod.event.platform;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.flag.FeatureFlagSet;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

import java.util.List;
import java.util.Optional;

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
            //TODO 1.21.1
            PackLocationInfo info = new PackLocationInfo(pack.packId(),
                                                         Component.literal(""),
                                                         PackSource.BUILT_IN,
                                                         Optional.empty());
            consumer.accept(new Pack(
                    info,
                    new Pack.ResourcesSupplier() {
                        @Override
                        public PackResources openPrimary(PackLocationInfo p_326301_) {
                            return pack;
                        }

                        @Override
                        public PackResources openFull(PackLocationInfo p_326241_, Pack.Metadata p_325959_) {
                            return pack;
                        }
                    },
                    new Pack.Metadata(Component.literal(""), PackCompatibility.COMPATIBLE, FeatureFlagSet.of(), List.of(), false),
                    new PackSelectionConfig(true, Pack.Position.TOP, true)
            ));
        });
    }
}
