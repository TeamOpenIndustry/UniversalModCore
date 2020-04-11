package #PACKAGE#;

import cam72cam.mod.ModCore;
import net.fabricmc.api.ModInitializer;

public class Mod implements ModInitializer {
    public static final String MODID = "#ID#";
    public static final String NAME = "#NAME#";
    public static final String VERSION = "#VERSION#";

    static {
        try {
            Class<ModCore.Mod> cls = (Class<ModCore.Mod>) Class.forName("#PACKAGE#.#CLASS#");
            ModCore.register(() -> {
                try {
                    return cls.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new RuntimeException("Could not construct mod " + MODID, e);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Could not load mod " + MODID, e);
        }
    }

    @Override
    public void onInitialize() {

    }
}

