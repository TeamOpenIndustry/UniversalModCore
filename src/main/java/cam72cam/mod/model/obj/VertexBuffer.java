package cam72cam.mod.model.obj;

public class VertexBuffer {
    public final boolean hasNormals;
    public final int vertsPerFace;
    //(vx, vy, vz, u, v, r, g, b, a, nx, ny, nz)
    public final int stride;
    public final float[] data;
    public final int vertexOffset;
    public final int textureOffset;
    public final int colorOffset;
    public final int normalOffset;

    private VertexBuffer(float[] data, int faces, boolean hasNormals) {
        this.hasNormals = hasNormals;
        this.vertsPerFace = 3;
        this.vertexOffset = 0;
        this.textureOffset = vertexOffset + 3;
        this.colorOffset = textureOffset + 2;
        this.normalOffset = hasNormals ? colorOffset + 4 : Integer.MIN_VALUE;
        this.stride = hasNormals ? normalOffset + 3 : colorOffset + 4;

        this.data = data != null ? data : new float[faces * vertsPerFace * stride];
    }

    public VertexBuffer(int faces, boolean hasNormals) {
        this(null, faces, hasNormals);
    }
    public VertexBuffer(float[] data, boolean hasNormals) {
        this(data, 0, hasNormals);
    }
}
