package cam72cam.mod.model.obj;

import cam72cam.mod.resource.Identifier;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

//@RunWith(Parameterized.class)
public class OBJModelTest {

    private static Identifier objId;
    private static Identifier objId2;
    private static final int vertsPerGroup = 1000;
    private static final int facesPerGroup = 1000;
    private static final int nGroups = 100;
    private static OBJModel model;

    static class FakeIdentifier extends Identifier {
        @FunctionalInterface
        interface IOFunction {
            InputStream get(Identifier id) throws IOException;
        }

        private final IOFunction data;

        public FakeIdentifier(String ident, IOFunction data) {
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

    @BeforeClass
    public static void setup() throws Exception {
        FileUtils.deleteDirectory(new File(System.getProperty("java.io.tmpdir"), "cache"));
/*
        File testObj = File.createTempFile("temp", "obj");
        testObj.deleteOnExit();

        List<String> lines = new ArrayList<>();
        lines.add("# Test File");

        for (int g = 1; g <= nGroups; g++) {
            lines.add(String.format("o group_%d", g));
            for (int v = 1; v <= vertsPerGroup; v++) {
                lines.add(String.format("v %s %s %s", (v + 0.1f)/g, (v + 0.2f)/g, (v + 0.3f)/g));
            }
            for (int vn = 1; vn <= vertsPerGroup; vn++) {
                lines.add(String.format("vn %s %s %s", (vn + 0.1f)/g, (vn + 0.2f)/g, (vn + 0.3f)/g));
            }
            for (int vt = 1; vt <= vertsPerGroup; vt++) {
                lines.add(String.format("vt %s %s", (vt + 0.1f)/g, (vt + 0.2f)/g));
            }
            for (int f = 1; f < facesPerGroup; f++) {
                lines.add(String.format("f %1$s/%1$s/%1$s %1$s/%1$s/%1$s %1$s/%1$s/%1$s", f*g));
            }
        }
        System.out.println(lines.size());

        Files.write(testObj.toPath(), lines, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
*/
        objId = new FakeIdentifier(
                "immersiverailroading:models/rolling_stock/locomotives/emd_sw1500/emd_sw1500.obj",
                loc -> new FileInputStream("/home/cmesh/Games/Minecraft/ImmersiveRailroading/github/ImmersiveRailroading/src/main/resources/assets/" + loc.getDomain() + "/" + loc.getPath())
        );
       // model = new OBJModel(objId, 1.0f, 1.0, Arrays.asList("", "blackwhitestriped", "blackred", "blueblack", "yellowblack", "greenblack"));

        objId2 = new FakeIdentifier(
                "immersiverailroading:models/rolling_stock/locomotives/big_boy/big_boy2.obj",
                loc -> new FileInputStream("/home/cmesh/Games/Minecraft/ImmersiveRailroading/github/ImmersiveRailroading/src/main/resources/assets/" + loc.getDomain() + "/" + loc.getPath())
        );
        //model = new OBJModel(objId2, 1.0f, 1.0, Collections.singletonList(""));
    }

    //@Parameterized.Parameters
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {{}, {}, {}, {}});
    }
/*
    @Test
    public void saveObjModelOther() throws Exception {
        for (int i = 0; i < 100; i++) {
            model = new OBJModel(objId, 1.0f, 1.0, Arrays.asList("", "blackwhitestriped", "blackred", "blueblack", "yellowblack", "greenblack"));
            model.vbo.get();
            model.textures.get("").get();
        }
    }*/


