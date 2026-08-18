package cam72cam.mod.math;

import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.serialization.TagMapped;

@TagMapped(Plane.TagMapper.class)
public class Plane {
    public final Vec3d normal;
    public final double d;
    public final Vec3d point;

    public Plane(Vec3d normal, double d) {
        this.normal = normal.normalize();
        this.d = d;
        this.point = this.normal.scale(-d);
    }

    public Plane(Vec3d point, Vec3d normal) {
        this.normal = normal.normalize();
        this.d = -this.normal.dotProduct(point);
        this.point = point;
    }

    public double distance(Vec3d p) {
        return normal.dotProduct(p) + d;
    }

    public Plane flip() {
        return new Plane(normal.scale(-1), -d);
    }

    public Plane offset(Vec3d offset) {
        Vec3d offsetVec = new Vec3d(offset.x, offset.y, offset.z);
        double newD = this.d - this.normal.dotProduct(offsetVec);
        return new Plane(this.normal, newD);
    }

    public static class TagMapper implements cam72cam.mod.serialization.TagMapper<Plane> {
        public TagAccessor<Plane> apply(Class<Plane> t, String fieldname, TagField tag) {
            return new TagAccessor<>(
                    (nbt, plane) -> {
                        if (plane == null){
                            nbt.remove(fieldname);
                            return;
                        }
                        TagCompound planeTag = new TagCompound();
                        planeTag.setVec3d("normal", plane.normal);
                        planeTag.setDouble("d", plane.d);
                        nbt.set(fieldname,planeTag);
                    },
                    nbt -> {
                        if (!nbt.hasKey(fieldname)){
                            return null;
                        }
                        TagCompound planeTag = nbt.get(fieldname);
                        Vec3d normal = planeTag.getVec3d("normal");
                        double d = planeTag.getDouble("d");
                        return new Plane(normal, d);
                    }
            );
        }
    }
}