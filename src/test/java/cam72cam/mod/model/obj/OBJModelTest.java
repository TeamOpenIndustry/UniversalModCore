package cam72cam.mod.model.obj;

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

public class OBJModelTest {
    @BeforeClass
    public static void setup() throws Exception {
        FileUtils.deleteDirectory(new File(System.getProperty("java.io.tmpdir"), "cache"));
    }


    private static class FakeIdentifier extends Identifier {
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
        FakeIdentifier objId = new FakeIdentifier(
                "umc:simplev.obj",
                loc -> loc.toString().endsWith("obj") ? new ByteArrayInputStream(finalObjData.getBytes(StandardCharsets.UTF_8)) : null
        );
        OBJModel model = new OBJModel(objId, 1.0f, 1.0, null);
        float[] data = model.vbo.buffer.get().data;
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
        FakeIdentifier objId = new FakeIdentifier(
                "umc:simplevvn.obj",
                loc -> loc.toString().endsWith("obj") ? new ByteArrayInputStream(finalObjData.getBytes(StandardCharsets.UTF_8)) : null
        );
        OBJModel model = new OBJModel(objId, 1.0f, 1.0, null);
        float[] data = model.vbo.buffer.get().data;
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
        FakeIdentifier objId = new FakeIdentifier(
                "umc:simplevvtvn.obj",
                loc -> loc.toString().endsWith("obj") ? new ByteArrayInputStream(finalObjData.getBytes(StandardCharsets.UTF_8)) : null
        );
        OBJModel model = new OBJModel(objId, 1.0f, 1.0, null);
        float[] data = model.vbo.buffer.get().data;
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