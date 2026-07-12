package cam72cam.mod.model.obj;

import cam72cam.mod.math.Vec3d;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A {@link VertexBuffer} API wrapper, making accessing geometry completely separated from backend.
 */
public class FaceAccessor implements Iterable<FaceAccessor> {
    private final OBJModel model;

    /**
     * Current face's vertex accessors, assuming all faces are triangles.
     */
    public VertexAccessor v0;
    public VertexAccessor v1;
    public VertexAccessor v2;

    private final VertexBuffer vbo;
    private final boolean canSplit;
    private int currentFaceIndex;
    private final int startFace;
    private final int endFace;

    /**
     * Internal, use OBJModel.getFaceAccessor
     */
    protected FaceAccessor(OBJModel model) {
        this(model, 0, Integer.MAX_VALUE);
    }

    protected FaceAccessor(OBJModel model, int startFace, int endFace) {
        this(model, startFace, endFace, true, null);
    }

    protected FaceAccessor(OBJModel model, int startFace, int endFace, boolean canSplit, VertexBuffer buffer) {
        this.model = model;
        this.vbo = buffer == null ? model.vbo.buffer.get() : buffer;
        int faceCount = this.vbo.data.length / this.vbo.stride / this.vbo.vertsPerFace;
        v0 = new VertexAccessor(0);
        v1 = new VertexAccessor(1);
        v2 = new VertexAccessor(2);
        if (endFace < startFace) {
            throw new IllegalStateException();
        }
        this.startFace = Math.max(0, startFace);
        this.endFace = Math.min(faceCount, endFace);
        this.currentFaceIndex = this.startFace;
        this.canSplit = canSplit;
    }

    /**
     * Get another {@link FaceAccessor} of certain group.
     * @param groupName Desired group's name
     * @return A {@link FaceAccessor} of given group, or {@code null} if group not present or this FaceAccessor is already grouped
     */
    public FaceAccessor getSubByGroup(String groupName) {
        if (!model.groups.containsKey(groupName) || !canSplit) {
            return null;
        }
        OBJGroup group = model.groups.get(groupName);
        // Pass buffer instead of caching it
        return new FaceAccessor(model, group.faceStart, group.faceStop + 1, false, vbo);
    }

    /**
     * Convert current accessing face into an {@link OBJFace}.
     * @return OBJFace of current face
     */
    public OBJFace asOBJFace() {
        OBJFace face = new OBJFace();

        face.vertex0 = new OBJFace.Vertex(v0);
        face.vertex1 = new OBJFace.Vertex(v1);
        face.vertex2 = new OBJFace.Vertex(v2);

        if (vbo.hasNormals) {
            face.normal = v0.normAsVec3d();
        } else {
            Vec3d v0 = face.vertex0.pos;
            Vec3d v1 = face.vertex1.pos;
            Vec3d v2 = face.vertex2.pos;
            face.normal = v1.subtract(v0).crossProduct(v2.subtract(v0)).normalize();
        }
        return face;
    }

    /**
     * An iterator designed specifically for for-each loop, use {@code asOBJFace} to create snapshot if you want to store it externally.
     * @return Iterator of self, only recommend to use in for-each
     */
    @Override
    @Nonnull
    public Iterator<FaceAccessor> iterator() {
        return new FaceIterator();
    }

    private class FaceIterator implements Iterator<FaceAccessor> {
        private int iteratorIndex;

        public FaceIterator() {
            this.iteratorIndex = startFace;
            currentFaceIndex = startFace;
        }

        @Override
        public boolean hasNext() {
            return iteratorIndex < endFace;
        }

        @Override
        public FaceAccessor next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            currentFaceIndex = iteratorIndex++;
            return FaceAccessor.this;
        }

        @Override
        public void remove() {
            Iterator.super.remove();
        }
    }

    @Override
    public Spliterator<FaceAccessor> spliterator() {
        return Spliterators.spliterator(iterator(), (endFace - startFace), Spliterator.SIZED | Spliterator.ORDERED | Spliterator.IMMUTABLE | Spliterator.NONNULL);
    }

    public Stream<FaceAccessor> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    /**
     * Vertex data accessor. Provides typed access to vertex attributes.
     * <br>
     * Use snapshot methods (posAsVec3d, etc.) to store data externally.
     */
    public class VertexAccessor {
        public final int vertOffset;

        protected VertexAccessor(int vertOffset) {
            this.vertOffset = vertOffset;
        }

        public Vec3d posAsVec3d() {
            return new Vec3d(x(), y(), z());
        }

        public Vec2f uvAsVec2f() {
            return new Vec2f(u(), v());
        }

        public Vec3d normAsVec3d() {
            if (!vbo.hasNormals) {
                return Vec3d.ZERO;
            }
            return new Vec3d(nx(), ny(), nz());
        }

        public float x() {
            return vbo.data[(currentFaceIndex * 3 + vertOffset) * vbo.stride + vbo.vertexOffset];
        }

        public float y() {
            return vbo.data[(currentFaceIndex * 3 + vertOffset) * vbo.stride + vbo.vertexOffset + 1];
        }

        public float z() {
            return vbo.data[(currentFaceIndex * 3 + vertOffset) * vbo.stride + vbo.vertexOffset + 2];
        }

        public float u() {
            return vbo.data[(currentFaceIndex * 3 + vertOffset) * vbo.stride + vbo.textureOffset];
        }

        public float v() {
            return vbo.data[(currentFaceIndex * 3 + vertOffset) * vbo.stride + vbo.textureOffset + 1];
        }

        public float r() {
            return vbo.data[(currentFaceIndex * 3 + vertOffset) * vbo.stride + vbo.colorOffset];
        }

        public float g() {
            return vbo.data[(currentFaceIndex * 3 + vertOffset) * vbo.stride + vbo.colorOffset + 1];
        }

        public float b() {
            return vbo.data[(currentFaceIndex * 3 + vertOffset) * vbo.stride + vbo.colorOffset + 2];
        }

        public float a() {
            return vbo.data[(currentFaceIndex * 3 + vertOffset) * vbo.stride + vbo.colorOffset + 3];
        }

        public float nx() {
            if (!vbo.hasNormals) {
                return 0;
            }
            return vbo.data[(currentFaceIndex * 3 + vertOffset) * vbo.stride + vbo.normalOffset];
        }

        public float ny() {
            if (!vbo.hasNormals) {
                return 0;
            }
            return vbo.data[(currentFaceIndex * 3 + vertOffset) * vbo.stride + vbo.normalOffset + 1];
        }

        public float nz() {
            if (!vbo.hasNormals) {
                return 0;
            }
            return vbo.data[(currentFaceIndex * 3 + vertOffset) * vbo.stride + vbo.normalOffset + 2];
        }
    }
}
