package cam72cam.mod.model.obj;

public class VertexBuffer {
    public final boolean hasNormals;
    //(vx, vy, vz, nx, ny, nz, u, v, r, g, b, a)
    public final int floatStride;
    public final float[] data;
    public final int vertexOffset;
    public final int normalOffset;
    public final int textureOffset;
    public final int colorOffset;

    public VertexBuffer(int faces, boolean hasNormals) {
        this.hasNormals = hasNormals;
        this.floatStride = hasNormals ? 12 : 9;
        this.data = new float[faces * 3 * floatStride];
        this.vertexOffset = 0;
        this.textureOffset = vertexOffset + 3;
        this.colorOffset = textureOffset + 2;
        this.normalOffset = hasNormals ? colorOffset + 4 : Integer.MIN_VALUE;
    }

    public int getPointStart(int face, int vertex) {
        return face * 3 * floatStride + vertex * floatStride;
    }
}
