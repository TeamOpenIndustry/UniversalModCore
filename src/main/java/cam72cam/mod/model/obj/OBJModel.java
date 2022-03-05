package cam72cam.mod.model.obj;

import cam72cam.mod.Config;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.obj.OBJTextureSheet;
import cam72cam.mod.render.obj.OBJRender;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.serialization.ResourceCache;
import cam72cam.mod.serialization.ResourceCache.GenericByteBuffer;
import cam72cam.mod.serialization.SerializationException;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.TagSerializer;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static cam72cam.mod.model.obj.ImageUtils.*;

public class OBJModel {
    private static final OBJTextureSheet defTex = new OBJTextureSheet(1, 1, () -> new ResourceCache.GenericByteBuffer(new int[] { 0x0000FF }), Integer.MAX_VALUE);

    public final OBJRender vbo;
    public final int textureWidth;
    public final int textureHeight;
    public final Map<String, OBJTextureSheet> textures = new HashMap<>();
    public final Map<String, OBJTextureSheet> icons = new HashMap<>();
    public final LinkedHashMap<String, OBJGroup> groups; //Order by vertex start/stop
    public final boolean isSmoothShading;

    public String hash;

    public OBJModel(Identifier modelLoc, float darken) throws Exception {
        this(modelLoc, darken, 1, null);
    }

    public OBJModel(Identifier modelLoc, float darken, Collection<String> variants) throws Exception {
        this(modelLoc, darken, 1, variants);
    }

    public OBJModel(Identifier modelLoc, float darken, double scale) throws Exception {
        this(modelLoc, darken, scale, null);
    }

    public OBJModel(Identifier modelLoc, float darken, double scale, Collection<String> variants) throws Exception {
        ResourceCache<OBJBuilder> cache = new ResourceCache<>(
                modelLoc,
                String.format("v1-%s-%s-%s-%s", scale, darken, variants == null ? "null" : String.join(":" + variants).hashCode(), Config.getMaxTextureSize()),
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
                    data.setList("groups", builder.getGroups(), v -> {
                        try {
                            TagCompound tag = new TagCompound();
                            TagSerializer.serialize(tag, v);
                            return tag;
                        } catch (SerializationException e) {
                            throw new RuntimeException(e);
                        }
                    });
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

            for (String variant : meta.getList("variants", k -> k.getString("variant"))) {
                //TODO CACHE SECONDS!
                int cacheSeconds = 30;

                if (Config.getMaxTextureSize() / 8 < Math.max(textureWidth, textureHeight)) {
                    Pair<Integer, Integer> size = scaleSize(textureWidth, textureHeight, Config.getMaxTextureSize()/8);
                    Supplier<GenericByteBuffer> iconData = cache.getResource(variant + "_icon.rgba", builder -> new GenericByteBuffer(toRGBA(scaleImage(builder.getTextures().get(variant).get(), Config.getMaxTextureSize() / 8))));
                    this.icons.put(variant, new OBJTextureSheet(size.getLeft(), size.getRight(), iconData, cacheSeconds, defTex));
                }
                Supplier<GenericByteBuffer> texData = cache.getResource(variant + ".rgba", builder -> new GenericByteBuffer(toRGBA(builder.getTextures().get(variant).get())));
                this.textures.put(variant, new OBJTextureSheet(textureWidth, textureHeight, texData, cacheSeconds, this.icons.getOrDefault(variant, defTex)));
            }
        } else {
            this.textureWidth = -1;
            this.textureHeight = -1;
        }

        this.groups = meta.getList("groups", v -> {
            try {
                OBJGroup group = new OBJGroup();
                TagSerializer.deserialize(v, group);
                return group;
            } catch (SerializationException e) {
                throw new RuntimeException(e);
            }
        }).stream().collect(Collectors.toMap(k -> k.name, v -> v, (x, y) -> y, LinkedHashMap::new));

        this.vbo = new OBJRender(this, () -> new VertexBuffer(vboData.get().floats(), hasVertexNormals));

        this.hash = cache.close();
    }

    public Set<String> groups() {
        return groups.keySet();
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
        private boolean icon = false;
        private String texName = "";

        private Binder() {

        }

        public Binder synchronous() {
            this.wait = true;
            return this;
        }

        public Binder icon() {
            this.icon = true;
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

            state.texture((icon && OBJModel.this.icons.containsKey(texName) ? OBJModel.this.icons : OBJModel.this.textures).get(texName).texture(wait));
            state.smooth_shading(OBJModel.this.isSmoothShading);
        }

        public OBJRender.Binding bind(RenderState state) {
            state = state.clone();
            apply(state);
            return vbo.bind(state);
        }

        public OBJRender.Builder builder() {
            return vbo.subModel(this::apply);
        }
    }

    public void free() {
        for (OBJTextureSheet texture : textures.values()) {
            texture.freeGL();
        }
        for (OBJTextureSheet texture : icons.values()) {
            texture.freeGL();
        }
        vbo.free();
    }
}
