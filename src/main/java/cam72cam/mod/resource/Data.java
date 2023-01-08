package cam72cam.mod.resource;

import cam72cam.mod.ModCore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.SidedProxy;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Internal, do not use directly */
class Data {
    @SidedProxy(clientSide = "cam72cam.mod.resource.Data$ClientProxy", serverSide = "cam72cam.mod.resource.Data$ServerProxy", modId = ModCore.MODID)
    public static DataProxy proxy;

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
                configDir = Loader.instance().getConfigDir().toString();
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
                            streams.add(new Identifier.InputStreamMod(new ByteArrayInputStream(IOUtils.toByteArray(stream)), file.lastModified()));
                        }
                        resourcePack.close();
                    }
                    File[] folders = folder.listFiles((dir, name) -> true);
                    for (File dir : folders) {
                        if (dir.isDirectory()) {
                            File path = Paths.get(dir.getPath(), pathString(location, false)).toFile();
                            if (path.exists()) {
                                streams.add(new Identifier.InputStreamMod(new FileInputStream(path), path.lastModified()));
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

    public static List<InputStream> unwrapResources(List<InputStream> in) {
        try {
            List<InputStream> out = new ArrayList<>();
            for (InputStream stream : in) {
                if (stream instanceof InflaterInputStream) {
                    // DOES NOT WORK PAST JAVA 8!!!!
                    Field zfsField = ((InflaterInputStream) stream).getClass().getDeclaredField("this$0");
                    zfsField.setAccessible(true);

                    Field modifiersField = Field.class.getDeclaredField("modifiers");
                    modifiersField.setAccessible(true);
                    modifiersField.setInt(zfsField, zfsField.getModifiers() & ~Modifier.FINAL);

                    Object zfs = zfsField.get(stream);
                    Method gzf = zfs.getClass().getDeclaredMethod("getZipFile");
                    gzf.setAccessible(true);
                    Path p = (Path) gzf.invoke(zfs);
                    out.add(new Identifier.InputStreamMod(stream, p.toFile().lastModified()));
                } else {
                    out.add(stream);
                }
            }
            return out;
        } catch (Exception ex) {
            return in;
        }
    }

    public static class ClientProxy extends DataProxy {
        @Override
        public List<InputStream> getResourceStreamAll(Identifier identifier) throws IOException {
            List<InputStream> res = new ArrayList<>();
            try {
                for (IResource resource : ((List<IResource>)Minecraft.getMinecraft().getResourceManager().getAllResources(identifier.internal))) {
                    res.add(resource.getInputStream());
                }
            } catch (java.io.FileNotFoundException ex) {
                // Ignore
            }
            res.addAll(getFileResourceStreams(identifier));
            return unwrapResources(res);
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

            return unwrapResources(res);
        }
    }
}
