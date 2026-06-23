package cam72cam.umc.api.model.obj;

import cam72cam.umc.api.entity.boundingbox.IBoundingBox;
import cam72cam.umc.api.math.Vec3d;

public class OBJFace {
    public Vertex vertex0;
    public Vertex vertex1;
    public Vertex vertex2;

    public Vec3d normal;

    public IBoundingBox getBoundingBox() {
        Vec3d min = vertex0.pos.min(vertex1.pos.min(vertex2.pos));
        Vec3d max = vertex0.pos.max(vertex1.pos.max(vertex2.pos));
        return IBoundingBox.from(min, max);
    }

    public OBJFace scale(double factor) {
        OBJFace scaled = new OBJFace();

        scaled.vertex0 = vertex0.scale(factor);
        scaled.vertex1 = vertex1.scale(factor);
        scaled.vertex2 = vertex2.scale(factor);

        scaled.normal = new Vec3d(normal.internal());
        return scaled;
    }

    public static class Vertex {
        public Vec3d pos;
        public Vec2f uv;

        public Vertex(Vec3d vertex, Vec2f uv) {
            this.pos = vertex;
            this.uv = uv;
        }

        public Vertex(FaceAccessor.VertexAccessor accessor) {
            this.pos = accessor.posAsVec3d();
            this.uv = accessor.uvAsVec2f();
        }

        public Vertex scale(double factor) {
            Vec3d newPos = pos.scale(factor);
            return new Vertex(newPos, uv);
        }
    }
}
