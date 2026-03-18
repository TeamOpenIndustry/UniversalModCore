package cam72cam.mod.model.common;

import cam72cam.mod.model.common.parser.IModelParser;
import cam72cam.mod.model.common.parser.OBJParser;
import cam72cam.mod.model.common.texture.TextureRepacker;
import cam72cam.mod.resource.Identifier;

import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;

public final class ModelLoader {
    private static final Map<String, Supplier<IModelParser>> PARSERS = new HashMap<>();

    static {
        registerParser("obj", OBJParser::new);
    }

    public static void registerParser(String extension, Supplier<IModelParser> factory) {
        PARSERS.put(extension.toLowerCase(), factory);
    }

    public static Model load(Identifier location, String ext) throws IOException {
        return (Model) load(location, ext, Collections.EMPTY_LIST).get(0);
    }

    public static List<Model> load(Identifier location, String ext, List<String> relativePaths) throws IOException {
        Supplier<IModelParser> factory = PARSERS.get(ext);
        if (factory == null) throw new IllegalArgumentException("No parser for ." + ext);
        IModelParser parser = factory.get();
        Model baseModel = parser.parse(location);
        return TextureRepacker.repack(baseModel, relativePaths);
    }
}