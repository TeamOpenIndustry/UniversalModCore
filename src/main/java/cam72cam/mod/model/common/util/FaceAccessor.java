package cam72cam.mod.model.common.util;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.common.mesh.Face;
import cam72cam.mod.model.common.mesh.Model;
import cam72cam.mod.model.common.mesh.ModelGroup;
import cam72cam.mod.model.common.mesh.VAOLayout;
import cam72cam.mod.model.obj.Vec2f;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A {@link cam72cam.mod.model.common.mesh.Model} API wrapper, making accessing geometry completely separated from backend.
 */
public class FaceAccessor implements Iterable<FaceAccessor> {
    private final Model model;
    private final float[] data;
    private final int stride;
    private final int posOffset;
    private final int uvOffset;
    private final int colorOffset;
    private final int normalOffset;
    
    /**
     * Current face's vertex accessors, assuming all faces are triangles.
     */
    public VertexAccessor v0;
    public VertexAccessor v1;
    public VertexAccessor v2;

    private final boolean canSplit;
    private int currentFaceIndex;
    private final int startFace;
    private final int endFace;

    public FaceAccessor(Model model) {
        this(model, 0, Integer.MAX_VALUE);
    }

    public FaceAccessor(Model model, int startFace, int endFace) {
        this(model, startFace, endFace, true);
    }

    public FaceAccessor(Model model, int startFace, int endFace, boolean canSplit) {
        this.model = model;
        this.data = model.getVboData();
        this.stride = model.getLayout().getStride();
        this.posOffset = model.getLayout().getOffset(VAOLayout.Usage.POSITION);
        this.uvOffset = model.getLayout().getOffset(VAOLayout.Usage.UV);
        this.colorOffset = model.getLayout().getOffset(VAOLayout.Usage.COLOR);
        this.normalOffset = model.getLayout().getOffset(VAOLayout.Usage.NORMAL);
        
        int faceCount = this.model.getVboData().length / stride / 3;
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
        if (!model.getGroups().containsKey(groupName) || !canSplit) {
            return null;
        }
        ModelGroup group = model.getGroups().get(groupName);
        // Pass buffer instead of caching it
        return new FaceAccessor(model, group.faceStart, group.faceEnd + 1, false);
    }

    /**
     * Convert current accessing face into an {@link Face}.
     * @return OBJFace of current face
     */
    public Face asOBJFace() {
        Face.Vertex vert0 = new Face.Vertex(v0);
        Face.Vertex vert1 = new Face.Vertex(v1);
        Face.Vertex vert2 = new Face.Vertex(v2);
        return new Face(vert0, vert1, vert2, normalOffset != -1 
                                             ? v0.normAsVec3d() 
                                             : vert1.pos.subtract(vert0.pos).crossProduct(vert2.pos.subtract(vert0.pos)).normalize());
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
            if (uvOffset == -1) {
                return Vec2f.ZERO;
            }
            return new Vec2f(u(), v());
        }

        public Vec3d normAsVec3d() {
            if (normalOffset == -1) {
                return Vec3d.ZERO;
            }
            return new Vec3d(nx(), ny(), nz());
        }

        public float x() {
            return data[(currentFaceIndex * 3 + vertOffset) * stride + posOffset];
        }

        public float y() {
            return data[(currentFaceIndex * 3 + vertOffset) * stride + posOffset + 1];
        }

        public float z() {
            return data[(currentFaceIndex * 3 + vertOffset) * stride + posOffset + 2];
        }

        public float u() {
            if (uvOffset == -1) {
                return 0;
            }
            return data[(currentFaceIndex * 3 + vertOffset) * stride + uvOffset];
        }

        public float v() {
            if (uvOffset == -1) {
                return 0;
            }
            return data[(currentFaceIndex * 3 + vertOffset) * stride + uvOffset + 1];
        }

        public float r() {
            if (colorOffset == -1) {
                return 0;
            }
            return data[(currentFaceIndex * 3 + vertOffset) * stride + colorOffset];
        }

        public float g() {
            if (colorOffset == -1) {
                return 0;
            }
            return data[(currentFaceIndex * 3 + vertOffset) * stride + colorOffset + 1];
        }

        public float b() {
            if (colorOffset == -1) {
                return 0;
            }
            return data[(currentFaceIndex * 3 + vertOffset) * stride + colorOffset + 2];
        }

        public float a() {
            if (colorOffset == -1) {
                return 0;
            }
            return data[(currentFaceIndex * 3 + vertOffset) * stride + colorOffset + 3];
        }

        public float nx() {
            if (normalOffset == -1) {
                return 0;
            }
            return data[(currentFaceIndex * 3 + vertOffset) * stride + normalOffset];
        }

        public float ny() {
            if (normalOffset == -1) {
                return 0;
            }
            return model.getVboData()[(currentFaceIndex * 3 + vertOffset) * stride + normalOffset + 1];
        }

        public float nz() {
            if (normalOffset == -1) {
                return 0;
            }
            return data[(currentFaceIndex * 3 + vertOffset) * stride + normalOffset + 2];
        }
    }
}