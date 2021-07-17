package cam72cam.mod.resource;

import cam72cam.mod.ModCore;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Internal, do not use directly */
public class Data {
    public static String pathString(Identifier location, boolean startingSlash) {
        return (startingSlash ? "/" : "") + "assets/" + location.getDomain() + "/" + location.getPath();
    }

    static List<InputStream> getFileResourceStreams(Identifier location) {
        try {
            return getFileResourceStreamsThrower(location);
        } catch (IOException ex) {
            ModCore.catching(ex);
            return Collections.emptyList();
        }
    }

    static List<InputStream> getFileResourceStreamsThrower(Identifier location) throws IOException {
        List<InputStream> streams = new ArrayList<>();

        File configDir = FabricLoader.getInstance().getConfigDir().toFile();
        configDir.mkdir();

        File folder = new File(configDir + File.separator + location.getDomain());
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
            }
        } else {
            folder.mkdirs();
        }
        return streams;
    }
}
