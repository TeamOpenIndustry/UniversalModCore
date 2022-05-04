package cam72cam.mod.resource;

import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.Resource;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Internal, do not use directly */
class Data {
    public static DataProxy proxy = DistExecutor.runForDist(() -> ClientProxy::new, () -> ServerProxy::new);

    public static abstract class DataProxy {
        private String configDir;

        public abstract List<InputStream> getResourceStreamAll(Identifier identifier) throws IOException;

        private InputStream getResourceStream(Identifier location, boolean reverse) throws IOException {
            InputStream chosen = null;
            List<InputStream> resources = getResourceStreamAll(location);
            if (reverse) {
                Collections.reverse(resources);
            }
            for (InputStream strm : resources) {
                if (chosen == null) {
                    chosen = strm;
                } else {
                    strm.close();
                }
            }
            if (chosen == null) {
                throw new java.io.FileNotFoundException(location.toString());
            }
            return chosen;
        }

        public InputStream getResourceStream(Identifier location) throws IOException {
            return getResourceStream(location, false);
        }
        public InputStream getLastResourceStream(Identifier location) throws IOException {
            return getResourceStream(location, true);
        }

        String pathString(Identifier location, boolean startingSlash) {
            return (startingSlash ? "/" : "") + "assets/" + location.getDomain() + "/" + location.getPath();
        }

        List<InputStream> getFileResourceStreams(Identifier location) throws IOException {
            List<InputStream> streams = new ArrayList<>();

            if (configDir == null) {
                configDir = FMLPaths.CONFIGDIR.get().toString();
                new File(configDir).mkdirs();
            }

            File folder = new File(this.configDir + File.separator + location.getDomain());
            if (folder.exists()) {
                if (folder.isDirectory()) {
                    File[] files = folder.listFiles((dir, name) -> name.endsWith(".zip"));
                    for (File file : files) {
                        ZipFile resourcePack = new ZipFile(file);
                        ZipEntry entry = resourcePack.getEntry(pathString(location, false));
                        if (entry != null) {
                            // Copy the input stream so we can close the resource pack
                            InputStream stream = resourcePack.getInputStream(entry);
                            streams.add(new ByteArrayInputStream(IOUtils.toByteArray(stream)));
                        }
                        resourcePack.close();
                    }
                    File[] folders = folder.listFiles((dir, name) -> true);
                    for (File dir : folders) {
                        if (dir.isDirectory()) {
                            File path = Paths.get(dir.getPath(), pathString(location, false)).toFile();
                            if (path.exists()) {
                                streams.add(new FileInputStream(path));
                            }
                        }
                    }
                }
            } else {
                folder.mkdirs();
            }
            return streams;
        }

    }

    public static class ClientProxy extends DataProxy {
        @Override
        public List<InputStream> getResourceStreamAll(Identifier identifier) throws IOException {
            List<InputStream> res = new ArrayList<>();
            try {
                for (Resource resource : Minecraft.getInstance().getResourceManager().getResources(identifier.internal)) {
                    res.add(resource.getInputStream());
                }
            } catch (java.io.FileNotFoundException ex) {
                // Ignore
            }
            res.addAll(getFileResourceStreams(identifier));
            return res;
        }
    }

    public static class ServerProxy extends DataProxy {
        private InputStream getEmbeddedResourceStream(Identifier location) throws IOException {
            URL url = this.getClass().getResource(pathString(location, true));
            return url != null ? this.getClass().getResourceAsStream(pathString(location, true)) : null;
        }

        @Override
        public List<InputStream> getResourceStreamAll(Identifier location) throws IOException {
            List<InputStream> res = new ArrayList<>();
            InputStream stream = getEmbeddedResourceStream(location);
            if (stream != null) {
                res.add(stream);
            }

            res.addAll(getFileResourceStreams(location));

            return res;
        }
    }
}
