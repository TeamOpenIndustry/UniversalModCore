package cam72cam.mod.render.cutter;

import cam72cam.mod.math.Vec3d;

public class ClipVertex {

    public Vec3d pos;

    public float u;
    public float v;

    public int color;
    public int light;

    public byte nx;
    public byte ny;
    public byte nz;

    public ClipVertex(
            Vec3d pos,
            float u,
            float v,
            int color,
            int light,
            byte nx,
            byte ny,
            byte nz) {

        this.pos = pos;

        this.u = u;
        this.v = v;

        this.color = color;
        this.light = light;

        this.nx = nx;
        this.ny = ny;
        this.nz = nz;
    }

    public ClipVertex copy() {
        return new ClipVertex(
                pos,
                u,
                v,
                color,
                light,
                nx,
                ny,
                nz
        );
    }

    public ClipVertex lerp(ClipVertex other, double t) {

        Vec3d p = pos.add(other.pos.subtract(pos).scale(t));

        float nu = (float) (u + (other.u - u) * t);
        float nv = (float) (v + (other.v - v) * t);

        int c = lerpColor(color, other.color, t);

        int l = lerpLight(light, other.light, t);

        byte nnx = (byte) Math.round(nx + (other.nx - nx) * t);
        byte nny = (byte) Math.round(ny + (other.ny - ny) * t);
        byte nnz = (byte) Math.round(nz + (other.nz - nz) * t);

        return new ClipVertex(
                p,
                nu,
                nv,
                c,
                l,
                nnx,
                nny,
                nnz
        );
    }

    private static int lerpColor(int a, int b, double t) {

        int aa = (a >>> 24) & 255;
        int ar = (a >>> 16) & 255;
        int ag = (a >>> 8) & 255;
        int ab = a & 255;

        int ba = (b >>> 24) & 255;
        int br = (b >>> 16) & 255;
        int bg = (b >>> 8) & 255;
        int bb = b & 255;

        int ca = (int)Math.round(aa + (ba - aa) * t);
        int cr = (int)Math.round(ar + (br - ar) * t);
        int cg = (int)Math.round(ag + (bg - ag) * t);
        int cb = (int)Math.round(ab + (bb - ab) * t);

        return (ca << 24)
                | (cr << 16)
                | (cg << 8)
                | cb;
    }

    /**
     * UV2(lightmap)format:
     * low16 = block light
     * high16 = sky light
     */
    private static int lerpLight(int a, int b, double t) {

        int ablock = a & 0xffff;
        int asky = (a >>> 16) & 0xffff;

        int bblock = b & 0xffff;
        int bsky = (b >>> 16) & 0xffff;

        int block = (int)Math.round(ablock + (bblock - ablock) * t);
        int sky = (int)Math.round(asky + (bsky - asky) * t);

        return (sky << 16) | block;
    }

    public ClipVertex(Vec3d pos) {
        this(
                pos,
                0,
                0,
                -1,
                0,
                (byte)0,
                (byte)0,
                (byte)0
        );
    }

    public ClipVertex(Vec3d pos, float u, float v) {
        this(
                pos,
                u,
                v,
                -1,
                0,
                (byte)0,
                (byte)0,
                (byte)0
        );
    }

    public ClipVertex(
            Vec3d pos,
            float u,
            float v,
            int color,
            int light) {

        this(
                pos,
                u,
                v,
                color,
                light,
                (byte)0,
                (byte)0,
                (byte)0
        );
    }

    public static ClipVertex of(Vec3d pos) {
        return new ClipVertex(pos);
    }

    public static ClipVertex of(
            Vec3d pos,
            float u,
            float v) {

        return new ClipVertex(pos, u, v);
    }
}