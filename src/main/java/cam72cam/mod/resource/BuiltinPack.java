package cam72cam.mod.resource;

import cam72cam.mod.ModCore;
import cam72cam.mod.event.platform.LoadDatapackEvent;
import com.mojang.logging.LogUtils;
import net.minecraft.FileUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.world.flag.FeatureFlagSet;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Utilities for wrapping resources, only available within UMC mods' namespaces.
 * <p>
 * Should be called as soon as possible!
 * <p>
 * When handling request, static resources added via <code>put</code> have the highest priority,
 * then <code>redirect</code>, then<code>conditional</code>.
 * */
@EventBusSubscriber(modid = ModCore.MODID)
public class BuiltinPack {
    private static final HashMap<Identifier, byte[]> DIRECT_RESOURCES = new HashMap<>();
    //Stringified identifier, longer is better
    private static final TreeMap<String, String> REDIRECTS =
            new TreeMap<>((a, b) -> {
                int d = Integer.compare(b.length(), a.length());
                return  d != 0 ? d : a.compareTo(b);
            });
    private static final List<Function<Identifier, byte[]>> GENERATORS = new LinkedList<>();
    private static final HashMap<Identifier, byte[]> CACHED_GENERATOR_RESULTS = new HashMap<>();

    /**
     * Registers a static resource.
     * <p>
     * The given bytes are returned as-is whenever this identifier is requested.
     * If the same identifier is registered again, the latest value wins.
     */
    public static void put(Identifier resource, byte[] content) {
        DIRECT_RESOURCES.put(resource, content);
    }

    /**
     * Registers a conditional resource generator.
     * <p>
     * The function is called with the requested identifier and should return:
     * <ul>
     *   <li>resource bytes, if this generator wants to provide it</li>
     *   <li>{@code null}, if it does not handle this identifier</li>
     * </ul>
     * Generated results are cached after the first successful generation, until next resource reload.
     */
    public static void conditional(Function<Identifier, byte[]> func) {
        GENERATORS.add(func);
    }

    /**
     * Registers a resource path redirect.
     * <p>
     * Any requested identifier whose string form starts with {@code requestedPrefix}
     * will be remapped back to {@code actualPrefix} (simple replacement).
     * This is mainly intended for compatibility aliases (e.g. cross-version path changes).
     */
    public static void redirect(Identifier requestedPrefix, Identifier actualPrefix) {
        //Namespaces will be redirected!
        String requested = requestedPrefix.toString();
        String actual = actualPrefix.toString();
        if (actual.startsWith(requested)) {
            throw new IllegalArgumentException("Attempting to redirect to child folders, this is not allowed! Redirect with full file name instead!");
        }
        REDIRECTS.put(requested, actual);
    }

    /**
     * Registers a file or folder as a resource pack to the game.
     */
    public static PackResources attach(File path) {
        if (path.isDirectory()) {
            return new UMCFolderPack(path);
        } else {
            return new UMCFilePack(path);
        }
    }

    /**
     * Internal, for putting datapack entries
     */
    public static void putData(ResourceLocation location, byte[] content) {
        InternalDataPack.data.put(location, content);
    }

    /**
     * Internal
     */
    public static void loadClientResources() {
        List<PackResources> packs = new ArrayList<>();

        for (ModCore.Mod mod : ModCore.instance.getLoadedMods()) {
            BuiltinPack.loadModResource(mod, packs);
        }

        PackResources pack = new InternalResourcePack();
        //Ensure people will get our result first via getResourceStream() and getLastResourceStream()
        packs.add(1, pack);
        packs.add(pack);

        Minecraft.getInstance().getResourcePackRepository().addPackFinder((consumer) -> {
            for (PackResources pack1 : packs) {
                //TODO 1.21.1
                PackLocationInfo info = new PackLocationInfo(pack1.packId(),
                                                             Component.literal(""),
                                                             PackSource.BUILT_IN,
                                                             Optional.empty());
                consumer.accept(new Pack(
                        info,
                        new Pack.ResourcesSupplier() {
                            @Override
                            public PackResources openPrimary(PackLocationInfo p_326301_) {
                                return pack1;
                            }

                            @Override
                            public PackResources openFull(PackLocationInfo p_326241_, Pack.Metadata p_325959_) {
                                return pack1;
                            }
                        },
                        new Pack.Metadata(Component.literal(""), PackCompatibility.COMPATIBLE, FeatureFlagSet.of(), List.of(), false),
                        new PackSelectionConfig(true, Pack.Position.TOP, true)
                ));
            }
        });
    }

    /**
     * Internal
     */
    @SubscribeEvent
    public static void loadServerResource(LoadDatapackEvent event) {
        event.addDataPack(new InternalDataPack());
    }

