package cam72cam.mod.render.cutter.adapter;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.cutter.ClipVertex;
import cam72cam.mod.render.cutter.Plane;
import cam72cam.mod.render.cutter.Polygon;
import cam72cam.mod.render.cutter.PolygonQuadBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.block.model.BakedQuad;

import java.util.ArrayList;
import java.util.List;

public class BakedQuadAdapter
        implements PrimitiveAdapter<BakedQuad, QuadTemplate> {

    private static final int STRIDE = DefaultVertexFormat.BLOCK.getVertexSize() / 4;

    @Override
    public QuadTemplate createTemplate(
            List<BakedQuad> quads,
            Plane plane) {

        if (quads.isEmpty()) {
            return null;
        }

        BakedQuad quad = quads.get(0);

        return new QuadTemplate(
                quad.getSprite(),
                BakedQuadDirectionUtil.fromNormal(
                        plane.normal.scale(-1)
                ),
                quad.getTintIndex(),
                quad.isShade(),
                quad.hasAmbientOcclusion()
        );
    }

    @Override
    public Polygon toPolygon(BakedQuad quad) {

        Polygon polygon = new Polygon();

        int[] data = quad.getVertices();

        for (int i = 0; i < 4; i++) {
            polygon.vertices.add(
                    readVertex(data, i)
            );
        }

        return polygon;
    }

    @Override
    public List<BakedQuad> fromPrimitive(
            Polygon polygon,
            BakedQuad primitive) {

        List<BakedQuad> result = new ArrayList<>();

        if (polygon.vertices.size() < 3) {
            return result;
        }

        for (Polygon quad : PolygonQuadBuilder.build(polygon)) {

            int[] data = primitive.getVertices().clone();

            writeVertex(data, 0, quad.vertices.get(0));
            writeVertex(data, 1, quad.vertices.get(1));
            writeVertex(data, 2, quad.vertices.get(2));
            writeVertex(data, 3, quad.vertices.get(3));

            result.add(new BakedQuad(
                    data,
                    primitive.getTintIndex(),
                    primitive.getDirection(),
                    primitive.getSprite(),
                    primitive.isShade(),
                    primitive.hasAmbientOcclusion()
            ));
        }

        return result;
    }

    @Override
    public List<BakedQuad> fromTemplate(
            Polygon polygon,
            QuadTemplate template) {

        List<BakedQuad> result = new ArrayList<>();

        if (polygon.vertices.size() < 3) {
            return result;
        }

        for (Polygon quad : PolygonQuadBuilder.build(polygon)) {

            int[] data = new int[STRIDE * 4];

            writeVertex(data, 0, quad.vertices.get(0));
            writeVertex(data, 1, quad.vertices.get(1));
            writeVertex(data, 2, quad.vertices.get(2));
            writeVertex(data, 3, quad.vertices.get(3));

            result.add(new BakedQuad(
                    data,
                    template.tintIndex,
                    template.direction,
                    template.sprite,
                    template.shade,
                    template.ambientOcclusion
            ));
        }

        return result;
    }

    @Override
    public void prepareCap(
            Polygon polygon,
            Plane plane,
            QuadTemplate template) {

        CapUVGenerator.generate(
                polygon,
                plane,
                template.sprite
        );
    }

    private static ClipVertex readVertex(
            int[] data,
            int index) {

        int base = index * STRIDE;

        float x = Float.intBitsToFloat(data[base]);
        float y = Float.intBitsToFloat(data[base + 1]);
        float z = Float.intBitsToFloat(data[base + 2]);

        int color = data[base + 3];

        float u = Float.intBitsToFloat(data[base + 4]);
        float v = Float.intBitsToFloat(data[base + 5]);

        int light = data[base + 6];

        int packed = data[base + 7];

        byte nx = (byte) packed;
        byte ny = (byte) (packed >> 8);
        byte nz = (byte) (packed >> 16);

        return new ClipVertex(
                new Vec3d(x, y, z),
                u,
                v,
                color,
                light,
                nx,
                ny,
                nz
        );
    }

    private static void writeVertex(
            int[] data,
            int index,
            ClipVertex v) {

        int base = index * STRIDE;

        data[base] =
                Float.floatToRawIntBits((float) v.pos.x);
        data[base + 1] =
                Float.floatToRawIntBits((float) v.pos.y);
        data[base + 2] =
                Float.floatToRawIntBits((float) v.pos.z);

        data[base + 3] = v.color;

        data[base + 4] =
                Float.floatToRawIntBits(v.u);
        data[base + 5] =
                Float.floatToRawIntBits(v.v);

        data[base + 6] = v.light;

        data[base + 7] =
                (v.nx & 0xff)
                        | ((v.ny & 0xff) << 8)
                        | ((v.nz & 0xff) << 16);
    }
}