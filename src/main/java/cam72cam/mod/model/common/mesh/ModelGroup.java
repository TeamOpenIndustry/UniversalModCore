package cam72cam.mod.model.common.mesh;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.serialization.TagCompound;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ModelGroup {
    public final String name;
    public final int faceStart;
    public final int faceEnd;
    public final Vec3d min;
    public final Vec3d max;
    public final Vec3d normal;

    ModelGroup(String name, int faceStart, int faceEnd, Vec3d min, Vec3d max, Vec3d normal) {
        this.name = name;
        this.faceStart = faceStart;
        this.faceEnd = faceEnd;
        this.min = min;
        this.max = max;
        this.normal = normal;
    }

    static ModelGroup buildGroup(String name, int start, int end, List<Vec3d> points) {
        if (points.isEmpty()) {
            points = Collections.singletonList(Vec3d.ZERO);
        }

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

        return new ModelGroup(name, start, end, groupMin, groupMax, normal);
    }

    static ModelGroup deserialize(TagCompound d) {
        return new ModelGroup(
                d.getString("name"),
                d.getInteger("faceStart"),
                d.getInteger("faceStop"),
                d.getVec3d("min"),
                d.getVec3d("max"),
                d.getVec3d("normal")
        );
    }

    TagCompound serialize() {
        return new TagCompound()
                .setString("name", name)
                .setInteger("faceStart", faceStart)
                .setInteger("faceStop", faceEnd)
                .setVec3d("min", min)
                .setVec3d("max", max)
                .setVec3d("normal", normal);
    }
}
