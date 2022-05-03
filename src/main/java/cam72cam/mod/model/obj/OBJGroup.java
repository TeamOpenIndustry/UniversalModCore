package cam72cam.mod.model.obj;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.serialization.TagField;

public class OBJGroup {
    @TagField
    public final String name;
    @TagField
    public final int faceStart;
    @TagField
    public final int faceStop;
    @TagField
    public final Vec3d min;
    @TagField
    public final Vec3d max;
    @TagField
    public final Vec3d normal;

    OBJGroup() {
        // Reflection
        this(null, 0, 0, null, null, null);
    }

    public OBJGroup(String name, int faceStart, int faceStop, Vec3d min, Vec3d max, Vec3d normal) {
        this.name = name;
        this.faceStart = faceStart;
        this.faceStop = faceStop;
        this.min = min;
        this.max = max;
        this.normal = normal;
    }
}
