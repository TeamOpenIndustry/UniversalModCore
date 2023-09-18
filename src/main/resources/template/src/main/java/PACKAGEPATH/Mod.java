package #PACKAGE#;

import cam72cam.mod.ModCore;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.data.event.GatherDataEvent;

@net.minecraftforge.fml.common.Mod(Mod.MODID)
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = Mod.MODID, bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
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
