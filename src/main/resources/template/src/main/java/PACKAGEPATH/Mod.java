package #PACKAGE#;

import cam72cam.mod.ModCore;

@cpw.mods.fml.common.Mod(modid = Mod.MODID, name = "#NAME#", version = "#VERSION#", dependencies = "required-after:universalmodcore@[#UMC_API#,#UMC_API_NEXT#)", acceptedMinecraftVersions = "[1.7.10,1.10)")
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
