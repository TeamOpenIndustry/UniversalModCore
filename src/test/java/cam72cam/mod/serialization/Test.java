package cam72cam.mod.serialization;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Facing;
import org.junit.jupiter.api.Assertions;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Test {
    @TagField
    private boolean booleanF;
    @TagField
    private Boolean booleanO;

    @org.junit.jupiter.api.Test
    public void booleans() throws SerializationException {
        TagCompound data = new TagCompound();
        Test t = new Test();

        TagSerializer.deserialize(data, t);
        Assertions.assertFalse(t.booleanF);
        Assertions.assertNull(t.booleanO);

        t.booleanF = true;
        t.booleanO = true;

        TagSerializer.serialize(data, t);
        Assertions.assertEquals(data.getBoolean("booleanF"), t.booleanF, data.toString());
        Assertions.assertEquals(data.getBoolean("booleanO"), t.booleanO, data.toString());

        t.booleanO = null;
        TagSerializer.serialize(data, t);
        Assertions.assertEquals(data.getBoolean("booleanF"), t.booleanF, data.toString());
        Assertions.assertEquals(data.getBoolean("booleanO"), t.booleanO, data.toString());

        data = new TagCompound();
        TagSerializer.serialize(data, t);
        Assertions.assertEquals(t.booleanF, data.getBoolean("booleanF"), data.toString());
        Assertions.assertEquals(t.booleanO, data.getBoolean("booleanO"), data.toString());
    }

    @TagField
    private byte byteF;
    @TagField
    private Byte byteO;

    @org.junit.jupiter.api.Test
    public void bytes() throws SerializationException {
        TagCompound data = new TagCompound();
        Test t = new Test();

        TagSerializer.deserialize(data, t);
        Assertions.assertEquals(t.byteF, 0);
        Assertions.assertNull(t.byteO);

        t.byteF = 1;
        t.byteO = 10;

        TagSerializer.serialize(data, t);
        Assertions.assertEquals(data.getByte("byteF"), (Byte)t.byteF, data.toString());
        Assertions.assertEquals(data.getByte("byteO"), t.byteO, data.toString());

        t.byteO = null;
        TagSerializer.serialize(data, t);
        Assertions.assertEquals(data.getByte("byteF"), (Byte)t.byteF, data.toString());
        Assertions.assertEquals(data.getByte("byteO"), t.byteO, data.toString());

        data = new TagCompound();
        TagSerializer.serialize(data, t);
        Assertions.assertEquals((Byte)t.byteF, data.getByte("byteF"), data.toString());
        Assertions.assertEquals(t.byteO, data.getByte("byteO"), data.toString());
    }

    @TagField
    private int intF;
    @TagField
    private Integer intO;

    @org.junit.jupiter.api.Test
    public void ints() throws SerializationException {
        TagCompound data = new TagCompound();
        Test t = new Test();

        TagSerializer.deserialize(data, t);
        Assertions.assertEquals(t.intF, 0);
        Assertions.assertNull(t.intO);

        t.intF = 1;
        t.intO = 10;

        TagSerializer.serialize(data, t);
        Assertions.assertEquals(data.getInteger("intF"), (Integer)t.intF, data.toString());
        Assertions.assertEquals(data.getInteger("intO"), t.intO, data.toString());

        t.intO = null;
        TagSerializer.serialize(data, t);
        Assertions.assertEquals(data.getInteger("intF"), (Integer)t.intF, data.toString());
        Assertions.assertEquals(data.getInteger("intO"), t.intO, data.toString());

        data = new TagCompound();
        TagSerializer.serialize(data, t);
        Assertions.assertEquals((Integer)t.intF, data.getInteger("intF"), data.toString());
        Assertions.assertEquals(t.intO, data.getInteger("intO"), data.toString());
    }

    @TagField
    private long longF;
    @TagField
    private Long longO;

    @org.junit.jupiter.api.Test
    public void longs() throws SerializationException {
        TagCompound data = new TagCompound();
        Test t = new Test();

        TagSerializer.deserialize(data, t);
        Assertions.assertEquals(t.longF, 0);
        Assertions.assertNull(t.longO);

        t.longF = 1;
        t.longO = 10L;

        TagSerializer.serialize(data, t);
        Assertions.assertEquals(data.getLong("longF"), (Long)t.longF, data.toString());
        Assertions.assertEquals(data.getLong("longO"), t.longO, data.toString());

        t.longO = null;
        TagSerializer.serialize(data, t);
        Assertions.assertEquals(data.getLong("longF"), (Long)t.longF, data.toString());
        Assertions.assertEquals(data.getLong("longO"), t.longO, data.toString());

        data = new TagCompound();
        TagSerializer.serialize(data, t);
        Assertions.assertEquals((Long)t.longF, data.getLong("longF"), data.toString());
        Assertions.assertEquals(t.longO, data.getLong("longO"), data.toString());
    }

    @TagField
    private float floatF;
    @TagField
    private Float floatO;

    @org.junit.jupiter.api.Test
    public void floats() throws SerializationException {
        TagCompound data = new TagCompound();
        Test t = new Test();

        TagSerializer.deserialize(data, t);
        Assertions.assertEquals(t.floatF, 0f, 0f);
        Assertions.assertNull(t.floatO);

        t.floatF = 1;
        t.floatO = 10f;

        TagSerializer.serialize(data, t);
        Assertions.assertEquals(data.getFloat("floatF"), (Float)t.floatF, data.toString());
        Assertions.assertEquals(data.getFloat("floatO"), t.floatO, data.toString());

        t.floatO = null;
        TagSerializer.serialize(data, t);
        Assertions.assertEquals(data.getFloat("floatF"), (Float)t.floatF, data.toString());
        Assertions.assertEquals(data.getFloat("floatO"), t.floatO, data.toString());

        data = new TagCompound();
        TagSerializer.serialize(data, t);
        Assertions.assertEquals((Float)t.floatF, data.getFloat("floatF"), data.toString());
        Assertions.assertEquals(t.floatO, data.getFloat("floatO"), data.toString());
    }

    @TagField
    private double doubleF;
    @TagField
    private Double doubleO;

    @org.junit.jupiter.api.Test
    public void doubles() throws SerializationException {
        TagCompound data = new TagCompound();
        Test t = new Test();

        TagSerializer.deserialize(data, t);
        Assertions.assertEquals(t.doubleF, 0, 0.0);
        Assertions.assertNull(t.doubleO);

        t.doubleF = 1;
        t.doubleO = 10.0;

        TagSerializer.serialize(data, t);
        Assertions.assertEquals(data.getDouble("doubleF"), (Double)t.doubleF, data.toString());
        Assertions.assertEquals(data.getDouble("doubleO"), t.doubleO, data.toString());

        t.doubleO = null;
        TagSerializer.serialize(data, t);
        Assertions.assertEquals(data.getDouble("doubleF"), (Double)t.doubleF, data.toString());
        Assertions.assertEquals(data.getDouble("doubleO"), t.doubleO, data.toString());

        data = new TagCompound();
        TagSerializer.serialize(data, t);
        Assertions.assertEquals((Double)t.doubleF, data.getDouble("doubleF"), data.toString());
        Assertions.assertEquals(t.doubleO, data.getDouble("doubleO"), data.toString());
    }

    @TagField
    private String stringO;

    @org.junit.jupiter.api.Test
    public void strings() throws SerializationException {
        TagCompound data = new TagCompound();
        Test t = new Test();

        TagSerializer.deserialize(data, t);
        Assertions.assertNull(t.stringO);

        t.stringO = "10";

        TagSerializer.serialize(data, t);
        Assertions.assertEquals(data.getString("stringO"), t.stringO, data.toString());

        t.stringO = null;
        TagSerializer.serialize(data, t);
        Assertions.assertEquals(data.getString("stringO"), t.stringO, data.toString());

        data = new TagCompound();
        TagSerializer.serialize(data, t);
        Assertions.assertEquals(t.stringO, data.getString("stringO"), data.toString());
    }

    @TagField
    private UUID uuidO;

    @org.junit.jupiter.api.Test
    public void uuids() throws SerializationException {
        TagCompound data = new TagCompound();
        Test t = new Test();

        TagSerializer.deserialize(data, t);
        Assertions.assertNull(t.uuidO);

        t.uuidO = UUID.randomUUID();

        TagSerializer.serialize(data, t);
        Assertions.assertEquals(data.getUUID("uuidO"), t.uuidO, data.toString());

        t.uuidO = null;
        TagSerializer.serialize(data, t);
        Assertions.assertEquals(data.getUUID("uuidO"), t.uuidO, data.toString());

        data = new TagCompound();
        TagSerializer.serialize(data, t);
        Assertions.assertEquals(t.uuidO, data.getUUID("uuidO"), data.toString());

        data = new TagCompound();
        t.uuidO = UUID.randomUUID();
        data.internal.putUUID("uuidO", t.uuidO);
        Assertions.assertEquals(t.uuidO, data.getUUID("uuidO"), data.toString());
    }

    @TagField
    private Vec3i Vec3iO;

    @org.junit.jupiter.api.Test
    public void Vec3is() throws SerializationException {
        TagCompound data = new TagCompound();
        Test t = new Test();

        TagSerializer.deserialize(data, t);
        Assertions.assertNull(t.Vec3iO);

        t.Vec3iO = new Vec3i(10,20,30);

        TagSerializer.serialize(data, t);
        Assertions.assertEquals(data.getVec3i("Vec3iO"), t.Vec3iO, data.toString());

        t.Vec3iO = null;
        TagSerializer.serialize(data, t);
        Assertions.assertEquals(data.getVec3i("Vec3iO"), t.Vec3iO, data.toString());

        data = new TagCompound();
        TagSerializer.serialize(data, t);
        Assertions.assertEquals(t.Vec3iO, data.getVec3i("Vec3iO"), data.toString());

        t.Vec3iO = new Vec3i(40, 50, 60);
        data.internal.putLong("Vec3iO", t.Vec3iO.toLong());
        Assertions.assertEquals(t.Vec3iO, data.getVec3i("Vec3iO"), data.toString());
    }

    @TagField
    private Vec3d Vec3dO;

    @org.junit.jupiter.api.Test
    public void Vec3ds() throws SerializationException {
        TagCompound data = new TagCompound();
        Test t = new Test();

        TagSerializer.deserialize(data, t);
        Assertions.assertNull(t.Vec3dO);

        t.Vec3dO = new Vec3d(10,20,30);

        TagSerializer.serialize(data, t);
        Assertions.assertEquals(data.getVec3d("Vec3dO"), t.Vec3dO, data.toString());

        t.Vec3dO = null;
        TagSerializer.serialize(data, t);
        Assertions.assertEquals(data.getVec3d("Vec3dO"), t.Vec3dO, data.toString());

        data = new TagCompound();
        TagSerializer.serialize(data, t);
        Assertions.assertEquals(t.Vec3dO, data.getVec3d("Vec3dO"), data.toString());
    }

    @TagField
    private Facing facing;

    @org.junit.jupiter.api.Test
    public void facing() throws SerializationException {
        TagCompound data = new TagCompound();
        Test t = new Test();

        TagSerializer.serialize(data, t);
        TagSerializer.deserialize(data, t);
        Assertions.assertNull(t.facing);

        data.setEnum("facing", Facing.EAST);
        TagSerializer.deserialize(data, t);
        TagSerializer.serialize(data, t);
        TagSerializer.deserialize(data, t);
        Assertions.assertEquals(Facing.EAST, t.facing);
    }

    @TagField(typeHint=Facing.class)
    public List<Facing> facingList;

    @org.junit.jupiter.api.Test
    public void facingList() throws SerializationException {
        TagCompound data = new TagCompound();
        Test t = new Test();

        Assertions.assertThrows(SerializationException.class, () -> TagSerializer.deserialize(data, new Test() {
            @TagField
            public List<Facing> badEnum;
        }));

        TagSerializer.serialize(data, t);
        TagSerializer.deserialize(data, t);
        Assertions.assertNull(t.facingList);

        t.facingList = new ArrayList<>();
        TagSerializer.deserialize(data, t);
        TagSerializer.serialize(data, t);
        TagSerializer.deserialize(data, t);
        Assertions.assertEquals(t.facingList, data.getEnumList("facingList", Facing.class));

        t.facingList.add(Facing.NORTH);
        t.facingList.add(Facing.WEST);
        TagSerializer.serialize(data, t);
        t = new Test();
        TagSerializer.deserialize(data, t);
        Assertions.assertEquals(t.facingList, data.getEnumList("facingList", Facing.class));
        Assertions.assertEquals(t.facingList.get(0), Facing.NORTH);
        Assertions.assertEquals(t.facingList.get(1), Facing.WEST);
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

    @org.junit.jupiter.api.Test
    public void genList() throws SerializationException {
        TagCompound data = new TagCompound();
        Test t = new Test();

        Assertions.assertThrows(SerializationException.class, () -> TagSerializer.deserialize(data, new Test() {
            @TagField
            public List<Integer> badgeneric;
        }));

        Assertions.assertThrows(SerializationException.class, () -> TagSerializer.deserialize(data, new Test() {
            @TagField(typeHint = Integer.class)
            public List<Integer> badgeneric;
        }));

        TagSerializer.serialize(data, t);
        TagSerializer.deserialize(data, t);
        Assertions.assertNull(t.genList);

        t.genList = new ArrayList<>();
        TagSerializer.serialize(data, t);
        TagSerializer.deserialize(data, t);
        Assertions.assertEquals(t.genList.size(), 0, data.toString());

        t.genList.add(53);
        t.genList.add(683);
        TagSerializer.serialize(data, t);
        t = new Test();
        TagSerializer.deserialize(data, t);
        Assertions.assertEquals(t.genList.get(0), (Integer)53);
        Assertions.assertEquals(t.genList.get(1), (Integer)683);
    }

    @TagField("sub")
    Test subObject;

    @org.junit.jupiter.api.Test
    public void testSubObjects() throws SerializationException {
        TagCompound data = new TagCompound();

        Test t = new Test();
        t.subObject = t;

        Assertions.assertThrows(SerializationException.class, () -> TagSerializer.serialize(data, t));

        t.facing = Facing.EAST;
        t.subObject = new Test();
        t.subObject.facing = Facing.NORTH;

        TagSerializer.serialize(data, t);

        Test o = new Test();

        TagSerializer.deserialize(data, o);

        Assertions.assertEquals(t.facing, o.facing);
        Assertions.assertEquals(t.subObject.facing, o.subObject.facing);
    }

    @TagField
    private final Integer hidden = 0;

    @org.junit.jupiter.api.Test
    public void testHidden() throws SerializationException {
        TagCompound data = new TagCompound();
        Test t = new Test();

        data.setInteger("hidden", 55);

        TagSerializer.deserialize(data, t);
        Assertions.assertEquals(t.hidden, (Integer)55);
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

    @org.junit.jupiter.api.Test
    public void customMapped() throws SerializationException {
        TagCompound data = new TagCompound();

        Test t = new Test();
        t.cc = new CustomClass(1);
        TagSerializer.serialize(data, t);
        TagSerializer.deserialize(data, t);
        Assertions.assertEquals(t.cc.field, (Integer)10);

    }

}
