package cam72cam.mod.model.obj;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.serialization.TagCompound;

public class OBJGroup {
    public final String name;
    public final int faceStart;
    public final int faceStop;
    public final Vec3d min;
    public final Vec3d max;
    public final Vec3d normal;

    OBJGroup(String name, int faceStart, int faceStop, Vec3d min, Vec3d max, Vec3d normal) {
        this.name = name;
        this.faceStart = faceStart;
        this.faceStop = faceStop;
        this.min = min;
        this.max = max;
        this.normal = normal;
    }

    OBJGroup(TagCompound d) {
        this(
                d.getString("name"),
                d.getInteger("faceStart"),
                d.getInteger("faceStop"),
                d.getVec3d("min"),
                d.getVec3d("max"),
                d.getVec3d("normal")
        );
    }

    TagCompound toTag() {
        return new TagCompound()
                .setString("name", name)
                .setInteger("faceStart", faceStart)
                .setInteger("faceStop", faceStop)
                .setVec3d("min", min)
                .setVec3d("max", max)
                .setVec3d("normal", normal);
    }
}
