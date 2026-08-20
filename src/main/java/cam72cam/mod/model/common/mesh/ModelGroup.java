package cam72cam.mod.model.common.mesh;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.serialization.TagCompound;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ModelGroup {
    public final String name;
    // Both inclusive
    public final int faceStart;
    public final int faceEnd;

    private final Supplier<Pair<Vec3d, Vec3d>> bounds;
    private Vec3d min;
    private Vec3d max;

    // Approximated direction
    private final Vec3d normal;

    private ModelGroup(String name, int faceStart, int faceEnd, Supplier<Pair<Vec3d, Vec3d>> bounds, Vec3d normal) {
        this.name = name;
        this.faceStart = faceStart;
        this.faceEnd = faceEnd;
        this.bounds = bounds;
        this.normal = normal;
    }

    /**
     * Creates a group with precomputed bounds, e.g. one loaded from the cache or built by a parser.
     */
    public static ModelGroup construct(String name, int faceStart, int faceEnd, Vec3d min, Vec3d max, Vec3d normal) {
        return new ModelGroup(name, faceStart, faceEnd, () -> Pair.of(min, max), normal);
    }

    /**
     * Creates a group whose bounds are computed lazily from the given VBO range on first access.
     * The normal remains uncomputed and is <code>null</code>.
     *
     * @param vbo       Interleaved vertex data (the same array the owning model draws from)
     * @param layout    Vertex layout describing the stride/position offset of {@code vbo}
     */
    public static ModelGroup lazy(String name, int faceStart, int faceEnd, float[] vbo, VAOLayout layout) {
        return new ModelGroup(name, faceStart, faceEnd,
                () -> computeBounds(vbo, layout, faceStart, faceEnd),
                null);
    }

    /** @return The minimum corner of this group's axis-aligned bounds */
    public Vec3d min() {
        if (min == null) {
            Pair<Vec3d, Vec3d> pair = bounds.get();
            min = pair.getLeft();
            max = pair.getRight();
        }
        return min;
    }

    /** @return The maximum corner of this group's axis-aligned bounds */
    public Vec3d max() {
        if (max == null) {
            Pair<Vec3d, Vec3d> pair = bounds.get();
            min = pair.getLeft();
            max = pair.getRight();
        }
        return max;
    }

    public Vec3d normal() {
        return normal;
    }

    /**
     * Computes the bounds and normal of a group from its vertex points.
     *
     * @param name    Group name
     * @param start   First face index (inclusive)
     * @param faceEnd Last face index (inclusive)
     * @param points  The de-duplicated vertex positions belonging to the group
     * @return The constructed group
     */
    public static ModelGroup buildGroup(String name, int start, int faceEnd, List<Vec3d> points) {
        Vec3d first = points.get(0);
        Vec3d groupMin = points.stream().reduce(first, Vec3d::min);
        Vec3d groupMax = points.stream().reduce(first, Vec3d::max);
        Vec3d center = groupMax.add(groupMin).scale(0.5);

        Vec3d min = first;
        Vec3d max = first;
        // Furthest from center
        for (Vec3d point : points) {
            if (max.distanceToSquared(center) < point.distanceToSquared(center)) {
                max = point;
            }
        }
        // Furthest from max
        for (Vec3d point : points) {
            if (min.distanceToSquared(max) < point.distanceToSquared(max)) {
                min = point;
            }
        }
        Vec3d finalMin = min.lengthSquared() < max.lengthSquared() ? min : max;
        Vec3d finalMax = min.lengthSquared() < max.lengthSquared() ? max : min;
        List<Vec3d> minG = points.stream()
                                 .filter(p -> p.distanceToSquared(finalMin) < p.distanceToSquared(finalMax))
                                 .collect(Collectors.toList());
        List<Vec3d> maxG = points.stream()
                                 .filter(p -> p.distanceToSquared(finalMin) > p.distanceToSquared(finalMax))
                                 .collect(Collectors.toList());
        Vec3d normal = new Vec3d(0, 0, 1);
        if (!minG.isEmpty() && !maxG.isEmpty()) {
            Vec3d minN = minG.stream().reduce(Vec3d.ZERO, Vec3d::add).scale(1d / minG.size());
            Vec3d maxN = maxG.stream().reduce(Vec3d.ZERO, Vec3d::add).scale(1d / maxG.size());
            normal = maxN.subtract(minN).normalize();
        }

        return construct(name, start, faceEnd, groupMin, groupMax, normal);
    }

    private static Pair<Vec3d, Vec3d> computeBounds(float[] vbo, VAOLayout layout, int faceStart, int faceEnd) {
        int stride = layout.getStride();
        int posOff = layout.getOffset(VAOLayout.Usage.POSITION);
        Vec3d min = null;
        Vec3d max = null;
        for (int face = faceStart; face <= faceEnd; face++) {
            for (int k = 0; k < 3; k++) {
                int idx = (face * 3 + k) * stride + posOff;
                Vec3d p = new Vec3d(vbo[idx], vbo[idx + 1], vbo[idx + 2]);
                min = min == null ? p : min.min(p);
                max = max == null ? p : max.max(p);
            }
        }
        return Pair.of(min, max);
    }

    public TagCompound serialize() {
        return new TagCompound()
                .setString("name", name)
                .setInteger("faceStart", faceStart)
                .setInteger("faceStop", faceEnd)
                .setVec3d("min", min())
                .setVec3d("max", max())
                .setVec3d("normal", normal() == null ? Vec3d.ZERO : normal);
    }

    public static ModelGroup deserialize(TagCompound d) {
        return construct(
                d.getString("name"),
                d.getInteger("faceStart"),
                d.getInteger("faceStop"),
                d.getVec3d("min"),
                d.getVec3d("max"),
                d.getVec3d("normal")
        );
    }
}
