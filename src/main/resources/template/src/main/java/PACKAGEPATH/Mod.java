package #PACKAGE#;

import cam72cam.mod.ModCore;
import net.fabricmc.api.ModInitializer;

public class Mod implements ModInitializer {
    public static final String MODID = "#ID#";
    public static final String NAME = "#NAME#";
    public static final String VERSION = "#VERSION#";

    static {
        ModCore.register(new #PACKAGE#.#CLASS#());
    }

    @Override
    public void onInitialize() {
    }
}