    private static void loadModResource(ModCore.Mod mod, List<PackResources> packs) {
        String configDir = FMLPaths.CONFIGDIR.toString();
        new File(configDir).mkdirs();

        PackResources modPack = BuiltinPack.attach(ModList.get().getModFileById(mod.modID()).getFile().getFilePath().toFile());
        // Ensure people will get our result first via getResourceStream() and getLastResourceStream()
        // (Also injects last modified time access)
        // BUG: sounds can still be overridden by resource packs
        packs.add(modPack);
        File folder = new File(configDir + File.separator + mod.modID());
        if (folder.exists()) {
            if (folder.isDirectory()) {
                File[] files = folder.listFiles(file -> file.getName().endsWith(".zip"));
                for (File file : files) {
                    packs.add(BuiltinPack.attach(file));
                }

                File[] folders = folder.listFiles(File::isDirectory);
                for (File dir : folders) {
                    packs.add(BuiltinPack.attach(dir));
                }
            }
        } else {
            folder.mkdirs();
        }
        packs.add(modPack);
    }

    /**
     * Internal
     */
    public static void reload() {
        CACHED_GENERATOR_RESULTS.clear();
    }

    /**
     * Internal, Client side assets loading
     */
    private static class InternalResourcePack extends AbstractPackResources {
        public InternalResourcePack() {
            super(new PackLocationInfo("UMC Generated Resources",
                                       Component.literal("UMC Generated Resources"),
                                       PackSource.BUILT_IN,
                                       Optional.empty()));
        }

        @Override
        public @org.jetbrains.annotations.Nullable IoSupplier<InputStream> getRootResource(String... strs) {
            String path = String.join("/", strs);
            if (hasResource(path)) {
                return () -> getResource(path);
            }
            return null;
        }

        @Override
        public IoSupplier<InputStream> getResource(PackType type, ResourceLocation loc) {
            String path = locationToName(type, loc);
            if (hasResource(path)) {
                return () -> getResource(path);
            }
            return null;
        }

        private InputStream getResource(String resourcePath) {
            Identifier ident = nameToLocation(resourcePath);

            try {
                if (DIRECT_RESOURCES.containsKey(ident)) {
                    return new ByteArrayInputStream(DIRECT_RESOURCES.get(ident));
                }

                for (Map.Entry<String, String> entry : REDIRECTS.entrySet()) {
                    String src = ident.toString();
                    if (src.startsWith(entry.getKey())) {
                        Identifier redirect = handleRedirect(ident, entry.getKey(), entry.getValue());
                        return redirect.getResourceStream();
                    }
                }

                //It must already have been populated in hasResourceName if exists
                if (CACHED_GENERATOR_RESULTS.containsKey(ident)) {
                    return new ByteArrayInputStream(CACHED_GENERATOR_RESULTS.get(ident));
                }
            } catch (Exception ignored) {
            }
            return null;
        }

        private boolean hasResource(String resourcePath) {
            if (resourcePath.endsWith("mcmeta") && !"pack.mcmeta".equals(resourcePath)) {
                //We don't handle resource metadata
                return false;
            }

            Identifier ident = nameToLocation(resourcePath);

            if (DIRECT_RESOURCES.containsKey(ident)) {
                return true;
            }

            for (Map.Entry<String, String> entry : REDIRECTS.entrySet()) {
                //Check if it's start with any of the [to]s
                if (ident.toString().startsWith(entry.getKey())
                        && handleRedirect(ident, entry.getKey(), entry.getValue()).canLoad()) {
                    return true;
                }
            }

            if (CACHED_GENERATOR_RESULTS.containsKey(ident)) {
                return true;
            }

            synchronized (GENERATORS) {
                for (Function<Identifier, byte[]> generator : GENERATORS) {
                    byte[] stream = generator.apply(ident);
                    if (stream != null) {
                        CACHED_GENERATOR_RESULTS.put(ident, stream);
                        return true;
                    }
                }
            }

            return false;
        }

        @Override
        public void listResources(PackType type, String pathIn, String namespace, PackResources.ResourceOutput output) {
            //TODO list all redirect/conditional resources, may need new parameters in API?
            final String folder = pathIn + "/"; // Ensure folders
            DIRECT_RESOURCES.forEach((k, v) -> {
                String path = k.getPath();
                if(k.getDomain().equals(namespace) && path.startsWith(folder)) {
                    output.accept(k.internal, () -> new ByteArrayInputStream(v));
                }
            });

            CACHED_GENERATOR_RESULTS.forEach((k, v) -> {
                String path = k.getPath();
                if(k.getDomain().equals(namespace) && path.startsWith(folder)) {
                    output.accept(k.internal, () -> new ByteArrayInputStream(v));
                }
            });
        }

        @Override
        public Set<String> getNamespaces(PackType type) {
            Set<String> collect = ModCore.instance.getLoadedMods().stream().map(ModCore.Mod::modID).collect(Collectors.toSet());
            collect.add("universalmodcore");
            return collect;
        }

