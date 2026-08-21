package cam72cam.mod.model.common;

import cam72cam.mod.model.common.mesh.Model;
import cam72cam.mod.model.common.mesh.ModelGroup;
import cam72cam.mod.model.common.mesh.VAOLayout;
import cam72cam.mod.model.common.util.MalformedModelException;
import cam72cam.mod.resource.Identifier;
import cpw.mods.modlauncher.Launcher;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLLoader;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class OBJParserTest {
    public static final AtomicInteger counter = new AtomicInteger(0);

    @BeforeClass
    public static void clearCache() throws Exception {
        FileUtils.deleteDirectory(new File(System.getProperty("java.io.tmpdir"), "cache/umccommon"));

        Constructor<Launcher> ctr = Launcher.class.getDeclaredConstructor();
        ctr.setAccessible(true);
        ctr.newInstance();

        Field dist = FMLLoader.class.getDeclaredField("dist");
        dist.setAccessible(true);
        dist.set(null, Dist.CLIENT);
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

        // Position
        Assert.assertEquals(0, data[0], 0.001);
        Assert.assertEquals(0, data[1], 0.001);
        Assert.assertEquals(0, data[2], 0.001);
        Assert.assertEquals(1, data[12], 0.001);
        Assert.assertEquals(0, data[13], 0.001);
        Assert.assertEquals(0, data[14], 0.001);
        Assert.assertEquals(0, data[24], 0.001);
        Assert.assertEquals(1, data[25], 0.001);
        Assert.assertEquals(0, data[26], 0.001);

        // White as no Kd specified
        Assert.assertEquals(1, data[5], 0.001);
        Assert.assertEquals(1, data[6], 0.001);
        Assert.assertEquals(1, data[7], 0.001);
        Assert.assertEquals(1, data[8], 0.001);

        // Normals
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
        Assert.assertEquals(6 * VAOLayout.POS_TEX_COLOR_NORMAL.getStride(), data.length); // Triangulated
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
        // Inclusive, 0-based
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

        // Positions
        Assert.assertEquals(0, data[0], 0.001);
        Assert.assertEquals(0, data[1], 0.001);
        Assert.assertEquals(0, data[2], 0.001);
        Assert.assertEquals(1, data[9], 0.001);
        Assert.assertEquals(0, data[10], 0.001);
        Assert.assertEquals(0, data[11], 0.001);
        Assert.assertEquals(0, data[18], 0.001);
        Assert.assertEquals(1, data[19], 0.001);
        Assert.assertEquals(0, data[20], 0.001);

        // UV defaults to 0.5
        for (int i = 0; i < 3; i ++) {
            int idx = i * VAOLayout.POS_TEX_COLOR.getStride() +
                    VAOLayout.POS_TEX_COLOR.getOffset(VAOLayout.Usage.UV);

            Assert.assertEquals(0.5, data[idx], 1E-8);
            Assert.assertEquals(0.5, data[idx + 1], 1E-8);
        }

        // Color is white
        Assert.assertEquals(1, data[5], 0.001);
        Assert.assertEquals(1, data[6], 0.001);
        Assert.assertEquals(1, data[7], 0.001);
        Assert.assertEquals(1, data[8], 0.001);

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
        Assert.assertEquals(3 * VAOLayout.POS_TEX_COLOR.getStride(), data.length);
        // Don't have normal in VBO
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
        Assert.assertEquals(3 * VAOLayout.POS_TEX_COLOR_NORMAL.getStride(), data.length);

        // Missing UVs have 0.5 by default
        for (int i = 0; i < 3; i ++) {
            int idx = i * VAOLayout.POS_TEX_COLOR_NORMAL.getStride() +
                    VAOLayout.POS_TEX_COLOR_NORMAL.getOffset(VAOLayout.Usage.UV);

            Assert.assertEquals(0.5, data[idx], 1E-8);
            Assert.assertEquals(0.5, data[idx + 1], 1E-8);
        }

        assertValid(data);
    }

    @Test(expected = MalformedModelException.class)
    public void nanUvThrows() throws Exception {
        ModelLoader.load(obj(defaultPos() +
                "vt NaN NaN \n" +
                "f 1/1 2/1 3/1\n"));
    }

    @Test(expected = MalformedModelException.class)
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
}
