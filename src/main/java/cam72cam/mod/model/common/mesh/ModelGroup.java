package cam72cam.mod.model.common.mesh;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.serialization.TagCompound;

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
