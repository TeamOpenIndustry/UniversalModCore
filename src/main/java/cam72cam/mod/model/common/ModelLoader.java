package cam72cam.mod.model.common;

import cam72cam.mod.Config;
import cam72cam.mod.model.common.format.OBJParser;
import cam72cam.mod.model.common.format.Parser;
import cam72cam.mod.model.common.mesh.Model;
import cam72cam.mod.model.common.util.ModelCache;
import cam72cam.mod.resource.Identifier;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModelLoader {
    private static final Map<String, Parser> PARSER = new HashMap<>();

    static {
        register("obj", OBJParser::parse);
    }

    private ModelLoader() {
    }

    public static void register(String extName, Parser parser) {
        PARSER.put(extName, parser);
    }

    public static Model load(Identifier modelLoc) {
        return load(modelLoc, 1f);
    }

    public static Model load(Identifier modelLoc, float scale) {
        return load(modelLoc, scale, Collections.emptySet(), 30, null);
    }

    public static Model load(Identifier modelLoc, Collection<String> variants) {
        return load(modelLoc, 1.0F, variants, 30, null);
    }

    public static Model load(Identifier modelLoc, float scale, Collection<String> variants, int cacheSeconds, LodSupplier lod) {
        List<Integer> lodValues;
        if (lod == null) {
            lodValues = new ArrayList<>();
            lodValues.add(Config.getMaxTextureSize());
        } else {
            lodValues = lod.getValues(Config.getMaxTextureSize());
        }

        String extName = FilenameUtils.getExtension(modelLoc.getPath());
        Parser parser = PARSER.get(extName);
        if (parser == null) {
            throw new RuntimeException("Unknown model format: " + extName);
        }

        try (ModelCache cache = new ModelCache(modelLoc, scale, variants, lodValues, parser)) {
            return cache.buildModel(cacheSeconds);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load model " + modelLoc, e);
        }
    }

    @FunctionalInterface
    public interface LodSupplier {
        List<Integer> getValues(int maxSize);
    }
}
