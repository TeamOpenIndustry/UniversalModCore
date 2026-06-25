package cam72cam.umc.api.model.obj;

import cam72cam.umc.api.Config;
import cam72cam.umc.api.ModCore;
import cam72cam.umc.api.math.Vec3d;
import cam72cam.umc.api.render.obj.OBJTextureSheet;
import cam72cam.umc.api.render.obj.OBJRender;
import cam72cam.umc.api.render.opengl.CustomTexture;
import cam72cam.umc.api.render.opengl.RenderState;
import cam72cam.umc.api.resource.Identifier;
import cam72cam.umc.api.serialization.ResourceCache;
import cam72cam.umc.api.serialization.ResourceCache.GenericByteBuffer;
import cam72cam.umc.api.serialization.TagCompound;
import org.apache.commons.lang3.tuple.Pair;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static cam72cam.umc.api.model.obj.ImageUtils.*;

public class OBJModel {
    private static final OBJTextureSheet defTex = new OBJTextureSheet(1, 1, () -> new ResourceCache.GenericByteBuffer(new int[] { 0x0000FF }), Integer.MAX_VALUE/2);
    public final OBJRender vbo;
    public final int textureWidth;
    public final int textureHeight;
    public final int defaultLodSize;
    public final Map<String, Map<Integer, OBJTextureSheet>> textures = new HashMap<>();
    public final Map<String, Map<Integer, OBJTextureSheet>> normals = new HashMap<>();
    public final Map<String, Map<Integer, OBJTextureSheet>> speculars = new HashMap<>();
    public final LinkedHashMap<String, OBJGroup> groups; //Order by vertex start/stop
    public final boolean isSmoothShading;

    public String hash;

    public OBJModel(Identifier modelLoc, float darken) throws Exception {
        this(modelLoc, darken, 1, null, 30, null);
    }

    public OBJModel(Identifier modelLoc, float darken, Collection<String> variants) throws Exception {
        this(modelLoc, darken, 1, variants, 30, null);
    }

    public OBJModel(Identifier modelLoc, float darken, double scale) throws Exception {
        this(modelLoc, darken, scale, null, 30, null);
    }

