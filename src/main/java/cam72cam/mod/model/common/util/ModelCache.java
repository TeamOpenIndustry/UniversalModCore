package cam72cam.mod.model.common.util;

import cam72cam.mod.Config;
import cam72cam.mod.ModCore;
import cam72cam.mod.model.common.format.Parser;
import cam72cam.mod.model.common.material.TextureRepacker;
import cam72cam.mod.model.common.mesh.*;
import cam72cam.mod.render.obj.OBJTextureSheet;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.serialization.ResourceCache;
import cam72cam.mod.serialization.ResourceCache.GenericByteBuffer;
import cam72cam.mod.serialization.TagCompound;
import org.apache.commons.lang3.tuple.Pair;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static cam72cam.mod.model.common.util.ImageUtils.*;

/**
 * Helper class to load a model from cache, or rebuild the cache if invalid<br>
 * Internal, don't use directly!
 */
public class ModelCache implements AutoCloseable {
    private final Identifier modelLoc;
    private final List<Integer> lodValues;
    private final ResourceCache<SimpleModelBuilder> cache;
    private final TagCompound meta;

    public ModelCache(Identifier modelLoc, double scale, Collection<String> variants, List<Integer> lodValues, Parser parser) throws IOException {
        this.modelLoc = modelLoc;
        this.lodValues = lodValues != null ? lodValues : Collections.emptyList();

        String settings = Arrays.toString(new Object[]{
                "v2",
                scale,
                variants == null || variants.isEmpty() ? "[]" : String.join(":", variants),
                this.lodValues.stream().map(Object::toString).collect(Collectors.joining("-"))
        });
        if (Config.DebugTextureSheets) {
            settings += "-debug";
        }
        Identifier cacheId = new Identifier(modelLoc.getDomain(), modelLoc.getPath() + "_" + settings.hashCode());

        this.cache = new ResourceCache<>(cacheId, provider -> {
            SimpleModelBuilder builder = new SimpleModelBuilder(modelLoc, scale, variants, provider);
            parser.parse(builder);
            builder.finish();
            return builder;
        });

        // Meta is read eagerly so the Model can be reconstructed on a cache hit without reparsing.
        this.meta = new TagCompound(cache.getResource("meta.nbt", builder -> {
            TextureRepacker repacker = builder.getRepacker();
            TagCompound meta = new TagCompound()
                    .setBoolean("hasSpecular", repacker.hasSpecular())
                    .setBoolean("hasNormal", repacker.hasNormal())
                    .setBoolean("isSmoothShading", builder.isSmoothShading())
                    // Fixed to old VBO type for now, TODO Extension
                    .set("layout", builder.hasNormal() ? VAOLayout.POS_TEX_COLOR_NORMAL.serialize() : VAOLayout.POS_TEX_COLOR.serialize());
            if (Config.getMaxTextureSize() > 0) {
                meta.setInteger("packedTextureWidth", repacker.getWidth())
                    .setInteger("packedTextureHeight", repacker.getHeight())
                    .setList("variants", new ArrayList<>(repacker.textures.keySet()), k -> new TagCompound().setString("variant", k));
            } else {
                meta.setInteger("packedTextureWidth", -1)
                    .setInteger("packedTextureHeight", -1)
                    .setList("variants", Collections.emptyList(), k -> new TagCompound().setString("variant", ""));
            }
            meta.setList("groups", new ArrayList<>(builder.validGroups()), ModelGroup::serialize);
            try {
                return new GenericByteBuffer(meta.toBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).get().bytes());
    }

    /**
     * Reconstructs the {@link Model} from the cache and links the cached texture sheets.
     *
     * @param cacheSeconds how long to keep the texture sheets in GPU memory after last use
     * @return the loaded model
     */
    public Model buildModel(int cacheSeconds) throws IOException {
        Supplier<GenericByteBuffer> vboData = cache.getResource("model.bin", builder -> new GenericByteBuffer(builder.build().getVboData()));

        LinkedHashMap<String, ModelGroup> groups = new LinkedHashMap<>();
        for (ModelGroup group : meta.getList("groups", ModelGroup::deserialize)) {
            groups.put(group.name, group);
        }
        VAOLayout layout = VAOLayout.deserialize(meta.get("layout"));

        Model model = new Model(modelLoc, layout, () -> vboData.get().floats(), groups,
                                meta.getBoolean("hasSpecular"),
                                meta.getBoolean("hasNormal"),
                                meta.getBoolean("isSmoothShading"),
                                meta.getInteger("packedTextureWidth"),
                                meta.getInteger("packedTextureHeight"));

        if (Config.getMaxTextureSize() > 0) {
            model.linkTextures(
                    loadTextures("", cacheSeconds),
                    model.hasSpecular ? loadTextures("_spec", cacheSeconds) : Collections.emptyMap(),
                    model.hasNormal ? loadTextures("_norm", cacheSeconds) : Collections.emptyMap()
            );
        }
        return model;
    }

    private Map<String, NavigableMap<Integer, OBJTextureSheet>> loadTextures(String suffix, int cacheSeconds) throws IOException {
        Map<String, NavigableMap<Integer, OBJTextureSheet>> result = new HashMap<>();
        if (!meta.hasKey("packedTextureWidth")) {
            return result;
        }
        int textureWidth = meta.getInteger("packedTextureWidth");
        int textureHeight = meta.getInteger("packedTextureHeight");
        int texSize = Math.max(textureWidth, textureHeight);
        for (String variant : meta.getList("variants", k -> k.getString("variant"))) {
            // TreeMap keeps LOD sizes sorted so ModelConfig can use floorEntry/lastEntry
            NavigableMap<Integer, OBJTextureSheet> lodMap = new TreeMap<>();
            lodMap.put(texSize, new OBJTextureSheet(textureWidth, textureHeight,
                                                    cache.getResource(variant + suffix + ".rgba", bm -> getTextureBytes(bm, variant, suffix, null)),
                                                    cacheSeconds));
            for (Integer lodValue : lodValues) {
                if (lodValue < texSize) {
                    Pair<Integer, Integer> size = scaleSize(textureWidth, textureHeight, lodValue);
                    lodMap.put(lodValue, new OBJTextureSheet(size.getLeft(), size.getRight(),
                            cache.getResource(variant + "_" + lodValue + suffix + ".rgba", bm -> getTextureBytes(bm, variant, suffix, lodValue)),
                            cacheSeconds));
                }
            }
            result.put(variant, lodMap);
        }
        return result;
    }

    /** Generates (on cache miss) the RGBA bytes for a texture sheet; {@code lod} is null for the full-size sheet. */
    private GenericByteBuffer getTextureBytes(IModelBuilder builder, String variant, String suffix, Integer lod) {
        TextureRepacker repacker = builder.getRepacker();
        Supplier<BufferedImage> source;
        switch (suffix) {
            default:
                // Not normal PBR, use base color
            case "":
                source = repacker.textures.get(variant);
                break;
            case "_spec":
                source = repacker.speculars.get(variant);
                break;
            case "_norm":
                source = repacker.normals.get(variant);
        }
        BufferedImage img = lod != null ? scaleImage(source.get(), lod) : source.get();
        if (Config.DebugTextureSheets && lod == null) {
            try {
                File cacheFile = ModCore.cacheFile(new Identifier(modelLoc.getDomain() + "debug", modelLoc.getPath() + "_" + variant + suffix + ".png"));
                ModCore.info("Writing debug to " + cacheFile);
                ImageIO.write(img, "png", cacheFile);
            } catch (IOException e) {
                ModCore.catching(e);
            }
        }
        return new GenericByteBuffer(toRGBA(img));
    }

    @Override
    public void close() {
        try {
            cache.close();
        } catch (IOException e) {
            ModCore.catching(e);
        }
    }

    public String closeAndGetHash() {
        try {
            return cache.close();
        } catch (IOException e) {
            ModCore.catching(e);
        }
        return null;
    }
}
