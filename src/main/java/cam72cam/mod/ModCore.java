package cam72cam.mod;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import net.minecraft.resources.*;
import net.minecraftforge.fml.loading.FMLLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cam72cam.mod.config.ConfigFile;
import cam72cam.mod.entity.ModdedEntity;
import cam72cam.mod.entity.sync.EntitySync;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.input.Mouse;
import cam72cam.mod.net.Packet;
import cam72cam.mod.net.PacketDirection;
import cam72cam.mod.render.Light;
import cam72cam.mod.resource.BuiltinPack;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.text.Command;
import cam72cam.mod.util.MinecraftFiles;
import cam72cam.mod.util.ModCoreCommand;
import cam72cam.mod.world.ChunkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Unit;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** UMC Mod, do not touch... */
@net.minecraftforge.fml.common.Mod(ModCore.MODID)
public class ModCore {
    public static final String MODID = "universalmodcore";
    public static final String NAME = "UniversalModCore";
    public static final String VERSION = "1.2.3";
    public static ModCore instance;
    public static boolean hasResources;
    private static boolean isInReload;

    private static List<Mod> mods = new ArrayList<>();
    private Logger logger;

    /** Register a mod, must happen before UMC is loaded! */
    public static void register(Mod ctr) {
        mods.add(ctr);
    }

