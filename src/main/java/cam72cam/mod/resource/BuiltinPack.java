package cam72cam.mod.resource;

import cam72cam.mod.ModCore;
import cam72cam.mod.event.platform.LoadDatapackEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.ClientResourcePackInfo;
import net.minecraft.resources.*;
import net.minecraft.resources.data.IMetadataSectionSerializer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import javax.annotation.Nullable;
import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
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
    public static IResourcePack attach(File path) {
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
        List<IResourcePack> packs = new ArrayList<>();

        for (ModCore.Mod mod : ModCore.instance.getLoadedMods()) {
            BuiltinPack.loadModResource(mod, packs);
        }

        IResourcePack pack = new InternalResourcePack();
        //Ensure people will get our result first via getResourceStream() and getLastResourceStream()
        packs.add(1, pack);
        packs.add(pack);

        Minecraft.getInstance().getResourcePackList().addPackFinder(new IPackFinder() {
            @Override
            public <T extends ResourcePackInfo> void addPackInfosToMap(Map<String, T> nameToPackMap, ResourcePackInfo.IFactory<T> packInfoFactory) {
                for (IResourcePack pack : packs) {
                    //noinspection unchecked
                    nameToPackMap.put(pack.getName(), (T) new ClientResourcePackInfo(pack.getName(),
                            true,
                            () -> pack,
                            new StringTextComponent(""),
                            new StringTextComponent(""),
                            PackCompatibility.COMPATIBLE,
                            ResourcePackInfo.Priority.TOP,
                            true,
                            null,
                            true));
                }
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

    private static void loadModResource(ModCore.Mod mod, List<IResourcePack> packs) {
        String configDir = FMLPaths.CONFIGDIR.toString();
        new File(configDir).mkdirs();

        IResourcePack modPack = BuiltinPack.attach(ModList.get().getModFileById(mod.modID()).getFile().getFilePath().toFile());
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
    private static class InternalResourcePack extends ResourcePack {
        public InternalResourcePack() {
            super(ModList.get().getModFileById("universalmodcore").getFile().getFilePath().toFile());
        }

        @Override
        public InputStream getInputStream(String resourcePath) throws IOException {
            if("pack.mcmeta".equals(resourcePath)) {
                return new ByteArrayInputStream("{}".getBytes());
            }

            Identifier ident = nameToLocation(resourcePath);

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

            return null;
        }

        @Override
        public boolean resourceExists(String resourcePath) {
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
        public Collection<ResourceLocation> getAllResourceLocations(ResourcePackType type, String pathIn, String namespace, int maxDepth, Predicate<String> filter) {
            //TODO list all redirect/conditional resources, may need new parameters in API?
            List<ResourceLocation> result = new ArrayList<>();
            final String folder = pathIn + "/"; // Ensure folders
            DIRECT_RESOURCES.forEach((k, v) -> {
                String path = k.getPath();
                if(k.getDomain().equals(namespace) && path.startsWith(folder) && filter.test(path)) {
                    path = path.substring((folder).length());
                    if (path.chars().filter(ch -> ch == '/').count() < maxDepth) {
                        result.add(k.internal);
                    }
                }
            });

            CACHED_GENERATOR_RESULTS.forEach((k, v) -> {
                String path = k.getPath();
                if(k.getDomain().equals(namespace) && path.startsWith(folder) && filter.test(path)) {
                    path = path.substring((folder).length());
                    if (path.chars().filter(ch -> ch == '/').count() < maxDepth) {
                        result.add(k.internal);
                    }
                }
            });

            return result;
        }

        @Override
        public Set<String> getResourceNamespaces(ResourcePackType type) {
            Set<String> collect = ModCore.instance.getLoadedMods().stream().map(ModCore.Mod::modID).collect(Collectors.toSet());
            collect.add("universalmodcore");
            return collect;
        }

        @Override
        public String getName() {
            return "UMC Generated Resources";
        }

        @Nullable
        @Override
        public <T> T getMetadata(IMetadataSectionSerializer<T> p_195760_1_) throws IOException {
            return getResourceMetadata(p_195760_1_, new ByteArrayInputStream("{}".getBytes()));
        }

        @Override
        public void close() throws IOException {
            //Have nothing to do here
        }
    }

    /**
     * Internal, Server side assets loading
     */
    @OnlyIn(Dist.DEDICATED_SERVER)
    public static InputStream loadServerResource(Identifier ident) throws IOException {
        if (ident.getPath().endsWith("mcmeta")) {
            //We don't handle resource metadata
            return null;
        }

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
    public static class InternalDataPack extends ResourcePack {
        static Map<ResourceLocation, byte[]> data = new HashMap<>();

        public InternalDataPack() {
            super(ModList.get().getModFileById("universalmodcore").getFile().getFilePath().toFile());
        }

        @Override
        public InputStream getInputStream(String resourcePath) throws IOException {
            if("pack.mcmeta".equals(resourcePath)) {
                return new ByteArrayInputStream("{}".getBytes());
            }

            return new ByteArrayInputStream(data.get(nameToLocation(resourcePath).internal));
        }

        @Override
        public boolean resourceExists(String resourcePath) {
            return "pack.mcmeta".equals(resourcePath) || data.containsKey(nameToLocation(resourcePath).internal);
        }

        @Override
        public Collection<ResourceLocation> getAllResourceLocations(ResourcePackType type, String pathIn, String namespace, int maxDepth, Predicate<String> filter) {
            List<ResourceLocation> result = new ArrayList<>();
            final String folder = pathIn + "/"; // Ensure folders
            data.keySet().forEach((k) -> {
                String path = k.getPath();
                if(k.getNamespace().equals(namespace) && path.startsWith(folder) && filter.test(path)) {
                    path = path.substring((folder).length());
                    if (path.chars().filter(ch -> ch == '/').count() < maxDepth) {
                        result.add(k);
                    }
                }
            });
            return result;
        }

        @Override
        public Set<String> getResourceNamespaces(ResourcePackType type) {
            Set<String> collect = ModCore.instance.getLoadedMods().stream().map(ModCore.Mod::modID).collect(Collectors.toSet());
            collect.add("universalmodcore");
            return collect;
        }

        @Override
        public String getName() {
            return "UMC Generated Data";
        }

        @Override
        public void close() throws IOException {

        }
    }

    private static class UMCFolderPack extends FolderPack {
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

    private static class UMCFilePack extends FilePack {
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

    private static Identifier handleRedirect(Identifier src, String requestedPrefix, String actualPrefix) {
        //Replace [requestedPrefix] with [actualPrefix] to redirect the request back
        String suffix = src.toString().substring(requestedPrefix.length());
        return new Identifier(actualPrefix + suffix);
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
