package cam72cam.mod.model.common;

import cam72cam.mod.math.Vec3d;

import java.util.List;
import java.util.stream.Collectors;

public class ModelGroup {
    public final String name;
    public final int faceStart;
    public final int faceStop;
    public final Vec3d min;
    public final Vec3d max;
    public final Vec3d normal;

    public ModelGroup(String name, int faceStart, int faceStop, Vec3d min, Vec3d max, Vec3d normal) {
        this.name = name;
        this.faceStart = faceStart;
        this.faceStop = faceStop;
        this.min = min;
        this.max = max;
        this.normal = normal;
    }

    public static ModelGroup fromGeometry(Geometry geometry) {
        String name = "model";
        int faceStart = 0;
        int faceStop = geometry.getFaceCount() - 1;

        List<Vec3d> points = geometry.enumerate();
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
        for (Vec3d point : points) {
            if (min.distanceToSquared(max) < point.distanceToSquared(max)) {
                min = point;
            }
        }
        Vec3d finalMin = min.lengthSquared() < max.lengthSquared() ? min : max;
        Vec3d finalMax = min.lengthSquared() < max.lengthSquared() ? max : min;
        List<Vec3d> minG = points.stream().filter(p -> p.distanceToSquared(finalMin) < p.distanceToSquared(finalMax)).collect(
                Collectors.toList());
        List<Vec3d> maxG = points.stream().filter(p -> p.distanceToSquared(finalMin) > p.distanceToSquared(finalMax)).collect(Collectors.toList());
        Vec3d minN = minG.stream().reduce(Vec3d.ZERO, Vec3d::add).scale(1. / minG.size());
        Vec3d maxN = maxG.stream().reduce(Vec3d.ZERO, Vec3d::add).scale(1. / maxG.size());
        Vec3d normal = maxN.subtract(minN).normalize();
        return new ModelGroup(name, faceStart, faceStop, minN, maxN, normal);
    }
}