        @Nullable
        @Override
        public <T> T getMetadataSection(MetadataSectionSerializer<T> p_195760_1_) throws IOException {
            return getMetadataFromStream(p_195760_1_, new ByteArrayInputStream("{}".getBytes()));
        }

        @Override
        public void close() {
            //Have nothing to do here
        }
    }

    /**
     * Internal, Server side assets loading
     */
    public static InputStream loadServerResource(Identifier ident) throws IOException {
        if (DIRECT_RESOURCES.containsKey(ident)) {
            return new ByteArrayInputStream(DIRECT_RESOURCES.get(ident));
        }

        for (Map.Entry<String, String> entry : REDIRECTS.entrySet()) {
            String src = ident.toString();
            if (src.startsWith(entry.getKey())) {
                Identifier redirect = handleRedirect(ident, entry.getKey(), entry.getValue());
                return redirect.getResourceStream();
            }
        }

        if (CACHED_GENERATOR_RESULTS.containsKey(ident)) {
            return new ByteArrayInputStream(CACHED_GENERATOR_RESULTS.get(ident));
        }

        synchronized (GENERATORS) {
            for (Function<Identifier, byte[]> generator : GENERATORS) {
                byte[] stream = generator.apply(ident);
                if (stream != null) {
                    CACHED_GENERATOR_RESULTS.put(ident, stream);
                    return new ByteArrayInputStream(CACHED_GENERATOR_RESULTS.get(ident));
                }
            }
        }

        return null;
    }

    /**
     * Internal, Server side data loading
     */
    public static class InternalDataPack extends AbstractPackResources {
        static Map<ResourceLocation, byte[]> data = new HashMap<>();

        public InternalDataPack() {
            super(new PackLocationInfo("UMC Generated Resources",
                                       Component.literal("UMC Generated Resources"),
                                       PackSource.BUILT_IN,
                                       Optional.empty()));
        }

        @Override
        public @org.jetbrains.annotations.Nullable IoSupplier<InputStream> getRootResource(String... strs) {
            String path = String.join("/", strs);
            if (hasResource(path)) {
                return () -> getResource(path);
            }
            return null;
        }

        @Override
        public IoSupplier<InputStream> getResource(PackType type, ResourceLocation loc) {
            String path = locationToName(type, loc);
            if (hasResource(path)) {
                return () -> getResource(path);
            }
            return null;
        }

        private InputStream getResource(String resourcePath) throws IOException {
            if("pack.mcmeta".equals(resourcePath)) {
                return new ByteArrayInputStream("""
                                                {
                                                    "pack": {
                                                        "description": {"text":"UMC Generated Resources"},
                                                		"pack_format": 9999,
                                                        "supported_formats": [0, 9999],
                                                		"min_format": 0,
                                                        "max_format": 9999
                                                    }
                                                }
                                                """.getBytes());
            }

            return new ByteArrayInputStream(data.get(nameToLocation(resourcePath).internal));
        }

        private boolean hasResource(String resourcePath) {
            return "pack.mcmeta".equals(resourcePath) || data.containsKey(nameToLocation(resourcePath).internal);
        }

        @Override
        public void listResources(PackType type, String namespace, String pathIn, PackResources.ResourceOutput output) {
            List<ResourceLocation> result = new ArrayList<>();
            final String folder = pathIn + "/"; // Ensure folders
            data.keySet().forEach((k) -> {
                String path = k.getPath();
                if(k.getNamespace().equals(namespace) && path.startsWith(folder)) {
                    output.accept(k, () -> new ByteArrayInputStream(data.get(k)));
                }
            });
        }

        @Override
        public Set<String> getNamespaces(PackType type) {
            Set<String> collect = ModCore.instance.getLoadedMods().stream().map(ModCore.Mod::modID).collect(Collectors.toSet());
            collect.add("universalmodcore");
            return collect;
        }

        @Override
        public void close() {

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

    private static Identifier handleRedirect(Identifier src, String requestedPrefix, String actualPrefix) {
        //Replace [requestedPrefix] with [actualPrefix] to redirect the request back
        String suffix = src.toString().substring(requestedPrefix.length());
        return new Identifier(actualPrefix + suffix);
    }

    private static String locationToName(PackType type, ResourceLocation path) {
        String pattern = type == PackType.CLIENT_RESOURCES ? "assets/%s/%s" : "data/%s/%s";
        return String.format(pattern, path.getNamespace(), path.getPath());
    }

    private static Identifier nameToLocation(String path) {
        if(path.startsWith("assets/")) {
            //assets/[domain]/[path] -> domain:path
            path = path.substring(7);
            int x = path.indexOf('/');
            return new Identifier(path.substring(0, x), path.substring(x + 1));
        } else if(path.startsWith("data/")) {
            //data/[domain]/[path] -> domain:path
            path = path.substring(5);
            int x = path.indexOf('/');
            return new Identifier(path.substring(0, x), path.substring(x + 1));
        }
        return new Identifier("universalmodcore", "invalid");
    }
}
