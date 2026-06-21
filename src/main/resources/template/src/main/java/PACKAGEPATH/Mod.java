package #PACKAGE#;

import cam72cam.mod.ModCore;
import cam72cam.mod.ModEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@net.minecraftforge.fml.common.Mod(Mod.MODID)
public class Mod extends ModCore.Mod {
    public static final String MODID = "#ID#";

    static {
        try {
            ModCore.register(new Mod());
        } catch (Exception e) {
            throw new RuntimeException("Could not load mod " + MODID, e);
        }
    }

    @Override
    public String modID() {
        return MODID;
    }

    @Override
    public void commonEvent(ModEvent modEvent) {

    }

    @Override
    public void clientEvent(ModEvent modEvent) {

    }

    @Override
    public void serverEvent(ModEvent modEvent) {

    }
}
