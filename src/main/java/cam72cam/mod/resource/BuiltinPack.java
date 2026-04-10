package cam72cam.mod.resource;

import cam72cam.mod.ModCore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.*;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraftforge.fml.common.Loader;

import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Utilities for wrapping resources across versions
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
     * Directly attach a resource to the game
     */
    public static void put(Identifier resource, byte[] content) {
        DIRECT_RESOURCES.put(resource, content);
    }

    /**
     * Attach a conditionally generated resource to the game, return null in the function if conditions not met.
     * <p>
     * Once the resource is successfully generated, it will be cached until the next reload.
     */
    public static void conditional(Function<Identifier, byte[]> func) {
        GENERATORS.add(func);
    }

    /**
     * Add a redirect logic for resources, especially useful when handling cross-version compatibilities
     * <p>
     * UMC will match all locations start with <code>from</code>, and replace them with <code>to</code> .
     */
    public static void redirect(Identifier from, Identifier to) {
        REDIRECTS.put(from, to);
    }

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

    public static void loadUMCResource(List<IResourcePack> packs) {
        IResourcePack pack = new InternalPack();
        packs.add(1, pack);
        packs.add(pack);
    }

    public static void reload() {
        CACHED_GENERATOR_RESULTS.clear();
    }

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

            for (Map.Entry<Identifier, Identifier> entry : REDIRECTS.entrySet()) {
                String src = ident.toString();
                if (src.startsWith(entry.getValue().toString())) {
                    Identifier redirect = handleRedirect(ident, entry.getKey(), entry.getValue());
                    return redirect.getResourceStream();
                }
            }

            if (DIRECT_RESOURCES.containsKey(ident)) {
                return new ByteArrayInputStream(DIRECT_RESOURCES.get(ident));
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

    private static Identifier handleRedirect(Identifier src, Identifier from, Identifier to) {
        //Replace [to] with [from] to implement redirect
        String suffix = src.toString().substring(to.toString().length());
        return new Identifier(from.toString() + suffix);
    }

    private static Identifier nameToLocation(String path) {
        if(path.startsWith("assets/")) {
            //assets/[domain]/[path] -> domain:path
            path = path.substring(7);
            int x = path.indexOf('/');
            return new Identifier(path.substring(0, x), path.substring(x + 1));
        }
        //Not possible to hit below 1.12, except for pack.mcmeta
        return new Identifier("universalmodcore", "invalid");
    }
}
