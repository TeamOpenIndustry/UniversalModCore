package cam72cam.mod;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import net.minecraft.resources.*;
import net.minecraft.resources.data.IMetadataSectionSerializer;
import net.minecraft.resources.data.PackMetadataSection;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cam72cam.mod.config.ConfigFile;
import cam72cam.mod.entity.ModdedEntity;
import cam72cam.mod.entity.sync.EntitySync;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.input.Mouse;
import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.Recipes;
import cam72cam.mod.net.Packet;
import cam72cam.mod.net.PacketDirection;
import cam72cam.mod.render.Light;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.text.Command;
import cam72cam.mod.util.ModCoreCommand;
import cam72cam.mod.world.ChunkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.profiler.IProfiler;
import net.minecraft.util.Unit;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent;
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
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** UMC Mod, do not touch... */
@net.minecraftforge.fml.common.Mod(ModCore.MODID)
public class ModCore {
    public static final String MODID = "universalmodcore";
    public static final String NAME = "UniversalModCore";
    public static final String VERSION = "1.1.4";
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

    /** SETUP Phase (Forge) */
    public void init(InterModEnqueueEvent event) {
        proxy.event(ModEvent.SETUP);
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
        public void event(ModEvent event, Mod m) {
            // Instance can be null during data gen
            if (event == ModEvent.CONSTRUCT && Minecraft.getInstance() != null) {
                Config.getMaxTextureSize(); //populate

                List<UMCResourcePack> packs = new ArrayList<>();
                packs.add(new TranslationResourcePack());
                UMCResourcePack modPack = createPack(((ModFileInfo) ModLoadingContext.get().getActiveContainer().getModInfo().getOwningFile()).getFile().getFilePath().toFile());
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

                // Force first and last (and inject mod time) BUG: sounds can still be overridden by resource packs
                Minecraft.getInstance().getResourcePackList().addPackFinder(new IPackFinder() {
                    @Override
                    public <T extends ResourcePackInfo> void addPackInfosToMap(Map<String, T> nameToPackMap, ResourcePackInfo.IFactory<T> packInfoFactory) {

                        final T packInfo = ResourcePackInfo.createResourcePack("umc_inject", true, () -> new CombinedResourcePack("umc_inject", "UMC Resources",
                                new PackMetadataSection(new StringTextComponent("Universal Mod Core"), 4), packs) , packInfoFactory, ResourcePackInfo.Priority.TOP);
                        nameToPackMap.put("umc_inject", packInfo);
                    }
                });
            }
            super.event(event, m);
            m.clientEvent(event);
        }

        @OnlyIn(Dist.CLIENT)
        private interface UMCResourcePack extends IResourcePack {
            boolean resourceExists(String resourcePath);

            InputStream getInputStream(String resourcePath) throws IOException;
        }

        private static class TranslationResourcePack extends ResourcePack implements UMCResourcePack {
            public TranslationResourcePack() {
                super(null);
            }

            private ResourceLocation toLang(String path) {
                // assets/mod/location
                //return String.format("%s/%s/%s", type.getDirectoryName(), location.getNamespace(), location.getPath());
                String[] parts = path.split("/");
                String type = parts[0];
                String namespace = parts[1];
                String prefix = String.format("%s/%s/", type, namespace);
                path = path.replace(prefix, "").replace(".json", ".lang");
                String lang = path.split("_")[1].replace(".lang", "");
                path = path.replace("_" + lang, "_" + lang.toUpperCase(Locale.ROOT));
                return new ResourceLocation(namespace, path.toLowerCase(Locale.ROOT)) {
                    @Override
                    public String getPath() {
                        // Very evil...
                        return path;
                    }
                };
            }

            @Override
            public boolean resourceExists(String resourcePath) {
                if (resourcePath.contains("/lang/") && resourcePath.endsWith(".json")) {
                    ResourceLocation lang = toLang(resourcePath);
                    return Minecraft.getInstance().getResourceManager().hasResource(lang);
                }
                return false;
            }

            @Override
            public InputStream getInputStream(String resourcePath) throws IOException {
                if (resourcePath.contains("/lang/") && resourcePath.endsWith(".json")) {
                    // Magical Translations!
                    ResourceLocation lang = toLang(resourcePath);
                    if (Minecraft.getInstance().getResourceManager().hasResource(lang)) {
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Minecraft.getInstance().getResourceManager().getResource(lang).getInputStream()))) {
                            List<String> translations = new ArrayList<>();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                String[] splits = line.split("=", 2);
                                if (splits.length == 2) {
                                    String key = splits[0];
                                    String value = splits[1];

                                    translations.add(String.format("\"%s\": \"%s\"", key, value));
                                    translations.add(String.format("\"%s\": \"%s\"", key.replace(":", "."), value));
                                    translations.add(String.format("\"%s\": \"%s\"", key.replace(".name", ""), value));
                                    translations.add(String.format("\"%s\": \"%s\"", key.replace(".name", "").replace(":", "."), value));
                                }
                            }
                            String output = "{" + String.join(",", translations) + "}";
                            return new ByteArrayInputStream(output.getBytes(StandardCharsets.UTF_8));
                        }
                    }
                }
                return null;
            }

            @Override
            public Collection<ResourceLocation> getAllResourceLocations(ResourcePackType type, String pathIn, int maxDepth, Predicate<String> filter) {
                return Collections.emptyList();
            }

            @Override
            public Set<String> getResourceNamespaces(ResourcePackType type) {
                return mods.stream().map(Mod::modID).collect(Collectors.toSet());
            }

            @Override
            public void close() throws IOException {

            }
        }

        private static class UMCFolderPack extends FolderPack implements UMCResourcePack {
            public UMCFolderPack(File folder) {
                super(folder);
            }

            @Override
            public InputStream getInputStream(String name) throws IOException {
                InputStream stream = super.getInputStream(name);
                File file = this.getFile(name);
                return new Identifier.InputStreamMod(stream, file.lastModified());
            }

            @Override
            public boolean resourceExists(String resourcePath) {
                return super.resourceExists(resourcePath);
            }
        }

        private static class UMCFilePack extends FilePack implements UMCResourcePack {
            private final File path;

            public UMCFilePack(File fileIn) {
                super(fileIn);
                this.path = fileIn;
            }

            @Override
            public InputStream getInputStream(String name) throws IOException {
                return new Identifier.InputStreamMod(super.getInputStream(name), path.lastModified());
            }
        }


        private static UMCResourcePack createPack(File path) {
            if (path.isDirectory()) {
                return new UMCFolderPack(path);
            } else {
                return new UMCFilePack(path);
            }
        }

        /**
         * Modified from Forge's DelegatingResourcePack
         */
        public static class CombinedResourcePack extends ResourcePack {
            private final List<UMCResourcePack> packs;
            private final String name;
            private final PackMetadataSection packInfo;

            public CombinedResourcePack(String id, String name, PackMetadataSection packInfo, List<UMCResourcePack> packs) {
                super(new File(id));
                this.name = name;
                this.packInfo = packInfo;
                this.packs = packs;
            }

            @Override
            public String getName() {
                return name;
            }

            @SuppressWarnings("unchecked")
            @Override
            public <T> T getMetadata(IMetadataSectionSerializer<T> deserializer) throws IOException {
                if (deserializer.getSectionName().equals("pack")) {
                    return (T) packInfo;
                }
                return null;
            }

            @Override
            public Collection<ResourceLocation> getAllResourceLocations(ResourcePackType type, String pathIn, int maxDepth, Predicate<String> filter) {
                synchronized (packs) {
                    return packs.stream()
                            .flatMap(r -> r.getAllResourceLocations(type, pathIn, maxDepth, filter).stream())
                            .collect(Collectors.toList());
                }
            }

            @Override
            public Set<String> getResourceNamespaces(ResourcePackType type) {
                synchronized (packs) {
                    return packs.stream()
                            .flatMap(r -> r.getResourceNamespaces(type).stream())
                            .collect(Collectors.toSet());
                }
            }

            @Override
            public void close() throws IOException {
                synchronized (packs) {
                    for (IResourcePack pack : packs) {
                        pack.close();
                    }
                }
            }

            @Override
            protected InputStream getInputStream(String resourcePath) throws IOException {
                if (!resourcePath.equals("pack.png")) // Mods shouldn't be able to mess with the pack icon
                {
                    synchronized (packs) {
                        for (UMCResourcePack pack : packs) {
                            if (pack.resourceExists(resourcePath)) {
                                return pack.getInputStream(resourcePath);
                            }
                        }
                    }
                }
                throw new ResourcePackFileNotFoundException(this.file, resourcePath);
            }

            @Override
            protected boolean resourceExists(String resourcePath) {
                synchronized (packs) {
                    for (UMCResourcePack pack : packs) {
                        if (pack.resourceExists(resourcePath)) {
                            return true;
                        }
                    }
                }
                return false;
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
            return "universalmodcoreinternal";
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
                    CommonEvents.World.LOAD.subscribe(w -> w.increaseMaxEntityRadius(32));
                    break;
                case FINALIZE:
                    ChunkManager.setup();
                    break;
                case START:
                    break;
            }
        }

        public interface SynchronousResourceReloadListener extends IFutureReloadListener {
            @Override
			default CompletableFuture<Void> reload(IFutureReloadListener.IStage stage, IResourceManager resourceManager, IProfiler preparationsProfiler, IProfiler reloadProfiler, Executor backgroundExecutor, Executor gameExecutor) {
                return stage.markCompleteAwaitingOthers(Unit.INSTANCE).thenRunAsync(() -> {
                    this.apply(resourceManager);
                }, backgroundExecutor);
            }

            void apply(IResourceManager var1);
        }

        @Override
        public void clientEvent(ModEvent event) {
            switch (event) {
                case CONSTRUCT:
                    // Instance can be null during data gen
                    if (Minecraft.getInstance() != null) {
                        ((IReloadableResourceManager) Minecraft.getInstance().getResourceManager()).addReloadListener((stage, resourceManager, preparationsProfiler, reloadProfiler, backgroundExecutor, gameExecutor) ->
                                stage.markCompleteAwaitingOthers(Unit.INSTANCE).thenRun(ClientEvents::fireReload));
                    }
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

    public static void genData(String MODID, GatherDataEvent event) {
        CommonEvents.Recipe.REGISTER.execute(Runnable::run);
        event.getGenerator().addProvider(new Recipes(event.getGenerator()));
        Fuzzy.register(event.getGenerator());
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

        instance.logger.catching(ex);
    }

    private static final List<File> usedCacheFiles = new ArrayList<>();

    /** Get a file for name in the UMC cache dir */
    public static synchronized File cacheFile(Identifier id) {
        Path configDir = FMLPaths.CONFIGDIR.get();
        if (configDir == null) {
            configDir = Paths.get(System.getProperty("java.io.tmpdir"), "minecraft");
        }
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
}
