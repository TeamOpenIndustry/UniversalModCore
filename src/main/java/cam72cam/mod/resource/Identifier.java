package cam72cam.mod.resource;

import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class Identifier {
    private static Function<Identifier, List<InputStream>> multi = Data::getFileResourceStreams;

    public final net.minecraft.util.Identifier internal;

    public Identifier(net.minecraft.util.Identifier internal) {
        this.internal = internal;
    }

    public Identifier(String ident) {
        this(new net.minecraft.util.Identifier(ident.toLowerCase()));
    }

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

    public Identifier getRelative(String path) {
        return new Identifier(getDomain(), FilenameUtils.concat(FilenameUtils.getPath(getPath()), path).replace('\\', '/'));
    }

    public Identifier getOrDefault(Identifier fallback){
        return this.canLoad() ? this : fallback;
    }

    public boolean canLoad() {
        try (InputStream stream = this.getResourceStream()) {
            return stream != null;
        } catch (IOException e){
            return false;
        }
    }


    public List<InputStream> getResourceStreamAll() throws IOException {
        List<InputStream> values = multi.apply(this);
        Collections.reverse(values);
        return values;
    }

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
}
