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
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.*;
import java.nio.charset.StandardCharsets;
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
@Mod.EventBusSubscriber(modid = ModCore.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
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
    private static final HashSet<String> EXTRA_NAMESPACES = new HashSet<>();

    static {
        addNamespace("universalmodcore");
    }

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
    @OnlyIn(Dist.CLIENT)
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
    public static void addNamespace(String namespace) {
        EXTRA_NAMESPACES.add(namespace);
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
                consumer.accept(Pack.create(pack1.packId(),
                                            Component.literal(""),
                                            true,
                                            s -> pack1,
                                            new Pack.Info(Component.literal(""), 13, FeatureFlagSet.of()),
                                            PackType.CLIENT_RESOURCES,
                                            Pack.Position.TOP,
                                            true,
                                            PackSource.DEFAULT
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
        String configDir = FMLPaths.CONFIGDIR.get().toString();
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
            super("UMC Generated Resources", true);
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
        public void listResources(PackType type, String namespace, String pathIn, PackResources.ResourceOutput output) {
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
            collect.addAll(EXTRA_NAMESPACES);
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
    @OnlyIn(Dist.DEDICATED_SERVER)
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
            super("UMC Generated Resources", true);
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
                return new ByteArrayInputStream(("{\n" +
                                                 "  \"pack\": {\n" +
                                                 "    \"description\": \"UMC Generated Data\",\n" +
                                                 "    \"pack_format\": 15\n" +
                                                 "  }\n" +
                                                 "}").getBytes(StandardCharsets.UTF_8));
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
                if(k.getNamespace().equals(namespace) && path.startsWith(folder) && data.containsKey(k)) {
                    output.accept(k, () -> new ByteArrayInputStream(data.get(k)));
                }
            });
        }

        @Override
        public Set<String> getNamespaces(PackType type) {
            Set<String> collect = ModCore.instance.getLoadedMods().stream().map(ModCore.Mod::modID).collect(Collectors.toSet());
            collect.addAll(EXTRA_NAMESPACES);
            return collect;
        }

        @Override
        public void close() {

        }
    }

    private static class UMCFolderPack extends PathPackResources {
        private final Path root;

        public UMCFolderPack(File folder) {
            super(folder.getName(), folder.toPath(), false);
            this.root = folder.toPath();
        }

        @Override
        public IoSupplier<InputStream> getResource(PackType p_249352_, ResourceLocation p_251715_) {
            Path path = this.root.resolve(p_249352_.getDirectory()).resolve(p_251715_.getNamespace());
            return getResource(p_251715_, path);
        }

        public static IoSupplier<InputStream> getResource(ResourceLocation p_250145_, Path p_251046_) {
            return FileUtil.decomposePath(p_250145_.getPath()).get().map((p_251647_) -> {
                Path path = FileUtil.resolvePath(p_251046_, p_251647_);
                return Files.exists(path) ? new Identifier.IoInputStreamMod(() -> Files.newInputStream(path), path.toFile().lastModified()) : null;
            }, (p_248714_) -> {
                LogUtils.getLogger().error("Invalid path {}: {}", p_250145_, p_248714_.message());
                return null;
            });
        }
    }

    private static class UMCFilePack extends FilePackResources {
        private final File path;

        public UMCFilePack(File fileIn) {
            super(fileIn.getName(), fileIn, false);
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