    public OBJModel(Identifier modelLoc, float darken, double scale, Collection<String> variants, int cacheSeconds, Function<Integer, List<Integer>> lodSizes) throws Exception {
        ModCore.debug("Start obj model " + modelLoc);
        List<Integer> lodValues;
        if (lodSizes != null) {
            lodValues = lodSizes.apply(Config.getMaxTextureSize());
        } else {
            lodValues = new ArrayList<>();
            lodValues.add(Config.getMaxTextureSize());
        }

        String settings = Arrays.toString(new Object[]{
                "v1",
                scale,
                darken,
                variants == null ? "[]" : String.join(":", variants),
                lodValues.stream().map(Object::toString).collect(Collectors.joining("-"))
        });
        if (Config.DebugTextureSheets) {
            settings += "-debug";
        }
        ResourceCache<OBJBuilder> cache = new ResourceCache<>(
                new Identifier(modelLoc.getDomain(), modelLoc.getPath() + "_" + settings.hashCode()),
                provider -> new OBJBuilder(modelLoc, provider, (float)scale, darken, variants)
        );

        Supplier<GenericByteBuffer> vboData = cache.getResource(
                "model.bin",
                builder -> new GenericByteBuffer(builder.vertexBufferObject().data)
        );
        TagCompound meta = new TagCompound(cache.getResource(
                "meta.nbt",
                builder -> {
                    TagCompound data = new TagCompound();
                    data.setBoolean("hasVertexNormals", builder.vertexBufferObject().hasNormals);
                    data.setBoolean("isSmoothShading", builder.isSmoothShading());
                    if (Config.getMaxTextureSize() > 0) {
                        data.setInteger("textureWidth", builder.getTextureWidth());
                        data.setInteger("textureHeight", builder.getTextureHeight());
                        data.setList("variants", new ArrayList<>(builder.getTextures().keySet()), k -> new TagCompound().setString("variant", k));
                    }
                    data.setBoolean("hasNormals", !builder.getNormals().isEmpty());
                    data.setBoolean("hasSpeculars", !builder.getSpeculars().isEmpty());
                    data.setList("groups", builder.getGroups(), OBJGroup::toTag);
                    try {
                        return new GenericByteBuffer(data.toBytes());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
        ).get().bytes());

        boolean hasVertexNormals = meta.getBoolean("hasVertexNormals");
        this.isSmoothShading = meta.hasKey("isSmoothShading") && meta.getBoolean("isSmoothShading");
        if (Config.getMaxTextureSize() > 0) {
            this.textureWidth = meta.getInteger("textureWidth");
            this.textureHeight = meta.getInteger("textureHeight");

            boolean hasNormals = meta.getBoolean("hasNormals");
            boolean hasSpeculars = meta.getBoolean("hasSpeculars");


            for (String variant : meta.getList("variants", k -> k.getString("variant"))) {
                ModCore.debug("%s : tex %s", modelLoc, variant);
                Map<Integer, OBJTextureSheet> baseTexLodMap = new HashMap<>();

                int texSize = Math.max(textureWidth, textureHeight);
                Supplier<GenericByteBuffer> texData = cache.getResource(variant + ".rgba", builder -> {
                    BufferedImage img = builder.getTextures().get(variant).get();
                    if (Config.DebugTextureSheets) {
                        try {
                            File cacheFile = ModCore.cacheFile(new Identifier(modelLoc.getDomain() + "debug", modelLoc.getPath() + "_" + variant + ".png"));
                            ModCore.info("Writing debug to " + cacheFile);
                            ImageIO.write(img, "png", cacheFile);
                        } catch (IOException e) {
                            ModCore.catching(e);
                        }
                    }
                    return new GenericByteBuffer(toRGBA(img));
                });
                baseTexLodMap.put(texSize, new OBJTextureSheet(textureWidth, textureHeight, texData, cacheSeconds));

                for (Integer lodValue : lodValues) {
                    if (lodValue < texSize) {
                        Pair<Integer, Integer> size = scaleSize(textureWidth, textureHeight, lodValue);
                        Supplier<GenericByteBuffer> lodData = cache.getResource(variant + String.format("_%s.rgba", lodValue),
                                builder -> new GenericByteBuffer(toRGBA(scaleImage(builder.getTextures().get(variant).get(), lodValue)))
                        );
                        baseTexLodMap.put(lodValue, new OBJTextureSheet(size.getLeft(), size.getRight(), lodData, cacheSeconds));
                    }
                }
                this.textures.put(variant, baseTexLodMap);

                if (hasNormals) {
                    Map<Integer, OBJTextureSheet> normalTexLodMap = new HashMap<>();
                    try {
                        Supplier<GenericByteBuffer> normData = cache.getResource(variant + ".norm", builder -> {
                            BufferedImage img = builder.getNormals().get(variant).get();
                            if (Config.DebugTextureSheets) {
                                try {
                                    File cacheFile = ModCore.cacheFile(new Identifier(modelLoc.getDomain() + "debug", modelLoc.getPath() + "_" + variant + "_norm.png"));
                                    ModCore.info("Writing debug normal to " + cacheFile);
                                    ImageIO.write(img, "png", cacheFile);
                                } catch (IOException e) {
                                    ModCore.catching(e);
                                }
                            }
                            return new GenericByteBuffer(toRGBA(img));
                        });
                        normalTexLodMap.put(texSize, new OBJTextureSheet(textureWidth, textureHeight, normData, cacheSeconds));

                        for (Integer lodValue : lodValues) {
                            if (lodValue < texSize) {
                                Pair<Integer, Integer> size = scaleSize(textureWidth, textureHeight, lodValue);
                                Supplier<GenericByteBuffer> lodNormData = cache.getResource(variant + "_" + lodValue + ".norm",
                                        builder -> new GenericByteBuffer(toRGBA(scaleImage(builder.getNormals().get(variant).get(), lodValue))));
                                normalTexLodMap.put(lodValue, new OBJTextureSheet(size.getLeft(), size.getRight(), lodNormData, cacheSeconds));
                            }
                        }
                        this.normals.put(variant, normalTexLodMap);
                    } catch (Exception ex) {
                        ModCore.warn("Unable to load normal map for %s, %s", modelLoc, ex);
                    }
                }

                if (hasSpeculars) {
                    Map<Integer, OBJTextureSheet> specularTexLodMap = new HashMap<>();
                    try {
                        Supplier<GenericByteBuffer> specData = cache.getResource(variant + ".spec", builder -> {
                            BufferedImage img = builder.getSpeculars().get(variant).get();
                            if (Config.DebugTextureSheets) {
                                try {
                                    File cacheFile = ModCore.cacheFile(new Identifier(modelLoc.getDomain() + "debug", modelLoc.getPath() + "_" + variant + "_spec.png"));
                                    ModCore.info("Writing debug specular to " + cacheFile);
                                    ImageIO.write(img, "png", cacheFile);
                                } catch (IOException e) {
                                    ModCore.catching(e);
                                }
                            }
                            return new GenericByteBuffer(toRGBA(img));
                        });
                        specularTexLodMap.put(texSize, new OBJTextureSheet(textureWidth, textureHeight, specData, cacheSeconds));

                        for (Integer lodValue : lodValues) {
                            if (lodValue < texSize) {
                                Pair<Integer, Integer> size = scaleSize(textureWidth, textureHeight, lodValue);
                                Supplier<GenericByteBuffer> lodSpecData = cache.getResource(variant + "_" + lodValue + ".spec",
                                        builder -> new GenericByteBuffer(toRGBA(scaleImage(builder.getSpeculars().get(variant).get(), lodValue))));
                                specularTexLodMap.put(lodValue, new OBJTextureSheet(size.getLeft(), size.getRight(), lodSpecData, cacheSeconds));
                            }
                        }
                        this.speculars.put(variant, specularTexLodMap);
                    } catch (Exception ex) {
                        ModCore.warn("Unable to load specular map for %s, %s", modelLoc, ex);
                    }
                }
            }
            defaultLodSize = textures.get("").keySet().stream().mapToInt(i -> i).max().getAsInt();
        } else {
            defaultLodSize = -1;
            this.textureWidth = -1;
            this.textureHeight = -1;
        }

        this.groups = meta.getList("groups", OBJGroup::new).stream().collect(Collectors.toMap(k -> k.name, v -> v, (x, y) -> y, LinkedHashMap::new));

        this.vbo = new OBJRender(this, () -> new VertexBuffer(vboData.get().floats(), hasVertexNormals));

        this.hash = cache.close();

        ModCore.debug("End obj model " + modelLoc);
    }

    public Set<String> groups() {
        return groups.keySet();
    }

    public FaceAccessor getFaceAccessor() {
        return new FaceAccessor(this);
    }

    public Vec3d minOfGroup(Iterable<String> groupNames) {
        Vec3d min = null;
        for (String group : groupNames) {
            Vec3d gmin = groups.get(group).min;
            if (min == null) {
                min = gmin;
            } else {
                min = min.min(gmin);
            }
        }
        return min;
    }

    public Vec3d maxOfGroup(Iterable<String> groupNames) {
        Vec3d max = null;
        for (String group : groupNames) {
            Vec3d gmax = groups.get(group).max;
            if (max == null) {
                max = gmax;
            } else {
                max = max.max(gmax);
            }
        }
        return max;
    }

    public Vec3d centerOfGroups(Iterable<String> groupNames) {
        Vec3d min = minOfGroup(groupNames);
        Vec3d max = maxOfGroup(groupNames);
        return new Vec3d((min.x + max.x) / 2, (min.y + max.y) / 2, (min.z + max.z) / 2);
    }

    public double heightOfGroups(Iterable<String> groupNames) {
        Vec3d min = minOfGroup(groupNames);
        Vec3d max = maxOfGroup(groupNames);
        return max.y - min.y;
    }

    public double lengthOfGroups(Iterable<String> groupNames) {
        Vec3d min = minOfGroup(groupNames);
        Vec3d max = maxOfGroup(groupNames);
        return max.x - min.x;
    }

    public double widthOfGroups(Iterable<String> groupNames) {
        Vec3d min = minOfGroup(groupNames);
        Vec3d max = maxOfGroup(groupNames);
        return max.z - min.z;
    }

    /** WARNING This is a very slow function and should be used for debug only */
    public List<Vec3d> points(OBJGroup group) {
        List<Vec3d> points = new ArrayList<>();
        VertexBuffer vbo = this.vbo.buffer.get();
        for (int face = group.faceStart; face <= group.faceStop; face++) {
            for (int point = 0; point < 3; point++) {
                int idx = (face * 3 + point) * vbo.stride + vbo.vertexOffset;
                points.add(new Vec3d(vbo.data[idx], vbo.data[idx+1], vbo.data[idx+2]));
            }
        }
        return points;
    }

    public Binder binder() {
        return new Binder();
    }

    public class Binder {
        private boolean wait = false;
        private int lodSize = Config.MaxTextureSize;
        private String texName = "";

        private Binder() {

        }

        public Binder synchronous() {
            this.wait = true;
            return this;
        }

        public Binder lod(int lodSize) {
            this.lodSize = lodSize;
            return this;
        }

        public Binder texture(String texName) {
            this.texName = texName;
            return this;
        }

        public void apply(RenderState state) {
            if (OBJModel.this.textures.get(texName) == null) {
                texName = ""; // Default
            }

            OBJTextureSheet tex = OBJModel.this.textures.get(texName).get(lodSize);
            if (tex == null) {
                tex = OBJModel.this.textures.get(texName).get(defaultLodSize);
            }
            if (wait) {
                state.texture(tex.synchronous(true));
            } else {
                // Start load even if not loaded
                tex.getId();

                if (!tex.isLoaded()) {
                    // Try to find a loaded LOD, with a sane default
                    tex = OBJModel.this.textures.get(texName).values().stream()
                            .filter(CustomTexture::isLoaded)
                            .findAny().orElse(null);
                }
                if (tex != null) {
                    state.texture(tex);
                } else {
                    state.texture(defTex.synchronous(true));
                }
            }

            OBJTextureSheet normTex = null;
            if (OBJModel.this.normals.containsKey(texName)) {
                Map<Integer, OBJTextureSheet> normLodMap = OBJModel.this.normals.get(texName);
                normTex = normLodMap.get(lodSize);
                if (normTex == null) {
                    normTex = normLodMap.get(defaultLodSize);
                }
            }
            if (normTex != null) {
                if (wait) {
                    state.normals(normTex.synchronous(true));
                } else {
                    normTex.getId();
                    if (!normTex.isLoaded()) {
                        normTex = OBJModel.this.normals.get(texName).values().stream()
                                .filter(CustomTexture::isLoaded)
                                .findAny().orElse(null);
                    }
                    if (normTex != null) {
                        state.normals(normTex);
                    } else {
                        state.normals(defTex);
                    }
                }
            } else {
                state.normals(defTex);
            }

            OBJTextureSheet specTex = null;
            if (OBJModel.this.speculars.containsKey(texName)) {
                Map<Integer, OBJTextureSheet> specLodMap = OBJModel.this.speculars.get(texName);
                specTex = specLodMap.get(lodSize);
                if (specTex == null) {
                    specTex = specLodMap.get(defaultLodSize);
                }
            }
            if (specTex != null) {
                if (wait) {
                    state.specular(specTex.synchronous(true));
                } else {
                    specTex.getId();
                    if (!specTex.isLoaded()) {
                        specTex = OBJModel.this.speculars.get(texName).values().stream()
                                .filter(CustomTexture::isLoaded)
                                .findAny().orElse(null);
                    }
                    if (specTex != null) {
                        state.specular(specTex);
                    } else {
                        state.specular(defTex);
                    }
                }
            } else {
                state.specular(defTex);
            }

            state.smooth_shading(OBJModel.this.isSmoothShading);
        }

        public OBJRender.Binding bind(RenderState state) {
            return bind(state, false);
        }

        public OBJRender.Binding bind(RenderState state, boolean waitForLoad) {
            state = state.clone();
            apply(state);
            return vbo.bind(state, waitForLoad);
        }

        public OBJRender.Builder builder() {
            return vbo.subModel(this::apply);
        }
    }

    public void free() {
        for (Map<Integer, OBJTextureSheet> lodMap : textures.values()) {
            for (OBJTextureSheet texture : lodMap.values()) {
                texture.dealloc();
            }
        }
        vbo.free();
    }
}
