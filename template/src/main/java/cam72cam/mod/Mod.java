package cam72cam.#MODID#;

import cam72cam.mod.ModCore;
import net.fabricmc.api.ModInitializer;

public class Mod implements ModInitializer {
    public static final String MODID = "#MODID#";
    public static final String NAME = "#MODNAME#";
    public static final String VERSION = "#MODVERSION#";

    static {
        try {
            Class<ModCore.Mod> cls = (Class<ModCore.Mod>) Class.forName("#MODCLASS#");
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

