package cam72cam.mod.resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.FilenameUtils;

import net.minecraft.util.ResourceLocation;

/** Pair(domain, path).  Used to reference registry entries and resource pack contents alike */
public class Identifier {
    /** MC Construct, do not use directly */
    public final ResourceLocation internal;

    /** Wrap MC Construct, do not use directly */
    public Identifier(ResourceLocation internal) {
        this.internal = internal;
    }

    /** Parse identifier from string (domain:path) */
    public Identifier(String ident) {
        this(new ResourceLocation(ident.toLowerCase()));
    }

    /** Standard constructor */
    public Identifier(String domain, String path) {
        this(new ResourceLocation(domain.toLowerCase(), path.toLowerCase()));
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
        return Data.proxy.getResourceStreamAll(this);
    }

    /**
     * @return the first resource this identifier points at (mod jar, mod config zip, resource pack)
     */
    public InputStream getResourceStream() throws IOException {
        return Data.proxy.getResourceStream(this);
    }
    /**
     * @return the last resource this identifier points at (mod jar, mod config zip, resource pack)
     */
    public InputStream getLastResourceStream() throws IOException {
        return Data.proxy.getResourceStream(this);
    }
}
