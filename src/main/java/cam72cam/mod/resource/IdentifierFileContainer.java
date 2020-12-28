package cam72cam.mod.resource;

import friedrichlp.renderlib.caching.serialization.Serializer;
import friedrichlp.renderlib.util.IFileContainer;

import java.io.*;

public class IdentifierFileContainer implements IFileContainer {
    private Identifier loc;

    // Used for IFileContainer reflection
    public IdentifierFileContainer() {}

    public IdentifierFileContainer(Identifier loc) {
        this.loc = loc;
    }

    public IdentifierFileContainer(String loc) {
        this.loc = new Identifier(loc);
    }

    @Override
    public InputStream getStream() throws IOException {
        return loc.getResourceStream();
    }

    @Override
    public IFileContainer getRelative(String path) {
        return new IdentifierFileContainer(loc.getRelative(path));
    }

    @Override
    public String getPath() {
        return loc.getPath();
    }

    @Override
    public String getName() {
        return new File(loc.getPath()).getName();
    }

    @Override
    public void save(Serializer.Out s) throws IOException {
        s.writeStr(loc.toString());
    }

    @Override
    public void load(Serializer.In s) throws IOException {
        loc = new Identifier(s.readStr());
    }
}
