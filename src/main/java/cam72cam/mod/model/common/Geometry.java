package cam72cam.mod.model.common;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.common.opengl.VertexBuffer;
import cam72cam.mod.util.With;

import java.util.List;

public class Geometry {
    private final VertexAttribute vao;
    private final VertexBuffer vbo;
    //TODO optional EBO?
    private int ebo;

    private boolean isUploaded;

    public Geometry(VertexAttribute vao, VertexBuffer vbo) {
        this.vao = vao;
        this.vbo = vbo;
    }

    public int getFaceCount() {
        return vbo.getData().length / 3;
    }

    public List<Vec3d> enumerate() {
        return null;
    }

    public void upload() {
        if (isUploaded) return;
        synchronized (this) {
            vbo.upload();
            isUploaded = true;
        }
    }

    public void destroy() {
        if (!isUploaded) return;
        synchronized (this) {
            vbo.close();
            isUploaded = false;
        }
    }

    public With setupRender() {
        return vao.setup().and(vbo.setup());
    }
}
