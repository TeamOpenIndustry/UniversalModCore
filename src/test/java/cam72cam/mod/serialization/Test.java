package cam72cam.mod.serialization;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.render.OpenGL;
import cam72cam.mod.util.Facing;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class Test {
    @TagField
    private boolean booleanF;
    @TagField
    private Boolean booleanO;

    @org.junit.Test
    public void timeA() {
        int k = 0;
        for (int i = 0; i < 10_000_000; i++) {
            try (OpenGL.With w = () -> {}) {
                k++;
            }
        }
    }
    @org.junit.Test
    public void timeB() {
        for (int i = 0; i < 10_000_000; i++) {
            Consumer<Integer> fn = (k) -> {
                k++;
            };
            fn.accept(i);
        }
    }

    @org.junit.Test
    public void booleans() throws SerializationException {
        TagCompound data = new TagCompound();
        Test t = new Test();

        TagSerializer.deserialize(data, t);
        Assert.assertFalse(t.booleanF);
        Assert.assertNull(t.booleanO);

        t.booleanF = true;
        t.booleanO = true;

        TagSerializer.serialize(data, t);
        Assert.assertEquals(data.toString(), data.getBoolean("booleanF"), t.booleanF);
        Assert.assertEquals(data.toString(), data.getBoolean("booleanO"), t.booleanO);

        t.booleanO = null;
        TagSerializer.serialize(data, t);
        Assert.assertEquals(data.toString(), data.getBoolean("booleanF"), t.booleanF);
        Assert.assertEquals(data.toString(), data.getBoolean("booleanO"), t.booleanO);

        data = new TagCompound();
        TagSerializer.serialize(data, t);
        Assert.assertEquals(data.toString(), t.booleanF, data.getBoolean("booleanF"));
        Assert.assertEquals(data.toString(), t.booleanO, data.getBoolean("booleanO"));
    }

    @TagField
    private byte byteF;
    @TagField
    private Byte byteO;

    @org.junit.Test
    public void bytes() throws SerializationException {
        TagCompound data = new TagCompound();
        Test t = new Test();

        TagSerializer.deserialize(data, t);
        Assert.assertEquals(t.byteF, 0);
        Assert.assertNull(t.byteO);

        t.byteF = 1;
        t.byteO = 10;

        TagSerializer.serialize(data, t);
        Assert.assertEquals(data.toString(), data.getByte("byteF"), (Byte)t.byteF);
        Assert.assertEquals(data.toString(), data.getByte("byteO"), t.byteO);

        t.byteO = null;
        TagSerializer.serialize(data, t);
        Assert.assertEquals(data.toString(), data.getByte("byteF"), (Byte)t.byteF);
        Assert.assertEquals(data.toString(), data.getByte("byteO"), t.byteO);

        data = new TagCompound();
        TagSerializer.serialize(data, t);
        Assert.assertEquals(data.toString(), (Byte)t.byteF, data.getByte("byteF"));
        Assert.assertEquals(data.toString(), t.byteO, data.getByte("byteO"));
    }

    @TagField
    private int intF;
    @TagField
    private Integer intO;

    @org.junit.Test
    public void ints() throws SerializationException {
        TagCompound data = new TagCompound();
        Test t = new Test();

        TagSerializer.deserialize(data, t);
        Assert.assertEquals(t.intF, 0);
        Assert.assertNull(t.intO);

        t.intF = 1;
        t.intO = 10;

        TagSerializer.serialize(data, t);
        Assert.assertEquals(data.toString(), data.getInteger("intF"), (Integer)t.intF);
        Assert.assertEquals(data.toString(), data.getInteger("intO"), t.intO);

        t.intO = null;
        TagSerializer.serialize(data, t);
        Assert.assertEquals(data.toString(), data.getInteger("intF"), (Integer)t.intF);
        Assert.assertEquals(data.toString(), data.getInteger("intO"), t.intO);

        data = new TagCompound();
        TagSerializer.serialize(data, t);
        Assert.assertEquals(data.toString(), (Integer)t.intF, data.getInteger("intF"));
        Assert.assertEquals(data.toString(), t.intO, data.getInteger("intO"));
    }

    @TagField
    private long longF;
    @TagField
    private Long longO;

    @org.junit.Test
    public void longs() throws SerializationException {
        TagCompound data = new TagCompound();
        Test t = new Test();

        TagSerializer.deserialize(data, t);
        Assert.assertEquals(t.longF, 0);
        Assert.assertNull(t.longO);

        t.longF = 1;
        t.longO = 10L;

        TagSerializer.serialize(data, t);
        Assert.assertEquals(data.toString(), data.getLong("longF"), (Long)t.longF);
        Assert.assertEquals(data.toString(), data.getLong("longO"), t.longO);

        t.longO = null;
        TagSerializer.serialize(data, t);
        Assert.assertEquals(data.toString(), data.getLong("longF"), (Long)t.longF);
        Assert.assertEquals(data.toString(), data.getLong("longO"), t.longO);

        data = new TagCompound();
        TagSerializer.serialize(data, t);
        Assert.assertEquals(data.toString(), (Long)t.longF, data.getLong("longF"));
        Assert.assertEquals(data.toString(), t.longO, data.getLong("longO"));
    }

    @TagField
    private float floatF;
    @TagField
    private Float floatO;

    @org.junit.Test
    public void floats() throws SerializationException {
        TagCompound data = new TagCompound();
        Test t = new Test();

        TagSerializer.deserialize(data, t);
        Assert.assertEquals(t.floatF, 0f, 0f);
        Assert.assertNull(t.floatO);

        t.floatF = 1;
        t.floatO = 10f;

        TagSerializer.serialize(data, t);
        Assert.assertEquals(data.toString(), data.getFloat("floatF"), (Float)t.floatF);
        Assert.assertEquals(data.toString(), data.getFloat("floatO"), t.floatO);

        t.floatO = null;
        TagSerializer.serialize(data, t);
        Assert.assertEquals(data.toString(), data.getFloat("floatF"), (Float)t.floatF);
        Assert.assertEquals(data.toString(), data.getFloat("floatO"), t.floatO);

        data = new TagCompound();
        TagSerializer.serialize(data, t);
        Assert.assertEquals(data.toString(), (Float)t.floatF, data.getFloat("floatF"));
        Assert.assertEquals(data.toString(), t.floatO, data.getFloat("floatO"));
    }

    @TagField
    private double doubleF;
    @TagField
    private Double doubleO;

    @org.junit.Test
    public void doubles() throws SerializationException {
        TagCompound data = new TagCompound();
        Test t = new Test();

        TagSerializer.deserialize(data, t);
        Assert.assertEquals(t.doubleF, 0, 0.0);
        Assert.assertNull(t.doubleO);

        t.doubleF = 1;
        t.doubleO = 10.0;

        TagSerializer.serialize(data, t);
        Assert.assertEquals(data.toString(), data.getDouble("doubleF"), (Double)t.doubleF);
        Assert.assertEquals(data.toString(), data.getDouble("doubleO"), t.doubleO);

        t.doubleO = null;
        TagSerializer.serialize(data, t);
        Assert.assertEquals(data.toString(), data.getDouble("doubleF"), (Double)t.doubleF);
        Assert.assertEquals(data.toString(), data.getDouble("doubleO"), t.doubleO);

        data = new TagCompound();
        TagSerializer.serialize(data, t);
        Assert.assertEquals(data.toString(), (Double)t.doubleF, data.getDouble("doubleF"));
        Assert.assertEquals(data.toString(), t.doubleO, data.getDouble("doubleO"));
    }

    @TagField
    private String stringO;

    @org.junit.Test
    public void strings() throws SerializationException {
        TagCompound data = new TagCompound();
        Test t = new Test();

        TagSerializer.deserialize(data, t);
        Assert.assertNull(t.stringO);

        t.stringO = "10";

        TagSerializer.serialize(data, t);
        Assert.assertEquals(data.toString(), data.getString("stringO"), t.stringO);

        t.stringO = null;
        TagSerializer.serialize(data, t);
        Assert.assertEquals(data.toString(), data.getString("stringO"), t.stringO);

        data = new TagCompound();
        TagSerializer.serialize(data, t);
        Assert.assertEquals(data.toString(), t.stringO, data.getString("stringO"));
    }

    @TagField
    private UUID uuidO;

    @org.junit.Test
    public void uuids() throws SerializationException {
        TagCompound data = new TagCompound();
        Test t = new Test();

        TagSerializer.deserialize(data, t);
        Assert.assertNull(t.uuidO);

        t.uuidO = UUID.randomUUID();

        TagSerializer.serialize(data, t);
        Assert.assertEquals(data.toString(), data.getUUID("uuidO"), t.uuidO);

        t.uuidO = null;
        TagSerializer.serialize(data, t);
        Assert.assertEquals(data.toString(), data.getUUID("uuidO"), t.uuidO);

        data = new TagCompound();
        TagSerializer.serialize(data, t);
        Assert.assertEquals(data.toString(), t.uuidO, data.getUUID("uuidO"));

        data = new TagCompound();
        t.uuidO = UUID.randomUUID();
        data.internal.setUniqueId("uuidO", t.uuidO);
        Assert.assertEquals(data.toString(), t.uuidO, data.getUUID("uuidO"));
    }

    @TagField
    private Vec3i Vec3iO;

    @org.junit.Test
    public void Vec3is() throws SerializationException {
        TagCompound data = new TagCompound();
        Test t = new Test();

        TagSerializer.deserialize(data, t);
        Assert.assertNull(t.Vec3iO);

        t.Vec3iO = new Vec3i(10,20,30);

        TagSerializer.serialize(data, t);
        Assert.assertEquals(data.toString(), data.getVec3i("Vec3iO"), t.Vec3iO);

        t.Vec3iO = null;
        TagSerializer.serialize(data, t);
        Assert.assertEquals(data.toString(), data.getVec3i("Vec3iO"), t.Vec3iO);

        data = new TagCompound();
        TagSerializer.serialize(data, t);
        Assert.assertEquals(data.toString(), t.Vec3iO, data.getVec3i("Vec3iO"));

        t.Vec3iO = new Vec3i(40, 50, 60);
        data.internal.setLong("Vec3iO", t.Vec3iO.toLong());
        Assert.assertEquals(data.toString(), t.Vec3iO, data.getVec3i("Vec3iO"));
    }

    @TagField
    private Vec3d Vec3dO;

    @org.junit.Test
    public void Vec3ds() throws SerializationException {
        TagCompound data = new TagCompound();
        Test t = new Test();

        TagSerializer.deserialize(data, t);
        Assert.assertNull(t.Vec3dO);

        t.Vec3dO = new Vec3d(10,20,30);

        TagSerializer.serialize(data, t);
        Assert.assertEquals(data.toString(), data.getVec3d("Vec3dO"), t.Vec3dO);

        t.Vec3dO = null;
        TagSerializer.serialize(data, t);
        Assert.assertEquals(data.toString(), data.getVec3d("Vec3dO"), t.Vec3dO);

        data = new TagCompound();
        TagSerializer.serialize(data, t);
        Assert.assertEquals(data.toString(), t.Vec3dO, data.getVec3d("Vec3dO"));
    }

    @TagField
    private Facing facing;

    @org.junit.Test
    public void facing() throws SerializationException {
        TagCompound data = new TagCompound();
        Test t = new Test();

        TagSerializer.serialize(data, t);
        TagSerializer.deserialize(data, t);
        Assert.assertNull(t.facing);

        data.setEnum("facing", Facing.EAST);
        TagSerializer.deserialize(data, t);
        TagSerializer.serialize(data, t);
        TagSerializer.deserialize(data, t);
        Assert.assertEquals(Facing.EAST, t.facing);
    }

    @TagField(typeHint=Facing.class)
    public List<Facing> facingList;

    @org.junit.Test
    public void facingList() throws SerializationException {
        TagCompound data = new TagCompound();
        Test t = new Test();

        Assert.assertThrows(SerializationException.class, () -> TagSerializer.deserialize(data, new Test() {
            @TagField
            public List<Facing> badEnum;
        }));

        TagSerializer.serialize(data, t);
        TagSerializer.deserialize(data, t);
        Assert.assertNull(t.facingList);

        t.facingList = new ArrayList<>();
        TagSerializer.deserialize(data, t);
        TagSerializer.serialize(data, t);
        TagSerializer.deserialize(data, t);
        Assert.assertEquals(t.facingList, data.getEnumList("facingList", Facing.class));

        t.facingList.add(Facing.NORTH);
        t.facingList.add(Facing.WEST);
        TagSerializer.serialize(data, t);
        t = new Test();
        TagSerializer.deserialize(data, t);
        Assert.assertEquals(t.facingList, data.getEnumList("facingList", Facing.class));
        Assert.assertEquals(t.facingList.get(0), Facing.NORTH);
        Assert.assertEquals(t.facingList.get(1), Facing.WEST);
    }

    @TagField(mapper=IntListMapper.class)
    public List<Integer> genList;

    private static class IntListMapper implements TagMapper<List<Integer>> {
        @Override
        public TagAccessor<List<Integer>> apply(Class<List<Integer>> type, String fieldName, TagField tag) {
            return new TagAccessor<>(
                (d, o) -> d.setList(fieldName, o, v -> new TagCompound().setInteger("value", (Integer)v)),
                d -> d.getList(fieldName, c -> c.getInteger("value"))
            );
        }
    }

    @org.junit.Test
    public void genList() throws SerializationException {
        TagCompound data = new TagCompound();
        Test t = new Test();

        Assert.assertThrows(SerializationException.class, () -> TagSerializer.deserialize(data, new Test() {
            @TagField
            public List<Integer> badgeneric;
        }));

        Assert.assertThrows(SerializationException.class, () -> TagSerializer.deserialize(data, new Test() {
            @TagField(typeHint = Integer.class)
            public List<Integer> badgeneric;
        }));

        TagSerializer.serialize(data, t);
        TagSerializer.deserialize(data, t);
        Assert.assertNull(t.genList);

        t.genList = new ArrayList<>();
        TagSerializer.serialize(data, t);
        TagSerializer.deserialize(data, t);
        Assert.assertEquals(data.toString(), t.genList.size(), 0);

        t.genList.add(53);
        t.genList.add(683);
        TagSerializer.serialize(data, t);
        t = new Test();
        TagSerializer.deserialize(data, t);
        Assert.assertEquals(t.genList.get(0), (Integer)53);
        Assert.assertEquals(t.genList.get(1), (Integer)683);
    }

    @TagField("sub")
    Test subObject;

    @org.junit.Test
    public void testSubObjects() throws SerializationException {
        TagCompound data = new TagCompound();

        Test t = new Test();
        t.subObject = t;

        Assert.assertThrows(SerializationException.class, () -> TagSerializer.serialize(data, t));

        t.facing = Facing.EAST;
        t.subObject = new Test();
        t.subObject.facing = Facing.NORTH;

        TagSerializer.serialize(data, t);

        Test o = new Test();

        TagSerializer.deserialize(data, o);

        Assert.assertEquals(t.facing, o.facing);
        Assert.assertEquals(t.subObject.facing, o.subObject.facing);
    }

    @TagField
    private final Integer hidden = 0;

    @org.junit.Test
    public void testHidden() throws SerializationException {
        TagCompound data = new TagCompound();
        Test t = new Test();

        data.setInteger("hidden", 55);

        TagSerializer.deserialize(data, t);
        Assert.assertEquals(t.hidden, (Integer)55);
    }

    private static class CustomMapper implements TagMapper<CustomClass> {
        @Override
        public TagAccessor<CustomClass> apply(Class<CustomClass> type, String fieldName, TagField tag) throws SerializationException {
            return new TagAccessor<>(
                    (d, o) -> {
                        if (o != null) {
                            d.setInteger(fieldName, o.field * 10);
                        }
                    },
                    d -> new CustomClass(d.getInteger(fieldName))
            );
        }
    }

    @TagMapped(CustomMapper.class)
    public static class CustomClass {
        private final Integer field;

        public CustomClass(Integer integer) {
            this.field = integer;
        }
    }

    @TagField
    CustomClass cc;

    @org.junit.Test
    public void customMapped() throws SerializationException {
        TagCompound data = new TagCompound();

        Test t = new Test();
        t.cc = new CustomClass(1);
        TagSerializer.serialize(data, t);
        TagSerializer.deserialize(data, t);
        Assert.assertEquals(t.cc.field, (Integer)10);

    }

}
