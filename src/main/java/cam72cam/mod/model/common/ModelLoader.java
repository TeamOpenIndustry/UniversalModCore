package cam72cam.mod.model.common;

import cam72cam.mod.Config;
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

/**
 * Entry point for the UMC model framework.
 *
 * <p>To support a new model format, register a {@link Parser} with {@link #registerFormat}.
 * OBJ support self-registers via {@code cam72cam.mod.model.common.format.OBJParser}.</p>
 *
 * <p>Typical usage:</p>
 * <pre>{@code
 * Model model = ModelLoader.load(new Identifier("mymod:models/thing.obj"));
 * try (ModelRenderer.Binding binding =
 *              ModelRenderer.getRendererFor(model).bind(state, true)) {
 *     binding.draw();
 * }
 * }</pre>
 */
public class ModelLoader {
    private static final Map<String, Parser> PARSER = new HashMap<>();

    /**
     * Registers a {@link Parser} for a model file extension.
     *
     * @param extName file extension, without the leading dot (e.g. {@code "obj"})
     * @param parser  parser to invoke for files with this extension
     */
    public static void registerFormat(String extName, Parser parser){
        PARSER.put(extName, parser);
    }

    public static Model load(Identifier modelLoc) throws Exception {
        return load(modelLoc, 1f);
    }

    public static Model load(Identifier modelLoc, float scale) throws Exception {
        return load(modelLoc, scale, Collections.emptySet(), 30, null);
    }

    public static Model load(Identifier modelLoc, Collection<String> variants) throws Exception {
        return load(modelLoc, 1.0F, variants, 30, null);
    }

    /**
     * Loads a model from an Identifier, reconstructing it from the cache folder if it has
     * been loaded before with the same settings.
     *
     * @param modelLoc Location of the model file
     * @param scale Scaling factor for the model during loading
     * @param variants Texture variants for the model
     * @param cacheSeconds How long should we keep it in GPU memory after last use?
     * @param lod Lod texture resolutions provider
     * @return The constructed model
     * @throws Exception if the format is unknown or the model fails to parse/load
     */
    public static Model load(Identifier modelLoc, float scale, Collection<String> variants, int cacheSeconds, LodSupplier lod) throws Exception {
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

        try {
            ModelCache cache = new ModelCache(modelLoc, scale, variants, lodValues, parser);
            Model result = cache.buildModel(cacheSeconds);
            result.modelHash = cache.closeAndGetHash();
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FunctionalInterface
    public interface LodSupplier {
        /**
         * Define which (set of) size of textures to be generated as LoDs <br>
         * For example, if a supplier returns a singleton <code>512</code>, UMC will generate a texture set which max size is 512, alongside the default size.
         * @param maxSize The max supported texture size on the device
         * @return A list of wanted sizes to be used as texture LoDs, can be later used via {@link cam72cam.mod.render.common.ModelConfig}
         */
        List<Integer> getValues(int maxSize);
    }
}
