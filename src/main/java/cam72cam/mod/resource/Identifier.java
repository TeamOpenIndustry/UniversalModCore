package cam72cam.mod.resource;

import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class Identifier {
    public final net.minecraft.util.Identifier internal;

    public Identifier(net.minecraft.util.Identifier internal) {
        this.internal = internal;
    }

    public Identifier(String ident) {
        this(new net.minecraft.util.Identifier(ident));
    }

    public Identifier(String domain, String path) {
        this(new net.minecraft.util.Identifier(domain, path));
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

    public Identifier getRelative(String path) {
        return new Identifier(getDomain(), FilenameUtils.concat(FilenameUtils.getPath(getPath()), path).replace('\\', '/'));
    }


    public List<InputStream> getResourceStreamAll() throws IOException {
        return Data.proxy.getResourceStreamAll(this);
    }

    public InputStream getResourceStream() throws IOException {
        return Data.proxy.getResourceStream(this);
    }
}
