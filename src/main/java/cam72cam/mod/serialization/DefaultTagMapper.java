package cam72cam.mod.serialization;

import cam72cam.mod.entity.Entity;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.world.World;

import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public final class DefaultTagMapper implements TagMapper {
    public BiFunction<TagCompound, World, Object> deserializer(Class<?> type, String fieldName, TagField tag) throws SerializationException {
        if (type == Boolean.class || type == boolean.class) {
            return (d, w) -> d.getBoolean(fieldName);
        }
        if (type == Byte.class || type == byte.class) {
            return (d, w) -> d.getByte(fieldName);
        }
        if (type == Integer.class || type == int.class) {
            return (d, w) -> d.getInteger(fieldName);
        }
        if (type == Long.class || type == long.class) {
            return (d, w) -> d.getLong(fieldName);
        }
        if (type == Float.class || type == float.class) {
            return (d, w) -> d.getFloat(fieldName);
        }
        if (type == Double.class || type == double.class) {
            return (d, w) -> d.getDouble(fieldName);
        }
        if (type == String.class) {
            return (d, w) -> d.getString(fieldName);
        }
        if (type == UUID.class) {
            return (d, w) -> d.getUUID(fieldName);
        }
        if (type.isEnum()) {
            return (d, w) -> d.getEnum(fieldName, (Class<? extends Enum<?>>)type);
        }
        if (List.class.isAssignableFrom(type)) {
            Class<?> subType = tag.typeHint() != Object.class ? tag.typeHint() : type.getComponentType();
            if (subType == null) {
                throw new SerializationException(String.format("Unable to determine the type of List for field %s.  Hint: for enums use typeHint in the @TagField", fieldName));
            }

            if (subType.isEnum()) {
                return (d, w) -> d.getEnumList(fieldName, (Class<? extends Enum<?>>)subType);
            }
            throw new SerializationException("Unable to decode generic lists, please use a custom TagMapper");
        }
        if (Vec3i.class.isAssignableFrom(type)) {
            return (d, w) -> d.getVec3i(fieldName);
        }
        if (Vec3d.class.isAssignableFrom(type)) {
            return (d, w) -> d.getVec3d(fieldName);
        }
        if (Entity.class.isAssignableFrom(type)) {
            return (d, w) -> d.getEntity(fieldName, w);
        }
        if (TagCompound.class.isAssignableFrom(type)) {
            return (d, w) -> d.get(fieldName);
        }
        throw new SerializationException(String.format("Invalid type %s for field %s", type, fieldName));
    }

    @Override
    public BiConsumer<TagCompound, Object> serializer(Class<?> type, String fieldName, TagField tag) throws SerializationException {
        if (type == Boolean.class || type == boolean.class) {
            return (d, o) -> d.setBoolean(fieldName, (Boolean)o);
        }
        if (type == Byte.class || type == byte.class) {
            return (d, o) -> d.setByte(fieldName, (Byte)o);
        }
        if (type == Integer.class || type == int.class) {
            return (d, o) -> d.setInteger(fieldName, (Integer)o);
        }
        if (type == Long.class || type == long.class) {
            return (d, o) -> d.setLong(fieldName, (Long)o);
        }
        if (type == Float.class || type == float.class) {
            return (d, o) -> d.setFloat(fieldName, (Float)o);
        }
        if (type == Double.class || type == double.class) {
            return (d, o) -> d.setDouble(fieldName, (Double)o);
        }
        if (type == String.class) {
            return (d, o) -> d.setString(fieldName, (String)o);
        }
        if (type == UUID.class) {
            return (d, o) -> d.setUUID(fieldName, (UUID)o);
        }
        if (type.isEnum()) {
            return (d, o) -> d.setEnum(fieldName, (Enum<?>)o);
        }
        if (List.class.isAssignableFrom(type)) {
            Class<?> subType = tag.typeHint() != Object.class ? tag.typeHint() : type.getComponentType();
            if (subType == null) {
                throw new SerializationException(String.format("Unable to determine the type of List for field %s.  Hint: for enums use typeHint in the @TagField", fieldName));
            }

            if (subType.isEnum()) {
                return (d, o) -> d.setEnumList(fieldName, (List<Enum<?>>)o);
            }
            throw new SerializationException("Unable to decode generic lists, please use a custom TagMapper");
        }
        if (Vec3i.class.isAssignableFrom(type)) {
            return (d, o) -> d.setVec3i(fieldName, (Vec3i)o);
        }
        if (Vec3d.class.isAssignableFrom(type)) {
            return (d, o) -> d.setVec3d(fieldName, (Vec3d)o);
        }
        if (Entity.class.isAssignableFrom(type)) {
            return (d, o) -> d.setEntity(fieldName, (Entity)o);
        }
        if (TagCompound.class.isAssignableFrom(type)) {
            return (d, o) -> d.set(fieldName, (TagCompound)o);
        }
        throw new SerializationException(String.format("Invalid type %s for field %s", type, fieldName));
    }
}
