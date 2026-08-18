package cam72cam.mod.model.common;

import cam72cam.mod.model.common.mesh.Model;
import cam72cam.mod.model.common.mesh.ModelGroup;
import cam72cam.mod.model.common.mesh.VAOLayout;
import cam72cam.mod.model.common.util.MalformedModelException;
import cam72cam.mod.model.obj.OBJGroup;
import cam72cam.mod.model.obj.OBJParser;
import cam72cam.mod.model.obj.VertexBuffer;
import cam72cam.mod.resource.Identifier;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class OBJParserTest {
    public static final AtomicInteger counter = new AtomicInteger(0);

    @BeforeClass
    public static void clearCache() throws IOException {
        FileUtils.deleteDirectory(new File(System.getProperty("java.io.tmpdir"), "cache/umccommon"));
    }

    private static class FakeIdentifier extends Identifier {
        @FunctionalInterface
        interface IOFunction {
            InputStream get(Identifier id) throws IOException;
        }

        private final IOFunction data;

        FakeIdentifier(String ident, IOFunction data) {
            super(ident);
            this.data = data;
        }

        @Override
        public InputStream getLastResourceStream() throws IOException {
            return data.get(this);
        }

        @Override
        public Identifier getRelative(String path) {
            return new FakeIdentifier(super.getRelative(path).toString(), data);
        }
    }

    private static Identifier obj(String obj) {
        return new FakeIdentifier("umccommon:test" + counter.getAndIncrement() + ".obj", loc ->
                loc.toString().endsWith("obj") ? new ByteArrayInputStream(obj.getBytes(StandardCharsets.UTF_8)) : null);
    }

    private static void assertValid(float[] data) {
        for (float f : data) {
            Assert.assertFalse("Non-finite value " + f + " in VBO", Float.isNaN(f) || Float.isInfinite(f));
        }
    }

    private static String defaultPos() {
        return "v 0 0 0\n" +
                "v 1 0 0\n" +
                "v 0 1 0\n";
    }

    @Test
    public void fullySpecifiedTriangle() throws Exception {
        Model model = ModelLoader.load(obj(defaultPos() +
                "vt 0 0\n" +
                "vt 1 0\n" +
                "vt 0 1\n" +
                "vn 0 0 1\n" +
                "vn 0 0 1\n" +
                "vn 0 0 1\n" +
                "o tri\n" +
                "f 1/1/1 2/2/2 3/3/3\n"));

        Assert.assertNotNull(model);
        Assert.assertEquals(VAOLayout.POS_TEX_COLOR_NORMAL.getStrideBytes(), model.getLayout().getStrideBytes());
        Assert.assertTrue(model.getLayout().has(VAOLayout.Usage.NORMAL));

        float[] data = model.getVboData();
        Assert.assertEquals(3 * 12, data.length); // 3 verts * (pos3 + uv2 + color4 + nrm3)

        // positions
        Assert.assertEquals(0, data[0], 0.001);
        Assert.assertEquals(0, data[1], 0.001);
        Assert.assertEquals(0, data[2], 0.001);
        Assert.assertEquals(1, data[12], 0.001);
        Assert.assertEquals(0, data[13], 0.001);
        Assert.assertEquals(0, data[14], 0.001);
        Assert.assertEquals(0, data[24], 0.001);
        Assert.assertEquals(1, data[25], 0.001);
        Assert.assertEquals(0, data[26], 0.001);

        // no mtl: default white baked into color
        Assert.assertEquals(1, data[5], 0.001);
        Assert.assertEquals(1, data[6], 0.001);
        Assert.assertEquals(1, data[7], 0.001);
        Assert.assertEquals(1, data[8], 0.001);

        // explicit normals
        Assert.assertEquals(0, data[9], 0.001);
        Assert.assertEquals(0, data[10], 0.001);
        Assert.assertEquals(1, data[11], 0.001);

        assertValid(data);
    }

    @Test
    public void quadTriangulates() throws Exception {
        Model model = ModelLoader.load(obj(
                "v 0 0 0\n" +
                "v 1 0 0\n" +
                "v 1 1 0\n" +
                "v 0 1 0\n" +
                "vt 0 0\n" +
                "vt 1 0\n" +
                "vt 1 1\n" +
                "vt 0 1\n" +
                "vn 0 0 1\n" +
                "vn 0 0 1\n" +
                "vn 0 0 1\n" +
                "vn 0 0 1\n" +
                "f 1/1/1 2/2/2 3/3/3 4/4/4\n"));

        float[] data = model.getVboData();
        Assert.assertEquals(6 * 12, data.length); // quad -> 2 triangles
    }

    @Test
    public void groups() throws Exception {
        Model model = ModelLoader.load(obj(defaultPos() +
                "o A\n" +
                "f 1 2 3\n" +
                "o B\n" +
                "f 1 2 3\n" +
                "f 1 2 3\n"));

        Assert.assertEquals(2, model.getGroups().size());
        Iterator<Map.Entry<String, ModelGroup>> iterator = model.getGroups().entrySet().iterator();
        ModelGroup firstGroup = iterator.next().getValue();
        ModelGroup secondGroup = iterator.next().getValue();
        Assert.assertEquals("A", firstGroup.name);
        Assert.assertEquals(0, firstGroup.faceStart);
        Assert.assertEquals(0, firstGroup.faceEnd);
        Assert.assertEquals("B", secondGroup.name);
        Assert.assertEquals(1, secondGroup.faceStart);
        Assert.assertEquals(2, secondGroup.faceEnd);
    }

    @Test
    public void missingUvAndNormal() throws Exception {
        Model model = ModelLoader.load(obj(defaultPos() +
                "f 1// 2// 3//\n"));

        Assert.assertFalse(model.getLayout().has(VAOLayout.Usage.NORMAL));
        Assert.assertEquals(VAOLayout.POS_TEX_COLOR.getStride(), model.getLayout().getStride());

        float[] data = model.getVboData();
        Assert.assertEquals(3 * 9, data.length); // pos3 + uv2 + color4, no normal

        // positions preserved
        Assert.assertEquals(0, data[0], 0.001);
        Assert.assertEquals(0, data[1], 0.001);
        Assert.assertEquals(0, data[2], 0.001);
        Assert.assertEquals(1, data[9], 0.001);
        Assert.assertEquals(0, data[10], 0.001);
        Assert.assertEquals(0, data[11], 0.001);
        Assert.assertEquals(0, data[18], 0.001);
        Assert.assertEquals(1, data[19], 0.001);
        Assert.assertEquals(0, data[20], 0.001);

        // default white color
        Assert.assertEquals(1, data[5], 0.001);
        Assert.assertEquals(1, data[6], 0.001);
        Assert.assertEquals(1, data[7], 0.001);
        Assert.assertEquals(1, data[8], 0.001);

        // UVs must be present (repacked 0.5 fallback) and finite
        assertValid(data);
    }

    @Test
    public void missingNormal() throws Exception {
        Model model = ModelLoader.load(obj(defaultPos() +
                "vt 0 0\n" +
                "vt 1 0\n" +
                "vt 0 1\n" +
                "f 1/1 2/2 3/3\n"));

        Assert.assertFalse(model.getLayout().has(VAOLayout.Usage.NORMAL));
        Assert.assertEquals(VAOLayout.POS_TEX_COLOR.getStride(), model.getLayout().getStride());

        float[] data = model.getVboData();
        Assert.assertEquals(3 * 9, data.length);
        Assert.assertEquals(0, data[0], 0.001);
        Assert.assertEquals(1, data[9], 0.001);
        Assert.assertEquals(0, data[18], 0.001);
        assertValid(data);
    }

    @Test
    public void missingUv() throws Exception {
        Model model = ModelLoader.load(obj(defaultPos() +
                "vn 0 0 1\n" +
                "vn 0 0 1\n" +
                "vn 0 0 1\n" +
                "f 1//1 2//2 3//3\n"));

        Assert.assertTrue(model.getLayout().has(VAOLayout.Usage.NORMAL));
        Assert.assertEquals(VAOLayout.POS_TEX_COLOR_NORMAL.getStride(), model.getLayout().getStride());

        float[] data = model.getVboData();
        Assert.assertEquals(3 * 12, data.length);

        // normals preserved
        Assert.assertEquals(0, data[9], 0.001);
        Assert.assertEquals(0, data[10], 0.001);
        Assert.assertEquals(1, data[11], 0.001);
        Assert.assertEquals(1, data[23], 0.001);

        assertValid(data);
    }

    @Test(expected = MalformedModelException.class)
    public void nanUvThrows() throws Exception {
        ModelLoader.load(obj(defaultPos() +
                "vt NaN NaN NaN\n" +
                "f 1/1 2/1 3/1\n"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingPositionThrows() throws Exception {
        ModelLoader.load(obj(defaultPos() +
                "vn 0 0 1\n" +
                "vn 0 0 1\n" +
                "vn 0 0 1\n" +
                "f //1 //2 //3\n"));
    }

    @Test
    public void degenerateFaceIgnored() throws Exception {
        Model model = ModelLoader.load(obj(defaultPos() +
                "f 1 2\n"));

        // No valid triangle was produced
        Assert.assertEquals(0, model.getVboData().length);
    }

    // ------------------------------------------------------------------
    // Backwards compatibility: identical vertex data to the legacy OBJ pipeline
    // ------------------------------------------------------------------

    @Test
    public void matchesOldVbo() throws Exception {
        // Only triangle faces, as triangulation is a separate concern, not part of this comparison
        StringBuilder objData = new StringBuilder();
        for (int i = 1; i <= 30; i++) {
            objData.append(String.format("v %d %d %d\n", i, i % 7, i % 5));
            objData.append(String.format("vt %f %f\n", i / 10f, i % 3 / 3f));
            objData.append(String.format("vn %d %d 1\n", i % 2, i % 3));
        }
        objData.append("o A\n");
        for (int i = 0; i < 30; i += 3) {
            int a = i + 1, b = i + 2, c = i + 3;
            objData.append(String.format("f %d/%d/%d %d/%d/%d %d/%d/%d\n", a, a, a, b, b, b, c, c, c));
            if (i == 15) {
                objData.append("o B\n");
            }
        }
        String data = objData.toString();

        // Old pipeline as a black box: raw VBO geometry (positions/uvs/normals)
        OBJParser oldParser = new OBJParser(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)), 1.0f);
        VertexBuffer oldVbo = oldParser.getBuffer();

        // New pipeline
        Model model = ModelLoader.load(obj(data));

        int stride = oldVbo.stride;
        Assert.assertEquals(stride, model.getLayout().getStride());
        Assert.assertEquals(oldVbo.data.length, model.getVboData().length);

        // Vertex data (skipping uv & color) as they're both POS_UV_COLOR_NORMAL
        for (int v = 0; v < oldVbo.data.length / stride; v++) {
            for (int c = 0; c < stride; c++) {
                if (c >= oldVbo.textureOffset && c < oldVbo.textureOffset + 4 || c >= oldVbo.colorOffset && c < oldVbo.colorOffset + 4) {
                    continue;
                }
                Assert.assertEquals("vertex " + v + " component " + c, oldVbo.data[v * stride + c], model.getVboData()[v * stride + c], 1e-5);
            }
        }
        Iterator<Map.Entry<String, ModelGroup>> iterator = model.getGroups().entrySet().iterator();
        ModelGroup A2 = iterator.next().getValue();
        ModelGroup B2 = iterator.next().getValue();

        //Group data
        OBJGroup A1 = oldParser.getGroups().get(0);
        Assert.assertEquals(A1.min, A2.min());
        Assert.assertEquals(A1.max, A2.max());
        Assert.assertEquals(A1.normal, A2.normal());

        OBJGroup B1 = oldParser.getGroups().get(1);
        Assert.assertEquals(B1.min, B2.min());
        Assert.assertEquals(B1.max, B2.max());
        Assert.assertEquals(B1.normal, B2.normal());
    }
}
