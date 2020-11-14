package cam72cam.mod.resource;

import com.google.common.collect.Lists;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/** Pair(domain, path).  Used to reference registry entries and resource pack contents alike */
public class Identifier {
    private static Function<Identifier, List<InputStream>> multi = Data::getFileResourceStreams;

    /** MC Construct, do not use directly */
    public final net.minecraft.util.Identifier internal;

    /** Wrap MC Construct, do not use directly */
    public Identifier(net.minecraft.util.Identifier internal) {
        this.internal = internal;
    }

    /** Parse identifier from string (domain:path) */
    public Identifier(String ident) {
        this(new net.minecraft.util.Identifier(ident.toLowerCase()));
    }

    /** Standard constructor */
    public Identifier(String domain, String path) {
        this(new net.minecraft.util.Identifier(domain, path.toLowerCase()));
    }

    @Override
    public String toString() {
        return internal.toString();
    }

    public String getDomain() {
        return internal.getNamespace();
    }

    public String getPath() {
        return internal.getPath();
    }

    /**
     * Get another path within this identifier's directory
     *
     * Example:
     * this: domain:some/path/object.file
     * path: other.file
     * returns domain:some/path/other.file
     */
    public Identifier getRelative(String path) {
        return new Identifier(getDomain(), FilenameUtils.concat(FilenameUtils.getPath(getPath()), path).replace('\\', '/'));
    }

    /**
     * @return This identifier if can load, fallback if it can't
     */
    public Identifier getOrDefault(Identifier fallback){
        return this.canLoad() ? this : fallback;
    }

    /** If the resource this identifier points to exists in any form */
    public boolean canLoad() {
        try (InputStream stream = this.getResourceStream()) {
            return stream != null;
        } catch (IOException e){
            return false;
        }
    }

    /** @return all resources this identifier points at (jar first) */
    public List<InputStream> getResourceStreamAll() throws IOException {
        List<InputStream> values = multi.apply(this);
        Collections.reverse(values);
        return values;
    }

    /**
     * @return the first resource this identifier points at (mod jar, mod config zip, resource pack)
     */
    public InputStream getResourceStream() throws IOException {
        InputStream chosen = null;
        for (InputStream strm : getResourceStreamAll()) {
            if (chosen == null) {
                chosen = strm;
            } else {
                strm.close();
            }
        }
        if (chosen == null) {
            throw new java.io.FileNotFoundException(internal.toString());
        }
        return chosen;
    }

    public static void registerSupplier(Function<Identifier, List<InputStream>> multi) {
        //This could probably be done cleaner with function composition
        Function<Identifier, List<InputStream>> oldMulti = Identifier.multi;

        Identifier.multi = id -> {
            List<InputStream> values = multi.apply(id);
            values.addAll(oldMulti.apply(id));
            return values;
        };
    }
    /**
     * @return the last resource this identifier points at (mod jar, mod config zip, resource pack)
     */
    public InputStream getLastResourceStream() throws IOException {
        InputStream chosen = null;
        for (InputStream strm : Lists.reverse(getResourceStreamAll())) {
            if (chosen == null) {
                chosen = strm;
            } else {
                strm.close();
            }
        }
        if (chosen == null) {
            throw new java.io.FileNotFoundException(internal.toString());
        }
        return chosen;
    }
}