    /** Called during Mod Construction phase */
    public ModCore() {
        System.out.println("Welcome to UniversalModCore!");
        instance = this;

        ModCore.register(new Internal());

        proxy.setup();
        proxy.event(ModEvent.CONSTRUCT);

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::preInit);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::init);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::postInit);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::serverStarting);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::serverStarted);

        MinecraftForge.EVENT_BUS.register(this);
    }

    /** INIT Phase (Forge) */
    public void preInit(FMLCommonSetupEvent event) {
        logger = LogManager.getLogger();
        proxy.event(ModEvent.INITIALIZE);
        hasResources = true;
    }

    private boolean hasSetup = false;

    /** SETUP Phase (Forge) */
    public void init(InterModEnqueueEvent event) {
        if(!hasSetup) {
            hasSetup = true;
            proxy.event(ModEvent.SETUP);
        }
    }

    /** FINALIZE Phase (Forge) */
    public void postInit(FMLLoadCompleteEvent event) {
        proxy.event(ModEvent.FINALIZE);
        for (Mod mod : mods) {
            File modDir = cacheFile(new Identifier(mod.modID(), "foo")).getParentFile();
            if (modDir.exists() && modDir.isDirectory()) {
                for (File file : modDir.listFiles()) {
                    if (!usedCacheFiles.contains(file)) {
                        ModCore.warn("Removing file cache entry: %s", file);
                        FileUtils.deleteQuietly(file);
                    }
                }
            }
        }
    }

	@SubscribeEvent
    public void serverStarting(FMLServerStartingEvent event) {
        Command.registration(event.getCommandDispatcher());
    }

    /** START Phase (Forge) */
    public void serverStarted(FMLServerStartedEvent event) {
        proxy.event(ModEvent.START);
    }

    /** Implement this to create a UMC mod */
    public static abstract class Mod {
        public abstract String modID();

        /** Called both server and client side with a given event */
        public abstract void commonEvent(ModEvent event);
        /** Called client side with a given event */
        public abstract void clientEvent(ModEvent event);
        /** Called server side with a given event */
        public abstract void serverEvent(ModEvent event);

        /** Get config file for filename */
        public final Path getConfig(String fname) {
            return Paths.get(FMLPaths.CONFIGDIR.get().toString(), fname);
        }

        /* Standard logging functions */

        public static void debug(String msg, Object...params) {
            ModCore.debug(msg, params);
        }
        public static void info(String msg, Object...params) {
            ModCore.info(msg, params);
        }
        public static void warn(String msg, Object...params) {
            ModCore.warn(msg, params);
        }
        public static void error(String msg, Object...params) {
            ModCore.error(msg, params);
        }
        public static void catching(Throwable ex) {
            ModCore.catching(ex);
        }
    }

    public List<Mod> getLoadedMods() {
        return mods;
    }

    private static Proxy proxy = DistExecutor.runForDist(() -> ClientProxy::new, () -> ServerProxy::new);
    /** Hooked into forge's proxy system and fires off corresponding events */
    public static class Proxy {
        public Proxy() {
            proxy = this;
        }

        public void event(ModEvent event) {
            instance.mods.forEach(m -> event(event, m));
        }
        public void event(ModEvent event, Mod m) {
            m.commonEvent(event);
        }

        public void setup() {
        }
    }

    public static class ClientProxy extends Proxy {
        static int MaxTextureSize = -1;

        public ClientProxy() {
            super();

            try {
                throw new Exception();
            } catch (Exception ex) {
                if (Arrays.toString(ex.getStackTrace()).contains("net.minecraftforge.fml.ModLoader.runDataGenerator")) {
                    ModCore.warn("Skipping MaxTextureSize detection during data generation");
                    return;
                }
            }
            if (FMLPaths.CONFIGDIR.get() != null) { /* not a test environment */
                MaxTextureSize = GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE);
                ModCore.info("Detected GL_MAX_TEXTURE_SIZE as: %s", MaxTextureSize);
            }
        }

        @Override
        public void setup() {
            if (Minecraft.getInstance() == null) {
                // Instance can be null during data gen
                return;
            }
            Config.getMaxTextureSize(); //populate

            BuiltinPack.loadClientResources();

            //Wrapper for lang format language files
            BuiltinPack.conditional(ident -> {
                String path = ident.getPath();
                if (!path.startsWith("lang/") || !path.endsWith(".json")) {
                    return null;
                }

                Identifier lang = new Identifier(ident.toString().replace(".json", ".lang"));
                if (!lang.canLoad()) {
                    return null;
                }

                Map<String, String> translationMap = new HashMap<>();
                try {
                    for (InputStream stream : lang.getResourceStreamAll()) {
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                //Remove comment
                                line = line.trim();
                                if (line.isEmpty() || line.startsWith("#")) {
                                    continue;
                                }

                                String[] splits = line.split("=", 2);
                                if (splits.length == 2) {
                                    translationMap.put(splits[0].trim(), splits[1].trim());
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                Set<String> translations = new HashSet<>();
                translationMap.forEach((key, value) -> {
                    if (!key.isEmpty()) {
                        translations.add(String.format("\"%s\": \"%s\"", key, value));
                        translations.add(String.format("\"%s\": \"%s\"", key.replace(":", "."), value));
                        translations.add(String.format("\"%s\": \"%s\"", key.replace(".name", ""), value));
                        translations.add(String.format("\"%s\": \"%s\"", key.replace(".name", "").replace(":", "."), value));
                    }
                });
                String output = "{" + String.join(",", translations) + "}";
                return output.getBytes(StandardCharsets.UTF_8);
            });
        }

        @Override
        public void event(ModEvent event, Mod m) {
            super.event(event, m);
            m.clientEvent(event);
        }
    }

    public static class ServerProxy extends Proxy {
        @Override
		public void event(ModEvent event, Mod m) {
            super.event(event, m);
            m.serverEvent(event);
        }
    }

    public static boolean isInReload() {
        return isInReload;
    }


    public static class Internal extends Mod {
        @Override
        public String modID() {
            return "universalmodcore";
        }

        @Override
        public void commonEvent(ModEvent event) {
            switch (event) {
                case CONSTRUCT:
                    Packet.register(EntitySync.EntitySyncPacket::new, PacketDirection.ServerToClient);
                    Packet.register(ModdedEntity.PassengerPositionsPacket::new, PacketDirection.ServerToClient);
                    Packet.register(ModdedEntity.PassengerSeatPacket::new, PacketDirection.ServerToClient);
                    Packet.register(Mouse.MousePressPacket::new, PacketDirection.ClientToServer);
                    Command.register(new ModCoreCommand());
                    ConfigFile.sync(Config.class);
                    break;
                case INITIALIZE:
                    break;
                case SETUP:
//                    CommonEvents.World.LOAD.subscribe(w -> w.increaseMaxEntityRadius(32));
                    break;
                case FINALIZE:
                    ChunkManager.setup();
                    break;
                case START:
                    break;
            }
        }

        @Override
        public void clientEvent(ModEvent event) {
            switch (event) {
                case CONSTRUCT:
                    // Instance can be null during data gen
                    if (Minecraft.getInstance() != null) {
                        ((IReloadableResourceManager) Minecraft.getInstance().getResourceManager()).addReloadListener((stage, resourceManager, preparationsProfiler, reloadProfiler, backgroundExecutor, gameExecutor) ->
                                stage.markCompleteAwaitingOthers(Unit.INSTANCE).thenRun(ClientEvents::fireReload).thenRun(BuiltinPack::reload));
                        Light.register();
                    }
                    break;
                case SETUP:
                    try {
                        Minecraft.getInstance().populateSearchTreeManager();
                    } catch (Exception ex) {
                        ModCore.catching(ex);
                    }
                    //BlockRender.onPostColorSetup();
                    //ClientEvents.fireReload();
                    break;
            }

        }

        @Override
        public void serverEvent(ModEvent event) {
        }
    }

    public static void debug(String msg, Object... params) {
        if (Config.DebugLogging) {
            if (instance == null || instance.logger == null) {
                System.out.println("DEBUG: " + String.format(msg, params));
                return;
            }

            if (params.length != 0) {
                instance.logger.info(String.format(msg, params));
            } else {
                instance.logger.info(msg);
            }
        }
    }

    public static void info(String msg, Object... params) {
        if (instance == null || instance.logger == null) {
            System.out.println("INFO: " + String.format(msg, params));
            return;
        }
        instance.logger.info(String.format(msg, params));
    }

    public static void warn(String msg, Object... params) {
        if (instance == null || instance.logger == null) {
            System.out.println("WARN: " + String.format(msg, params));
            return;
        }

        instance.logger.warn(String.format(msg, params));
    }

    public static void error(String msg, Object... params) {
        if (instance == null || instance.logger == null) {
            System.out.println("ERROR: " + String.format(msg, params));
            return;
        }

        instance.logger.error(String.format(msg, params));
    }

    public static void catching(Throwable ex, String msg, Object... params) {
        error(msg, params);
        catching(ex);
    }

    public static void catching(Throwable ex) {
        if (instance == null || instance.logger == null) {
            ex.printStackTrace();
            return;
        }

        instance.logger.error("Exception", ex);
    }

    private static final List<File> usedCacheFiles = new ArrayList<>();

    /** Get a file for name in the UMC cache dir */
    public static synchronized File cacheFile(Identifier id) {
        Path configDir = MinecraftFiles.getConfigDir().toPath();
        File cacheDir = Paths.get(configDir.getParent().toFile().getPath(), "cache", id.getDomain()).toFile();
        cacheDir.mkdirs();

        // https://stackoverflow.com/questions/1155107/is-there-a-cross-platform-java-method-to-remove-filename-special-chars#comment96425990_17745189
        String path = id.getPath().replaceAll("(?U)[^\\w\\._]+", ".");
        if (SystemUtils.IS_OS_WINDOWS) {
            // In a world with linux, who needs windows or gates?
            path = StringUtils.right(path, 250 - cacheDir.getAbsolutePath().length()); // Windows default max *Path* len is 256
        } else {
            path = StringUtils.right(path, 250); // Most FS's allow up to 255
        }
        File f = new File(cacheDir, path);
        usedCacheFiles.add(f);
        return f;
    }

    /* Loader Utils */
    public static String loaderBrand() {
        return "forge";
    }

    public static int mcVersion() {
        return 11502;
    }

    public static boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
    }
}
