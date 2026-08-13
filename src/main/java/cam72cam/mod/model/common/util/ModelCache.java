package cam72cam.mod.model.common.util;

import cam72cam.mod.Config;
import cam72cam.mod.ModCore;
import cam72cam.mod.model.common.format.Parser;
import cam72cam.mod.model.common.material.TextureRepacker;
import cam72cam.mod.model.common.mesh.GlModelBuilder;
import cam72cam.mod.model.common.mesh.Model;
import cam72cam.mod.model.common.mesh.ModelGroup;
import cam72cam.mod.model.common.mesh.VAOLayout;
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

public class ModelCache implements AutoCloseable {
    private final Identifier modelLoc;
    private final List<Integer> lodValues;
    private final ResourceCache<BuiltModel> cache;
    private final TagCompound meta;

    /** Memoized build output so every converter shares a single {@link Model}. */
    private static class BuiltModel {
        final GlModelBuilder builder;
        Model model;

        BuiltModel(GlModelBuilder builder) {
            this.builder = builder;
        }

        Model model() {
            if (model == null) {
                model = builder.build(VAOLayout.POS_TEX_COLOR_NORMAL);
            }
            return model;
        }

        TextureRepacker repacker() {
            return builder.getRepacker();
        }
    }

    public ModelCache(Identifier modelLoc, float scale, Collection<String> variants, List<Integer> lodValues, Parser parser) throws IOException {
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
            // Record the model file's hash so editing the source invalidates the cache.
            // The parser itself stays cache-free and reads through the Identifier.
            provider.apply(modelLoc);
            GlModelBuilder builder = new GlModelBuilder(modelLoc, scale, variants);
            parser.parse(modelLoc, builder);
            builder.finish();
            return new BuiltModel(builder);
        });

        this.meta = new TagCompound(cache.getResource("meta.nbt", ModelCache::buildMeta).get().bytes());
    }

    private static GenericByteBuffer buildMeta(BuiltModel bm) {
        Model model = bm.model();
        TextureRepacker repacker = bm.repacker();
        TagCompound data = new TagCompound();
        data.setBoolean("hasSpecular", model.hasSpecular);
        data.setBoolean("hasNormal", model.hasNormal);
        data.setBoolean("isSmoothShading", model.isSmoothShading);
        if (Config.getMaxTextureSize() > 0) {
            data.setInteger("textureWidth", repacker.getWidth());
            data.setInteger("textureHeight", repacker.getHeight());
            data.setList("variants", new ArrayList<>(repacker.textures.keySet()), k -> new TagCompound().setString("variant", k));
        }
        data.setList("groups", new ArrayList<>(model.getGroups().values()), ModelGroup::serialize);
        try {
            return new GenericByteBuffer(data.toBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** Reconstructs the {@link Model} from the cache, linking the cached texture sheets. */
    public Model buildModel(int cacheSeconds) throws IOException {
        float[] vboData = cache.getResource("model.bin", bm -> new GenericByteBuffer(bm.model().getVboData())).get().floats();

        LinkedHashMap<String, ModelGroup> groups = new LinkedHashMap<>();
        for (ModelGroup group : meta.getList("groups", ModelGroup::deserialize)) {
            groups.put(group.name, group);
        }

        Model model = new Model(modelLoc, VAOLayout.POS_TEX_COLOR_NORMAL, vboData, groups,
                meta.getBoolean("hasSpecular"), meta.getBoolean("hasNormal"), meta.getBoolean("isSmoothShading"));

        if (Config.getMaxTextureSize() > 0) {
            model.linkTextures(
                    loadTextures("", cacheSeconds),
                    model.hasSpecular ? loadTextures("_spec", cacheSeconds) : Collections.emptyMap(),
                    model.hasNormal ? loadTextures("_norm", cacheSeconds) : Collections.emptyMap()
            );
        }
        return model;
    }

    private Map<String, Map<Integer, OBJTextureSheet>> loadTextures(String suffix, int cacheSeconds) throws IOException {
        Map<String, Map<Integer, OBJTextureSheet>> result = new HashMap<>();
        if (!meta.hasKey("textureWidth")) {
            return result;
        }
        int textureWidth = meta.getInteger("textureWidth");
        int textureHeight = meta.getInteger("textureHeight");
        int texSize = Math.max(textureWidth, textureHeight);

        for (String variant : meta.getList("variants", k -> k.getString("variant"))) {
            String base = variant + suffix;
            Map<Integer, OBJTextureSheet> lodMap = new HashMap<>();
            lodMap.put(texSize, new OBJTextureSheet(textureWidth, textureHeight,
                    cache.getResource(base + ".rgba", bm -> textureBytes(bm, variant, suffix, null)),
                    cacheSeconds));
            for (Integer lodValue : lodValues) {
                if (lodValue < texSize) {
                    Pair<Integer, Integer> size = scaleSize(textureWidth, textureHeight, lodValue);
                    lodMap.put(lodValue, new OBJTextureSheet(size.getLeft(), size.getRight(),
                            cache.getResource(base + "_" + lodValue + ".rgba", bm -> textureBytes(bm, variant, suffix, lodValue)),
                            cacheSeconds));
                }
            }
            result.put(variant, lodMap);
        }
        return result;
    }

    /** Generates (on cache miss) the RGBA bytes for a texture sheet. {@code lod} is null for full-size. */
    private GenericByteBuffer textureBytes(BuiltModel bm, String variant, String suffix, Integer lod) {
        Supplier<BufferedImage> source = textureSource(bm, variant, suffix);
        BufferedImage img = lod != null ? scaleImage(source.get(), lod) : source.get();
        if (Config.DebugTextureSheets && lod == null) {
            try {
                File cacheFile = ModCore.cacheFile(new Identifier(modelLoc.getDomain() + "debug",
                        modelLoc.getPath() + "_" + variant + suffix + ".png"));
                ModCore.info("Writing debug to " + cacheFile);
                ImageIO.write(img, "png", cacheFile);
            } catch (IOException e) {
                ModCore.catching(e);
            }
        }
        return new GenericByteBuffer(toRGBA(img));
    }

    private static Supplier<BufferedImage> textureSource(BuiltModel bm, String variant, String suffix) {
        TextureRepacker repacker = bm.repacker();
        switch (suffix) {
            case "":
                return repacker.textures.get(variant);
            case "_spec":
                return repacker.speculars.get(variant);
            case "_norm":
                return repacker.normals.get(variant);
            default:
                throw new IllegalArgumentException("Unknown texture suffix: " + suffix);
        }
    }

    @Override
    public void close() {
        try {
            cache.close();
        } catch (IOException e) {
            ModCore.catching(e);
        }
    }
}
