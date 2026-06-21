package #PACKAGE#;

import cam72cam.mod.ModCore;

@net.minecraftforge.fml.common.Mod(Mod.MODID)
public class Mod {
    public static final String MODID = "#ID#";

    static {
        try {
            ModCore.register(new #PACKAGE#.#CLASS#());
        } catch (Exception e) {
            throw new RuntimeException("Could not load mod " + MODID, e);
        }
    }
}
