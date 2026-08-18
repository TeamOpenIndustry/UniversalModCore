package cam72cam.mod.model.common.mesh;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.common.ModelLoader;
import cam72cam.mod.model.common.util.FaceAccessor;
import cam72cam.mod.render.common.ModelRenderer;
import cam72cam.mod.render.obj.OBJTextureSheet;
import cam72cam.mod.resource.Identifier;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * The runtime representation of a parsed model.<br>
 * Models are normally obtained via {@link ModelLoader#load} and drawn with
 * {@link ModelRenderer#getRendererFor}. Call {@link #free()} to manually release GPU resources when
 * the model is no longer needed.</p>
 */
public class Model {
    private final Identifier location;
    private final VAOLayout layout;
    private final Supplier<float[]> vboSupplier;
    private float[] vboData = null;
    private final LinkedHashMap<String, ModelGroup> groups;

    public final boolean hasSpecular;
    public final boolean hasNormal;
    public final boolean isSmoothShading;

    public final int packedTextureWidth;
    public final int packedTextureHeight;
    private int defaultLodSize;

    private final Map<String, NavigableMap<Integer, OBJTextureSheet>> textures = new HashMap<>();
    private final Map<String, NavigableMap<Integer, OBJTextureSheet>> speculars = new HashMap<>();
    private final Map<String, NavigableMap<Integer, OBJTextureSheet>> normals = new HashMap<>();

    public String hash;

    // The model where self textures came from, tracked for shared textures dealloc
    private Model textureOwner = this;
    private final AtomicInteger refCount = new AtomicInteger(1);

    public Model(Identifier location, VAOLayout layout, Supplier<float[]> vboSupplier, LinkedHashMap<String, ModelGroup> groups,
                 boolean hasSpecular, boolean hasNormal, boolean isSmoothShading,
                 int packedTextureWidth, int packedTextureHeight) {
        this.location = location;
        this.layout = layout;
        this.vboSupplier = vboSupplier;
        this.groups = groups;
        this.hasSpecular = hasSpecular;
        this.hasNormal = hasNormal;
        this.isSmoothShading = isSmoothShading;
        this.packedTextureWidth = packedTextureWidth;
        this.packedTextureHeight = packedTextureHeight;
    }

    /**
     * Links the packed texture sheets (albedo, specular, normal) to this model.
     *
     * <p>Called during cache reconstruction and sub-model baking. The maps are keyed by
     * variant name and LOD size.
     *
     * @param tex  albedo sheets per variant/LOD
     * @param spec specular sheets per variant/LOD (may be empty)
     * @param norm normal sheets per variant/LOD (may be empty)
     */
    public void linkTextures(Map<String, NavigableMap<Integer, OBJTextureSheet>> tex,
                             Map<String, NavigableMap<Integer, OBJTextureSheet>> spec,
                             Map<String, NavigableMap<Integer, OBJTextureSheet>> norm) {
        this.textures.putAll(tex);
        this.speculars.putAll(spec);
        this.normals.putAll(norm);
        defaultLodSize = textures.values().stream()
                                 .flatMap(m -> m.keySet().stream())
                                 .mapToInt(i -> i).max().orElse(-1);
    }

    // ModelGroup helpers

    /** @return the named groups of this model, keyed by group name */
    public Map<String, ModelGroup> getGroups() {
        return groups;
    }

    /** @return the set of group names in this model */
    public Set<String> groups() {
        return groups.keySet();
    }

    /**
     * @param groupNames groups to include
     * @return the minimum corner of the axis-aligned bounds of the given groups
     */
    public Vec3d minOfGroups(Iterable<String> groupNames) {
        Vec3d min = null;
        for (String group : groupNames) {
            Vec3d gmin = groups.get(group).min();
            if (min == null) {
                min = gmin;
            } else {
                min = min.min(gmin);
            }
        }
        return min;
    }

    /**
     * @param groupNames groups to include
     * @return the maximum corner of the axis-aligned bounds of the given groups
     */
    public Vec3d maxOfGroups(Iterable<String> groupNames) {
        Vec3d max = null;
        for (String group : groupNames) {
            Vec3d gmax = groups.get(group).max();
            if (max == null) {
                max = gmax;
            } else {
                max = max.max(gmax);
            }
        }
        return max;
    }

    /**
     * @param groupNames groups to include
     * @return the center of the axis-aligned bounds of the given groups
     */
    public Vec3d centerOfGroups(Iterable<String> groupNames) {
        Vec3d min = minOfGroups(groupNames);
        Vec3d max = maxOfGroups(groupNames);
        return new Vec3d((min.x + max.x) / 2, (min.y + max.y) / 2, (min.z + max.z) / 2);
    }

    /** @return the length (x extent) of the axis-aligned bounds of the given groups */
    public double lengthOfGroups(Iterable<String> groupNames) {
        Vec3d min = minOfGroups(groupNames);
        Vec3d max = maxOfGroups(groupNames);
        return max.x - min.x;
    }

    /** @return the width (z extent) of the axis-aligned bounds of the given groups */
    public double widthOfGroups(Iterable<String> groupNames) {
        Vec3d min = minOfGroups(groupNames);
        Vec3d max = maxOfGroups(groupNames);
        return max.z - min.z;
    }

    /** @return the height (y extent) of the axis-aligned bounds of the given groups */
    public double heightOfGroups(Iterable<String> groupNames) {
        Vec3d min = minOfGroups(groupNames);
        Vec3d max = maxOfGroups(groupNames);
        return max.y - min.y;
    }

    /** WARNING This is a very slow function and should be used for debug only */
    public List<Vec3d> points(ModelGroup group) {
        getVboData(); //Populate
        List<Vec3d> points = new ArrayList<>();
        for (int face = group.faceStart; face <= group.faceEnd; face++) {
            for (int point = 0; point < 3; point++) {
                int idx = (face * 3 + point) * layout.getStride() + layout.getOffset(VAOLayout.Usage.POSITION);
                points.add(new Vec3d(vboData[idx], vboData[idx+1], vboData[idx+2]));
            }
        }
        return points;
    }

    /**
     * @return a cursor-based, iterable view over the faces of this model, giving typed
     *         access to per-vertex attributes
     */
    public FaceAccessor getFaceAccessor() {
        return new FaceAccessor(this);
    }

    /** @return the resource identifier this model was loaded from */
    public Identifier location() {
        return location;
    }

    /** @return the vertex layout describing this model's interleaved vertex data */
    public VAOLayout getLayout() {
        return layout;
    }

    /**
     * Lazily materializes and returns the interleaved vertex data of this model.
     *
     * @return the raw VBO data, in the order described by {@link #getLayout()}
     */
    public float[] getVboData() {
        if (vboData == null) {
            vboData = vboSupplier.get();
        }
        return vboData;
    }

    /** @return albedo texture sheets, keyed by variant name then LOD size */
    public Map<String, NavigableMap<Integer, OBJTextureSheet>> getTextures() {
        return textures;
    }

    /** @return specular texture sheets, keyed by variant name then LOD size */
    public Map<String, NavigableMap<Integer, OBJTextureSheet>> getSpeculars() {
        return speculars;
    }

    /** @return normal texture sheets, keyed by variant name then LOD size */
    public Map<String, NavigableMap<Integer, OBJTextureSheet>> getNormals() {
        return normals;
    }

    /** @return the size of the largest LOD texture available for this model */
    public int getDefaultLodSize() {
        return defaultLodSize;
    }

    /**
     * Releases this model's reference to the shared texture sheets and its cached renderer.
     */
    public void free() {
        tryReleaseTexture();
        ModelRenderer.getRendererFor(this).free();
    }

    /**
     * Decrements the reference count of the shared texture resources, and deallocating the texture
     * sheets once the last referencing model is released.
     */
    protected final void tryReleaseTexture() {
        Model owner = this;
        while (owner.textureOwner != owner) {
            owner = owner.textureOwner;
        }
        if (owner.refCount.decrementAndGet() <= 0) {
            owner.deallocTextures();
        }
    }

    /**
     * Shares the texture lifetime of {@code owner}. Called by derived models (e.g. baked
     * models) that reference another model's texture sheets.
     */
    protected final void shareTexturesWith(Model owner) {
        this.textureOwner = owner;
        this.linkTextures(owner.getTextures(), owner.getSpeculars(), owner.getNormals());
        while (owner.textureOwner != owner) {
            owner = owner.textureOwner;
        }
        owner.refCount.getAndIncrement();
    }

    private void deallocTextures() {
        for (Map<Integer, OBJTextureSheet> lodMap : textures.values()) {
            for (OBJTextureSheet texture : lodMap.values()) {
                texture.dealloc();
            }
        }
        for (Map<Integer, OBJTextureSheet> lodMap : speculars.values()) {
            for (OBJTextureSheet texture : lodMap.values()) {
                texture.dealloc();
            }
        }
        for (Map<Integer, OBJTextureSheet> lodMap : normals.values()) {
            for (OBJTextureSheet texture : lodMap.values()) {
                texture.dealloc();
            }
        }
    }
}
