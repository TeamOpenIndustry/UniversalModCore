package #PACKAGE#;

import cam72cam.mod.ModCore;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@net.neoforged.fml.common.Mod(Mod.MODID)
@net.neoforged.fml.common.EventBusSubscriber(modid = Mod.MODID)
public class Mod {
    public static final String MODID = "#ID#";

    static {
        try {
            ModCore.register(new #PACKAGE#.#CLASS#());
        } catch (Exception e) {
            throw new RuntimeException("Could not load mod " + MODID, e);
        }
    }

    @SubscribeEvent
    public static void genData(GatherDataEvent event) {
        ModCore.genData(MODID, event);
    }
}
