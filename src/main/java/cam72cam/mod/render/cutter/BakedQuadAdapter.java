package cam72cam.mod.render.cutter;

import cam72cam.mod.math.Plane;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.util.Facing;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

import java.util.ArrayList;
import java.util.List;

public class BakedQuadAdapter implements PrimitiveAdapter<BakedQuad, QuadTemplate> {
    private static final int STRIDE = DefaultVertexFormats.BLOCK.getIntegerSize();

    @Override
    public QuadTemplate createTemplate(List<BakedQuad> quads, Plane plane) {
        if (quads.isEmpty()) {
            return null;
        }

        BakedQuad source = findBestQuad(quads, plane);
        if (source == null) {
            return null;
        }

        Vec3d[] sourcePos = new Vec3d[4];
        float[] sourceU = new float[4];
        float[] sourceV = new float[4];
        int[] data = source.getVertexData();

        for (int i = 0; i < 4; i++) {
            int base = i * STRIDE;
            sourcePos[i] = new Vec3d(
                    Float.intBitsToFloat(data[base]),
                    Float.intBitsToFloat(data[base + 1]),
                    Float.intBitsToFloat(data[base + 2])
            );
            sourceU[i] = Float.intBitsToFloat(data[base + 4]);
            sourceV[i] = Float.intBitsToFloat(data[base + 5]);
        }

        return new QuadTemplate(
                source.func_187508_a(),
                Facing.fromNormal(plane.normal),
                source.getTintIndex(),
                source.shouldApplyDiffuseLighting(),
                false,
                DefaultVertexFormats.BLOCK,
                source,
                quads,
                sourcePos,
                sourceU,
                sourceV
        );
    }

    private static BakedQuad findBestQuad(List<BakedQuad> quads, Plane plane) {
        Facing target = Facing.fromNormal(plane.normal.scale(-1));
        if (target == null) {
            return null;
        }

        for (BakedQuad quad : quads) {
            Facing face = Facing.from(quad.getFace());
            if (face == target) {
                return quad;
            }
        }

        return quads.isEmpty() ? null : quads.get(0);
    }

    @Override
    public Polygon toPolygon(BakedQuad quad) {
        List<ClipVertex> verts = new ArrayList<>(4);

        int[] data = quad.getVertexData();
        for (int i = 0; i < 4; i++) {
            verts.add(readVertex(data, i));
        }

        Facing dir = Facing.from(quad.getFace());
        Vec3d normal = new Vec3d(dir.getXMultiplier(), dir.getYMultiplier(), dir.getZMultiplier());
        return new Polygon(verts, normal);
    }

    @Override
    public List<BakedQuad> fromPrimitive(Polygon polygon, BakedQuad primitive) {
        List<BakedQuad> result = new ArrayList<>();
        if (polygon.getVertices().size() < 3) {
            return result;
        }

        for (Polygon quad : polygon.convexToQuads()) {
            int[] data = primitive.getVertexData().clone();
            List<ClipVertex> quadVerts = quad.getVertices();

            writeVertex(data, 0, quadVerts.get(0));
            writeVertex(data, 1, quadVerts.get(1));
            writeVertex(data, 2, quadVerts.get(2));
            writeVertex(data, 3, quadVerts.get(3));

            result.add(new BakedQuad(
                    data,
                    primitive.getTintIndex(),
                    primitive.getFace(),
                    primitive.func_187508_a(),
                    primitive.shouldApplyDiffuseLighting()
            ));
        }
        return result;
    }

    @Override
    public List<BakedQuad> fromTemplate(Polygon polygon, QuadTemplate template) {
        List<BakedQuad> result = new ArrayList<>();
        if (polygon.getVertices().size() < 3) {
            return result;
        }

        applyNormal(polygon, template.facing);

        for (Polygon quad : polygon.convexToQuads()) {
            int[] data = template.source.getVertexData().clone();
            List<ClipVertex> quadVerts = quad.getVertices();

            writeVertex(data, 0, quadVerts.get(3));
            writeVertex(data, 1, quadVerts.get(2));
            writeVertex(data, 2, quadVerts.get(1));
            writeVertex(data, 3, quadVerts.get(0));

            result.add(new BakedQuad(
                    data,
                    template.source.getTintIndex(),
                    template.source.getFace(),
                    template.source.func_187508_a(),
                    template.source.shouldApplyDiffuseLighting()
            ));
        }
        return result;
    }

    private static void applyNormal(Polygon polygon, Facing facing) {
        byte nx = (byte) (facing.getXMultiplier() * 127);
        byte ny = (byte) (facing.getYMultiplier() * 127);
        byte nz = (byte) (facing.getZMultiplier() * 127);

        for (ClipVertex vertex : polygon.getVertices()) {
            vertex.nx = nx;
            vertex.ny = ny;
            vertex.nz = nz;
        }
    }

    @Override
    public void prepareCap(Polygon polygon, Plane plane, QuadTemplate template) {
        polygon.generateUV(template);
    }

    private static ClipVertex readVertex(int[] data, int index) {
        int base = index * STRIDE;

        float x = Float.intBitsToFloat(data[base]);
        float y = Float.intBitsToFloat(data[base + 1]);
        float z = Float.intBitsToFloat(data[base + 2]);

        int color = data[base + 3];

        float u = Float.intBitsToFloat(data[base + 4]);
        float v = Float.intBitsToFloat(data[base + 5]);

        int light = data[base + 6];

        return new ClipVertex(new Vec3d(x, y, z), u, v, color, light, (byte)0, (byte)0, (byte)0);
    }

    private static void writeVertex(int[] data, int index, ClipVertex v) {
        int base = index * STRIDE;

        // DefaultVertexFormats.BLOCK as we can only cut blocks
        data[base] = Float.floatToRawIntBits((float)v.pos.x);
        data[base + 1] = Float.floatToRawIntBits((float)v.pos.y);
        data[base + 2] = Float.floatToRawIntBits((float)v.pos.z);

        data[base + 3] = v.color;

        data[base + 4] = Float.floatToRawIntBits(v.u);
        data[base + 5] = Float.floatToRawIntBits(v.v);

        data[base + 6] = v.light;

        data[base + 7] = (v.nx & 0xff) | ((v.ny & 0xff) << 8) | ((v.nz & 0xff) << 16);
    }
}