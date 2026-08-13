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

public class ModelCache implements AutoCloseable {
    private final Identifier modelLoc;
    private final List<Integer> lodValues;
    private final ResourceCache<GlModelBuilder> cache;
    private final TagCompound meta;

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
            GlModelBuilder builder = new GlModelBuilder(modelLoc, scale, variants, provider);
            parser.parse(builder);
            builder.finish();
            return builder;
        });

        // Meta is read eagerly so the Model can be reconstructed on a cache hit without re-parsing.
        this.meta = new TagCompound(cache.getResource("meta.nbt", bm -> {
            // Fixed for now, TODO Extension
            Model model = bm.build(VAOLayout.POS_TEX_COLOR_NORMAL);
            TextureRepacker repacker = bm.getRepacker();
            TagCompound data = new TagCompound();
            data.setBoolean("hasSpecular", model.hasSpecular);
            data.setBoolean("hasNormal", model.hasNormal);
            data.setBoolean("isSmoothShading", model.isSmoothShading);
            data.set("layout", VAOLayout.POS_TEX_COLOR_NORMAL.serialize());
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
        }).get().bytes());
    }

    /** Reconstructs the {@link Model} from the cache, linking the cached texture sheets. */
    public Model buildModel(int cacheSeconds) throws IOException {
        float[] vboData = cache.getResource("model.bin", bm -> new GenericByteBuffer(bm.build(VAOLayout.POS_TEX_COLOR_NORMAL).getVboData())).get().floats();

        LinkedHashMap<String, ModelGroup> groups = new LinkedHashMap<>();
        for (ModelGroup group : meta.getList("groups", ModelGroup::deserialize)) {
            groups.put(group.name, group);
        }
        VAOLayout layout = VAOLayout.deserialize(meta.get("layout"));

        Model model = new Model(modelLoc, layout, vboData, groups,
                                meta.getBoolean("hasSpecular"),
                                meta.getBoolean("hasNormal"),
                                meta.getBoolean("isSmoothShading"));

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

        String ext = suffix.isEmpty() ? "rgba" : suffix.substring(1);

        for (String variant : meta.getList("variants", k -> k.getString("variant"))) {
            Map<Integer, OBJTextureSheet> lodMap = new HashMap<>();
            lodMap.put(texSize, new OBJTextureSheet(textureWidth, textureHeight,
                    cache.getResource(variant + "." + ext, bm -> textureBytes(bm, variant, suffix, null)),
                    cacheSeconds));
            for (Integer lodValue : lodValues) {
                if (lodValue < texSize) {
                    Pair<Integer, Integer> size = scaleSize(textureWidth, textureHeight, lodValue);
                    lodMap.put(lodValue, new OBJTextureSheet(size.getLeft(), size.getRight(),
                            cache.getResource(variant + "_" + lodValue + "." + ext, bm -> textureBytes(bm, variant, suffix, lodValue)),
                            cacheSeconds));
                }
            }
            result.put(variant, lodMap);
        }
        return result;
    }

    /** Generates (on cache miss) the RGBA bytes for a texture sheet; {@code lod} is null for the full-size sheet. */
    private GenericByteBuffer textureBytes(GlModelBuilder bm, String variant, String suffix, Integer lod) {
        TextureRepacker repacker = bm.getRepacker();
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
}