    @Test
    public void testVertexObject() throws Exception {
        StringBuilder objData = new StringBuilder();
        for (int i = 1; i <= 30; i++) {
            objData.append(String.format("v %s %s %s\n", i / 1f, i / 2f, i / 3f));
        }
        objData.append("o A\n");
        for (int i = 0; i < 30; i+=3) {
            objData.append(String.format("f %s %s %s\n", i + 1, i + 2, i + 3));
            if (i == 15) {
                objData.append("o B\n");
            }
        }
        String finalObjData = objData.toString();
        objId = new FakeIdentifier(
                "umc:simplev.obj",
                loc -> loc.toString().endsWith("obj") ? new ByteArrayInputStream(finalObjData.getBytes(StandardCharsets.UTF_8)) : null
        );
        model = new OBJModel(objId, 1.0f, 1.0, null);
        float[] data = model.vbo.get().floats();
        //System.out.println(Arrays.toString(data));

        int vertOff = 0;
        int floatStride = 9;
        for (int i = 1; i <= 30; i++) {
            Assert.assertEquals(data[vertOff], i/1f, 0.0001);
            Assert.assertEquals(data[vertOff+1], i/2f, 0.0001);
            Assert.assertEquals(data[vertOff+2], i/3f, 0.0001);
            vertOff += floatStride;
        }
    }
    @Test
    public void testNormalObject() throws Exception {
        StringBuilder objData = new StringBuilder();
        for (int i = 1; i <= 30; i++) {
            objData.append(String.format("v %s %s %s\n", i * 100 / 1f, i * 100 / 2f, i * 100 / 3f));
        }
        for (int i = 1; i <= 30; i++) {
            objData.append(String.format("vn %s %s %s\n", i / 1f, i / 2f, i / 3f));
        }
        objData.append("o A\n");
        for (int i = 0; i < 30; i+=3) {
            objData.append(String.format("f %1$s//%1$s %2$s//%2$s %3$s//%3$s\n", i + 1, i + 2, i + 3));
            if (i == 15) {
                objData.append("o B\n");
            }
        }
        String finalObjData = objData.toString();
        objId = new FakeIdentifier(
                "umc:simplevvn.obj",
                loc -> loc.toString().endsWith("obj") ? new ByteArrayInputStream(finalObjData.getBytes(StandardCharsets.UTF_8)) : null
        );
        model = new OBJModel(objId, 1.0f, 1.0, null);
        float[] data = model.vbo.get().floats();
        //System.out.println(Arrays.toString(data));

        int vertOff = 0;
        int normalOff = 9;
        int floatStride = 12;
        for (int i = 1; i <= 30; i++) {
            Assert.assertEquals(data[vertOff], i*100/1f, 0.0001);
            Assert.assertEquals(data[vertOff+1], i*100/2f, 0.0001);
            Assert.assertEquals(data[vertOff+2], i*100/3f, 0.0001);
            Assert.assertEquals(data[normalOff], i/1f, 0.0001);
            Assert.assertEquals(data[normalOff+1], i/2f, 0.0001);
            Assert.assertEquals(data[normalOff+2], i/3f, 0.0001);
            vertOff += floatStride;
            normalOff += floatStride;
        }
    }

    @Test
    public void testTexturedObject() throws Exception {
        StringBuilder objData = new StringBuilder();
        for (int i = 1; i <= 30; i++) {
            objData.append(String.format("v %s %s %s\n", i * 100 / 1f, i * 100 / 2f, i * 100 / 3f));
        }
        for (int i = 1; i <= 30; i++) {
            objData.append(String.format("vn %s %s %s\n", i / 1f, i / 2f, i / 3f));
        }
        for (int i = 1; i <= 30; i++) {
            objData.append(String.format("vt %s %s\n", 0, 0));
        }
        for (int i = 1; i <= 30; i++) {
            objData.append(String.format("vt %s %s\n", -i / 1f, -i / 2f));
        }
        objData.append("o A\n");
        for (int i = 0; i < 30; i+=3) {
            objData.append(String.format("f %1$s/%4$s/%1$s %2$s/%5$s/%2$s %3$s/%6$s/%3$s\n", i + 1, i + 2, i + 3, i + 31, i+32, i+33));
            if (i == 15) {
                objData.append("o B\n");
            }
        }
        String finalObjData = objData.toString();
        objId = new FakeIdentifier(
                "umc:simplevvtvn.obj",
                loc -> loc.toString().endsWith("obj") ? new ByteArrayInputStream(finalObjData.getBytes(StandardCharsets.UTF_8)) : null
        );
        model = new OBJModel(objId, 1.0f, 1.0, null);
        float[] data = model.vbo.get().floats();
        //System.out.println(Arrays.toString(data));

        int vertOff = 0;
        int texOffset = 3;
        int normalOff = 9;
        int floatStride = 12;
        for (int i = 1; i <= 30; i++) {
            Assert.assertEquals(data[vertOff], i*100/1f, 0.0001);
            Assert.assertEquals(data[vertOff+1], i*100/2f, 0.0001);
            Assert.assertEquals(data[vertOff+2], i*100/3f, 0.0001);
            Assert.assertEquals(data[normalOff], i/1f, 0.0001);
            Assert.assertEquals(data[normalOff+1], i/2f, 0.0001);
            Assert.assertEquals(data[normalOff+2], i/3f, 0.0001);
            Assert.assertEquals(data[texOffset], -i/1f, 0.0001);
            Assert.assertEquals(data[texOffset+1], -i/2f, 0.0001);
            vertOff += floatStride;
            normalOff += floatStride;
            texOffset += floatStride;
        }
    }
}