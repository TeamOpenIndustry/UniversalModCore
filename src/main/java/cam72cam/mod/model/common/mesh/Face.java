package cam72cam.mod.model.common.mesh;

import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.common.util.FaceAccessor;
import cam72cam.mod.model.obj.Vec2f;

/**
 * An immutable snapshot of a single triangle.
 */
public class Face {
    public final Vertex vertex0;
    public final Vertex vertex1;
    public final Vertex vertex2;

    public final Vec3d normal;
    //TODO more accurate one, like OBB
    private IBoundingBox box;

    public Face(Vertex vertex0, Vertex vertex1, Vertex vertex2, Vec3d normal) {
        this.vertex0 = vertex0;
        this.vertex1 = vertex1;
        this.vertex2 = vertex2;
        this.normal = normal;
    }

    /**
     * @return The AABB of this face
     */
    public IBoundingBox getBoundingBox() {
        if (box == null) {
            //Uses AABB for now but we may want OBB or something more accurate in the future
            Vec3d min = vertex0.pos.min(vertex1.pos.min(vertex2.pos));
            Vec3d max = vertex0.pos.max(vertex1.pos.max(vertex2.pos));
            box = IBoundingBox.from(min, max);
        }
        return box;
    }

    /**
     * @param factor Scale factor applied to the vertices positions.
     * @return A copy of this face with scaled vertices and unchanged normal
     */
    public Face scale(double factor) {
        return new Face(vertex0.scale(factor), vertex1.scale(factor), vertex2.scale(factor), normal);
    }

    public static class Vertex {
        public final Vec3d pos;
        public final Vec2f uv;

        public Vertex(Vec3d vertex, Vec2f uv) {
            this.pos = vertex;
            this.uv = uv;
        }

        public Vertex(FaceAccessor.VertexAccessor accessor) {
            this.pos = accessor.posAsVec3d();
            this.uv = accessor.uvAsVec2f();
        }

        public Vertex scale(double factor) {
            return new Vertex(pos.scale(factor), uv);
        }
    }
}
