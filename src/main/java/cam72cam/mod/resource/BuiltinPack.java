package cam72cam.mod.resource;

import cam72cam.mod.ModCore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.*;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.*;
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
public class BuiltinPack {
    private static final HashMap<Identifier, byte[]> DIRECT_RESOURCES = new HashMap<>();
    private static final TreeMap<Identifier, Identifier> REDIRECTS =
            new TreeMap<>((a, b) -> {
                //Longer is better
                String aStr = a.toString();
                String bStr = b.toString();
                int d = Integer.compare(bStr.length(), aStr.length());
                return d != 0 ? d : aStr.compareTo(bStr);
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
     * Any requested identifier whose string form starts with {@code targetPrefix}
     * will be remapped back to {@code sourcePrefix} (simple replacement).
     * This is mainly intended for compatibility aliases (e.g. cross-version path changes).
     */
    public static void redirect(Identifier sourcePrefix, Identifier targetPrefix) {
        //Namespaces will be redirected!
        REDIRECTS.put(sourcePrefix, targetPrefix);
    }

    /**
     * Registers a file or folder as a resource pack to the game.
     */
    @SideOnly(Side.CLIENT)
    public static IResourcePack attach(File path) {
        if (path.isDirectory()) {
            return new FolderResourcePack(path) {
                @Override
                protected InputStream getInputStreamByName(String name) throws IOException {
                    InputStream stream = super.getInputStreamByName(name);
                    File file = this.getFile(name);
                    return new Identifier.InputStreamMod(stream, file.lastModified());
                }
            };
        } else {
            return new FileResourcePack(path) {
                @Override
                protected InputStream getInputStreamByName(String name) throws IOException {
                    return new Identifier.InputStreamMod(super.getInputStreamByName(name), resourcePackFile.lastModified());
                }
            };
        }
    }

    /**
     * Internal
     */
    public static void loadModResource(ModCore.Mod mod) {
        List<IResourcePack> packs = Minecraft.getMinecraft().defaultResourcePacks;

        String configDir = Loader.instance().getConfigDir().toString();
        new File(configDir).mkdirs();

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
    }

    /**
     * Internal
     */
    public static void onConstruct(List<IResourcePack> packs) {
        IResourcePack pack = new InternalPack();
        //Ensure people will get our result first via getResourceStream() and getLastResourceStream()
        packs.add(1, pack);
        packs.add(pack);
    }

    /**
     * Internal
     */
    public static void reload() {
        CACHED_GENERATOR_RESULTS.clear();
    }

    /**
     * Internal, Client side
     */
    @SideOnly(Side.CLIENT)
    private static class InternalPack extends AbstractResourcePack {
        public InternalPack() {
            //We're initializing UMC
            super(Loader.instance().activeModContainer().getSource());
        }

        @Override
        protected InputStream getInputStreamByName(String resourcePath) throws IOException {
            if("pack.mcmeta".equals(resourcePath)) {
                return new ByteArrayInputStream("{}".getBytes());
            }

            Identifier ident = nameToLocation(resourcePath);

            if (DIRECT_RESOURCES.containsKey(ident)) {
                return new ByteArrayInputStream(DIRECT_RESOURCES.get(ident));
            }

            for (Map.Entry<Identifier, Identifier> entry : REDIRECTS.entrySet()) {
                String src = ident.toString();
                if (src.startsWith(entry.getValue().toString())) {
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
        protected boolean hasResourceName(String resourcePath) {
            if (resourcePath.endsWith("mcmeta") && !"pack.mcmeta".equals(resourcePath)) {
                //We don't handle resource metadata
                return false;
            }

            Identifier ident = nameToLocation(resourcePath);

            if (DIRECT_RESOURCES.containsKey(ident)) {
                return true;
            }

            for (Map.Entry<Identifier, Identifier> entry : REDIRECTS.entrySet()) {
                //Check if it's start with any of the [to]s
                if (ident.toString().startsWith(entry.getValue().toString())
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
        public Set<String> getResourceDomains() {
            Set<String> collect = ModCore.instance.getLoadedMods().stream().map(ModCore.Mod::modID).collect(Collectors.toSet());
            collect.add("universalmodcore");
            return collect;
        }

        @Override
        public String getPackName() {
            return "UMC Generated Resources";
        }

        @Override
        public <T extends IMetadataSection> T getPackMetadata(MetadataSerializer metadataSerializer, String metadataSectionName) throws IOException {
            return super.getPackMetadata(metadataSerializer, metadataSectionName);
        }
    }

    /**
     * Internal, Server side
     */
    @SideOnly(Side.SERVER)
    public static InputStream loadServerResource(Identifier ident) throws IOException {
        if (ident.getPath().endsWith("mcmeta")) {
            //We don't handle resource metadata
            return null;
        }

        if (DIRECT_RESOURCES.containsKey(ident)) {
            return new ByteArrayInputStream(DIRECT_RESOURCES.get(ident));
        }

        for (Map.Entry<Identifier, Identifier> entry : REDIRECTS.entrySet()) {
            String src = ident.toString();
            if (src.startsWith(entry.getValue().toString())) {
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

    private static Identifier handleRedirect(Identifier src, Identifier sourcePrefix, Identifier targetPrefix) {
        //Replace [targetPrefix] with [sourcePrefix] to redirect the request back
        String suffix = src.toString().substring(targetPrefix.toString().length());
        return new Identifier(sourcePrefix.toString() + suffix);
    }

    private static Identifier nameToLocation(String path) {
        if(path.startsWith("assets/")) {
            //assets/[domain]/[path] -> domain:path, for 1.12- path
            path = path.substring(7);
            int x = path.indexOf('/');
            return new Identifier(path.substring(0, x), path.substring(x + 1));
        }
        //Not possible to hit below 1.12, except for pack.mcmeta
        return new Identifier("universalmodcore", "invalid");
    }
}
