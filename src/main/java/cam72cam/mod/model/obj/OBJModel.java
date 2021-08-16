package cam72cam.mod.model.obj;

import cam72cam.mod.Config;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.serialization.ResourceCache;
import cam72cam.mod.serialization.ResourceCache.GenericByteBuffer;
import cam72cam.mod.serialization.SerializationException;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.TagSerializer;

import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static cam72cam.mod.model.obj.ImageUtils.scaleImage;
import static cam72cam.mod.model.obj.ImageUtils.toRGBA;

public class OBJModel {
    public final Supplier<VertexBuffer> vbo;
    public final int textureWidth;
    public final int textureHeight;
    public final Map<String, Supplier<GenericByteBuffer>> textures = new HashMap<>();
    public final Map<String, Supplier<GenericByteBuffer>> icons = new HashMap<>();
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
                this.textures.put(variant, cache.getResource(variant + ".rgba", builder -> new GenericByteBuffer(toRGBA(builder.getTextures().get(variant).get()))));
                if (Config.getMaxTextureSize() / 8 < Math.max(textureWidth, textureHeight)) {
                    this.icons.put(variant, cache.getResource(variant + "_icon.rgba", builder -> new GenericByteBuffer(toRGBA(scaleImage(builder.getTextures().get(variant).get(), Config.getMaxTextureSize() / 8)))));
                }
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

        this.vbo = () -> new VertexBuffer(vboData.get().floats(), hasVertexNormals);

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
}
