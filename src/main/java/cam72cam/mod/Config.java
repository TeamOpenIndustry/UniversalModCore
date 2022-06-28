package cam72cam.mod;

import cam72cam.mod.config.ConfigFile;

@ConfigFile.Comment("Configuration File")
@ConfigFile.Name("general")
@ConfigFile.File("universalmodcore.cfg")
public class Config {
    @ConfigFile.Comment("Size of each sprite in the texture sheet")
    public static int SpriteSize = 128;

    @ConfigFile.Comment("Max size of a generated texture sheet (-1 == autodetect, 4096 is a sane option)")
    public static int MaxTextureSize = -1;

    public static int getMaxTextureSize() {
        if (MaxTextureSize < 128 && ModCore.instance != null /*for tests*/) {
            MaxTextureSize = ModCore.instance.getGPUTextureSize();
        }
        return MaxTextureSize;
    }

    @ConfigFile.Comment("Enable Debug Logging")
    public static boolean DebugLogging = false;

    @ConfigFile.Comment("Write texture sheets to PNGs in cache directory")
    public static boolean DebugTextureSheets = false;

    @ConfigFile.Comment("Enable threaded texture loading")
    public static boolean ThreadedTextureLoading = true;
}
