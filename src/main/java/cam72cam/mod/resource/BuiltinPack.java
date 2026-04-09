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
    private static final HashMap<Identifier, Identifier> REDIRECTS = new HashMap<>();
    private static final List<Function<Identifier, byte[]>> GENERATORS = new LinkedList<>();
    private static final HashMap<Identifier, byte[]> CACHED_GENERATOR_RESULTS = new HashMap<>();

    /**
     * Directly attach a resource to the game
     */
    public static void addResource(Identifier resource, byte[] content) {
        DIRECT_RESOURCES.put(resource, content);
    }

    /**
     * Attach a conditionally generated resource to the game. The result will be cached until next reload.
     */
    public static void addGenerator(Function<Identifier, byte[]> func) {
        GENERATORS.add(func);
    }

    /**
     * Add a redirect logic for resources, especially useful when handling cross-version compatibilities
     */
    public static void addRedirect(Identifier from, Identifier to) {
        if (!to.canLoad())
            throw new RuntimeException("Invalid redirect to: " + to);
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

    public static void loadWrappedResource(List<IResourcePack> packs) {
        IResourcePack pack = new InternalPack();
        packs.add(1, pack);
        packs.add(pack);
    }

    public static void reload() {
        CACHED_GENERATOR_RESULTS.clear();
    }

    private static class InternalPack extends AbstractResourcePack {
        public InternalPack() {
            super(null);
        }

        @Override
        protected InputStream getInputStreamByName(String resourcePath) throws IOException {
            if("pack.mcmeta".equals(resourcePath)) {
                return new ByteArrayInputStream("{}".getBytes());
            }

            Identifier identifier = nameToLocation(resourcePath);

            //TODO more robust logic
            for (Map.Entry<Identifier, Identifier> entry : REDIRECTS.entrySet()) {
                if (identifier.toString().startsWith(entry.getKey().toString())) {
                    Identifier target = new Identifier(identifier.toString().replace(entry.getKey().toString(), entry.getValue().toString()));
                    return target.getResourceStream();
                }
            }

            if (DIRECT_RESOURCES.containsKey(identifier)) {
                return new ByteArrayInputStream(DIRECT_RESOURCES.get(identifier));
            }

            //It must already have been cached in hasResourceName if exists
            if (CACHED_GENERATOR_RESULTS.containsKey(identifier)) {
                return new ByteArrayInputStream(CACHED_GENERATOR_RESULTS.get(identifier));
            }

            return null;
        }

        @Override
        protected boolean hasResourceName(String resourcePath) {
            Identifier identifier = nameToLocation(resourcePath);

            if (DIRECT_RESOURCES.containsKey(identifier)) {
                return true;
            }

            for (Map.Entry<Identifier, Identifier> entry : REDIRECTS.entrySet()) {
                if (identifier.toString().startsWith(entry.getKey().toString())) {
                    return true;
                }
            }

            if (CACHED_GENERATOR_RESULTS.containsKey(identifier)) {
                return true;
            }

            synchronized (GENERATORS) {
                for (Function<Identifier, byte[]> generator : GENERATORS) {
                    byte[] stream = generator.apply(identifier);
                    if (stream != null) {
                        CACHED_GENERATOR_RESULTS.put(identifier, stream);
                        return true;
                    }
                }
            }

            return false;
        }

        @Override
        public Set<String> getResourceDomains() {
            Set<String> set = ModCore.instance.getLoadedMods().stream().map(ModCore.Mod::modID).collect(Collectors.toSet());
            set.add("universalmodcore");
            return set;
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

    private static Identifier nameToLocation(String path) {
        if(path.startsWith("assets/")) {
            //assets/[domain]/[path] -> domain:path
            path = path.substring(7);
            int x = path.indexOf('/');
            return new Identifier(path.substring(0, x), path.substring(x + 1));
        }
        //Not possible to hit below 1.12, except for pack.mcmeta
        return null;
    }
}
