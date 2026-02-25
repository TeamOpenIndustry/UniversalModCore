package cam72cam.mod;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import cam72cam.mod.event.CommonEvents;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraft.FileUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.Resource;

import java.util.*;

import net.minecraft.resources.*;
import net.minecraft.util.Unit;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.CreativeModeTabRegistry;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.data.loading.DatagenModLoader;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
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
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.text.Command;
import cam72cam.mod.util.MinecraftFiles;
import cam72cam.mod.util.ModCoreCommand;
import cam72cam.mod.world.ChunkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.*;
import net.neoforged.fml.loading.FMLEnvironment;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import javax.annotation.Nullable;
import java.io.*;
import java.util.*;
import java.io.File;
import java.io.IOException;
import java.util.stream.Collectors;

/** UMC Mod, do not touch... */
@net.neoforged.fml.common.Mod(ModCore.MODID)
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

        proxy.event(ModEvent.CONSTRUCT, ctr);
    }

    /** Called during Mod Construction phase */
    public ModCore(IEventBus modEventBus, Dist dist) {
        System.out.println("Welcome to UniversalModCore!");
        instance = this;

        ModCore.register(new Internal());
        proxy.setup();

        modEventBus.addListener(this::preInit);
        modEventBus.addListener(this::init);
        modEventBus.addListener(this::postInit);
        //FMLJavaModLoadingContext.get().getModEventBus().addListener(this::serverStarting);
        //FMLJavaModLoadingContext.get().getModEventBus().addListener(this::serverStarted);
        CommonEvents.Item.CREATIVE_TAB.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);
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
    public void serverStarting(ServerStartingEvent event) {
		// Formerly for command registration
    }

	/**
	 * <pre>
	 * Used to register commands.
	 * Moved from {@link ModCore#serverStarting serverStarting()}
	 * </pre>
	 *
	 * @param event
	 */
	@SubscribeEvent
	public void registerCommands(RegisterCommandsEvent event) {
		Command.registration(event.getDispatcher());
	}

    /** START Phase (Forge) */
    @SubscribeEvent
    public void serverStarted(ServerStartedEvent event) {
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

    private static Proxy proxy = FMLEnvironment.dist.isClient() ? new ClientProxy() : new ServerProxy();
    /** Hooked into forge's proxy system and fires off corresponding events */
    public static class Proxy {
        public Proxy() {
            proxy = this;
        }

        public void event(ModEvent event) {
            mods.forEach(m -> event(event, m));
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

            if (DatagenModLoader.isRunningDataGen()) {
                ModCore.warn("Skipping MaxTextureSize detection during data generation");
                return;
            }

            if (FMLPaths.CONFIGDIR.get() != null) { /* not a test environment */
                MaxTextureSize = RenderSystem.maxSupportedTextureSize();
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

            // Force first and last (and inject mod time) BUG: sounds can still be overridden by resource packs
            Minecraft.getInstance().getResourcePackRepository().addPackFinder(consumer -> {
                List<PackResources> packs = new ArrayList<>();
                packs.add(new TranslationResourcePack());

                for (Mod m : mods) {
                    PackResources modPack = createPack(ModList.get().getModFileById(m.modID()).getFile().getFilePath().toFile());
                    packs.add(modPack);
                    String configDir = FMLPaths.CONFIGDIR.get().toString();
                    new File(configDir).mkdirs();

                    File folder = new File(configDir + File.separator + m.modID());
                    if (folder.exists()) {
                        if (folder.isDirectory()) {
                            File[] files = folder.listFiles((dir, name) -> name.endsWith(".zip"));
                            for (File file : files) {
                                packs.add(createPack(file));
                            }

                            File[] folders = folder.listFiles((dir, name) -> dir.isDirectory());
                            for (File dir : folders) {
                                packs.add(createPack(dir));
                            }
                        }
                    } else {
                        folder.mkdirs();
                    }
                    packs.add(modPack);
                }


                for (PackResources pack : packs) {
                    //TODO 1.21.1
                    PackLocationInfo info = new PackLocationInfo(pack.packId(),
                                                                 Component.literal(""),
                                                                 PackSource.BUILT_IN,
                                                                 Optional.empty());
                    consumer.accept(new Pack(
                            info,
                            new Pack.ResourcesSupplier() {
                                @Override
                                public PackResources openPrimary(PackLocationInfo p_326301_) {
                                    return pack;
                                }

                                @Override
                                public PackResources openFull(PackLocationInfo p_326241_, Pack.Metadata p_325959_) {
                                    return pack;
                                }
                            },
                            new Pack.Metadata(Component.literal(""), PackCompatibility.COMPATIBLE, FeatureFlagSet.of(), List.of(), false),
                            new PackSelectionConfig(true, Pack.Position.TOP, true)
                            ));
                }
            });
        }

        @Override
        public void event(ModEvent event, Mod m) {
            super.event(event, m);
            m.clientEvent(event);
        }

        private static class TranslationResourcePack extends AbstractPackResources {
            public TranslationResourcePack() {
                super(new PackLocationInfo("translation Hackery",
                                           Component.literal("translation Hackery"),
                                           PackSource.BUILT_IN,
                                           Optional.empty()));
            }

            @org.jetbrains.annotations.Nullable
            @Override
            public IoSupplier<InputStream> getRootResource(String... p_252049_) {
                return null;
            }

            @org.jetbrains.annotations.Nullable
            @Override
            public IoSupplier<InputStream> getResource(PackType type, ResourceLocation resourcePath) {
                if (resourcePath.getPath().contains("lang/") && resourcePath.getPath().endsWith(".json")) {
                    // Magical Translations!
                    ResourceLocation lang = ResourceLocation.tryBuild(resourcePath.getNamespace(), resourcePath.getPath().replace("json", "lang"));
                    List<Resource> langFiles = Minecraft.getInstance().getResourceManager().getResourceStack(lang);
                    if (!langFiles.isEmpty()) {
                        Map<String, String> translationMap = new HashMap<>();
                        for (Resource resource : langFiles) {
                            try (BufferedReader reader = resource.openAsReader()) {
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    //Remove comment
                                    line = line.trim();
                                    int comment = line.indexOf("#");
                                    if (line.isEmpty() || comment == 0) {
                                        continue;
                                    }

                                    String[] splits = line.split("=", 2);
                                    if (splits.length == 2) {
                                        translationMap.put(splits[0].trim(), splits[1].trim());
                                    }
                                }
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
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
                        return () -> new ByteArrayInputStream(output.getBytes(StandardCharsets.UTF_8));
                    }
                }
                return null;
            }

            @Override
            public void listResources(PackType p_10289_, String p_251379_, String p_251932_, ResourceOutput p_249347_) {

            }

            @Override
            public Set<String> getNamespaces(PackType p_195759_1_) {
                return mods.stream().map(Mod::modID).collect(Collectors.toSet());
            }

            @Override
            public void close() {

            }

            @Nullable
            @Override
            public <T> T getMetadataSection(MetadataSectionSerializer<T> p_195760_1_) throws IOException {
                return getMetadataFromStream(p_195760_1_, new ByteArrayInputStream("{}".getBytes()));
            }
        }

        private static class UMCFolderPack extends PathPackResources {
            private final Path root;

            public UMCFolderPack(File folder) {
                super(new PackLocationInfo(folder.getName(),
                                           Component.literal(folder.getName()),
                                           PackSource.BUILT_IN,
                                           Optional.empty()),
                      folder.toPath());
                this.root = folder.toPath();
            }

            @Override
            public IoSupplier<InputStream> getResource(PackType p_249352_, ResourceLocation p_251715_) {
                Path path = this.root.resolve(p_249352_.getDirectory()).resolve(p_251715_.getNamespace());
                return getResource(p_251715_, path);
            }

            public static IoSupplier<InputStream> getResource(ResourceLocation p_250145_, Path p_251046_) {
                try {
                    List<String> list = FileUtil.decomposePath(p_250145_.getPath()).getPartialOrThrow();
                    String s = String.join("\\", list);
                    Path path = FileUtil.resolvePath(p_251046_, List.of(s));
                    return Files.exists(path)
                           ? new Identifier.IoInputStreamMod(IoSupplier.create(path), path.toFile().lastModified())
                           : null;
                } catch (IllegalStateException|IndexOutOfBoundsException e) {
                    LogUtils.getLogger().error("Invalid path {}", p_250145_);
                    return null;
                }
            }
        }

        private static class UMCFilePack extends FilePackResources {
            private final File path;

            public UMCFilePack(File fileIn) {
				super(new PackLocationInfo(fileIn.getName(),
                                           Component.literal(fileIn.getName()),
                                           PackSource.BUILT_IN,
                                           Optional.empty()),
                      new SharedZipFileAccess(fileIn), "");
                this.path = fileIn;
            }

            @Override
            public IoSupplier<InputStream> getResource(PackType p_249605_, ResourceLocation p_252147_) {
                IoSupplier<InputStream> found = super.getResource(p_249605_, p_252147_);
                return found != null ? new Identifier.IoInputStreamMod(found, path.lastModified()) : null;
            }
        }


        private static PackResources createPack(File path) {
            if (path.isDirectory()) {
                return new UMCFolderPack(path);
            } else {
                return new UMCFilePack(path);
            }
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
                    Light.register();
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
                        ((ReloadableResourceManager) Minecraft.getInstance().getResourceManager()).registerReloadListener((stage, resourceManager, preparationsProfiler, reloadProfiler, backgroundExecutor, gameExecutor) ->
                                stage.wait(Unit.INSTANCE).thenRun(ClientEvents::fireReload));
                    }
                case SETUP:
                    try {
                        CreativeModeTabRegistry.recalculateItemCreativeModeTabs();
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

        instance.logger.error("CATCHING", ex);
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
        return "neoforge";
    }

    public static int mcVersion() {
        return 12101;
    }

    public static boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
    }
}
