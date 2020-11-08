package #PACKAGE#;

import cam72cam.mod.ModCore;
import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.Recipes;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent;

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
    public static void registerRecipes(GatherDataEvent event) {
        CommonEvents.Recipe.REGISTER.execute(Runnable::run);
        event.getGenerator().addProvider(new Recipes(event.getGenerator()));
        Fuzzy.register(gen);
    }
}
