package cam72cam.mod.model.common;

import cam72cam.mod.Config;
import cam72cam.mod.ModCore;
import cam72cam.mod.model.common.format.OBJParser;
import cam72cam.mod.model.common.format.Parser;
import cam72cam.mod.model.common.material.TextureRepacker;
import cam72cam.mod.model.common.mesh.GlModelBuilder;
import cam72cam.mod.model.common.mesh.Model;
import cam72cam.mod.model.common.mesh.VAOLayout;
import cam72cam.mod.render.obj.OBJTextureSheet;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.serialization.ResourceCache;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;

import static cam72cam.mod.model.common.util.ImageUtils.*;

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
        if (!PARSER.containsKey(extName)) {
            throw new RuntimeException("Unknown model format: " + extName);
        }
        GlModelBuilder builder = new GlModelBuilder(modelLoc, scale, variants);
        PARSER.get(extName).parse(modelLoc, builder);
        builder.finish();
        Model result = builder.build(VAOLayout.POS_TEX_COLOR_NORMAL);
        TextureRepacker repacker = builder.getRepacker();
        if (repacker != null && Config.getMaxTextureSize() > 0) {
            result.linkTextures(processLod(modelLoc, repacker, repacker.textures, cacheSeconds, lodValues, ""),
                                result.hasSpecular
                                    ? processLod(modelLoc, repacker, repacker.speculars, cacheSeconds, lodValues, "_spec")
                                    : Collections.emptyMap(),
                                result.hasNormal
                                    ? processLod(modelLoc, repacker, repacker.normals, cacheSeconds, lodValues, "_norm")
                                    : Collections.emptyMap());
        }
        return result;
    }

    private static Map<String, Map<Integer, OBJTextureSheet>> processLod(
            Identifier model, TextureRepacker repacker, Map<String, Supplier<BufferedImage>> source,
            int cacheSeconds, List<Integer> lodValues, String debugSuffix) {
        Map<String, Map<Integer, OBJTextureSheet>> result = new HashMap<>();
        if (source.isEmpty()) {
            return result;
        }

        int textureWidth = repacker.getWidth();
        int textureHeight = repacker.getHeight();
        int texSize = Math.max(textureWidth, textureHeight);

        for (String variant : source.keySet()) {
            Supplier<BufferedImage> image = source.get(variant);
            Map<Integer, OBJTextureSheet> lodMap = new HashMap<>();
            lodMap.put(texSize, new OBJTextureSheet(textureWidth, textureHeight,
                    () -> {
                        BufferedImage full = image.get();
                        if (Config.DebugTextureSheets) {
                            try {
                                File cacheFile = ModCore.cacheFile(new Identifier(model.getDomain() + "debug", model.getPath() + "_" + variant + debugSuffix + ".png"));
                                ModCore.info("Writing debug to " + cacheFile);
                                ImageIO.write(full, "png", cacheFile);
                            } catch (IOException e) {
                                ModCore.catching(e);
                            }
                        }
                        return new ResourceCache.GenericByteBuffer(toRGBA(full));
                    }, cacheSeconds));
            for (Integer lodValue : lodValues) {
                if (lodValue < texSize) {
                    Pair<Integer, Integer> size = scaleSize(textureWidth, textureHeight, lodValue);
                    lodMap.put(lodValue, new OBJTextureSheet(size.getLeft(), size.getRight(),
                            () -> new ResourceCache.GenericByteBuffer(toRGBA(scaleImage(image.get(), lodValue))), cacheSeconds));
                }
            }
            result.put(variant, lodMap);
        }
        return result;
    }

    @FunctionalInterface
    public interface LodSupplier {
        List<Integer> getValues(int maxSize);
    }
}
