package cam72cam.mod.serialization;

import cam72cam.mod.entity.Entity;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.world.World;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.UUID;

class DefaultTagMapper implements TagMapper {
    @Override
    public TagAccessor apply(Class type, String fieldName, TagField tag) throws SerializationException {
        if (type == Boolean.class || type == boolean.class) {
            return new TagAccessor<>((d, o) -> d.setBoolean(fieldName, o), d -> d.getBoolean(fieldName));
        }
        if (type == Byte.class || type == byte.class) {
            return new TagAccessor<>((d, o) -> d.setByte(fieldName, o), d -> d.getByte(fieldName));
        }
        if (type == Integer.class || type == int.class) {
            return new TagAccessor<>((d, o) -> d.setInteger(fieldName, o), d -> d.getInteger(fieldName));
        }
        if (type == Long.class || type == long.class) {
            return new TagAccessor<>((d, o) -> d.setLong(fieldName, o), d -> d.getLong(fieldName));
        }
        if (type == Float.class || type == float.class) {
            return new TagAccessor<>((d, o) -> d.setFloat(fieldName, o), d -> d.getFloat(fieldName));
        }
        if (type == Double.class || type == double.class) {
            return new TagAccessor<>((d, o) -> d.setDouble(fieldName, o), d -> d.getDouble(fieldName));
        }
        if (type == String.class) {
            return new TagAccessor<>((d, o) -> d.setString(fieldName, o), d -> d.getString(fieldName));
        }
        if (type == UUID.class) {
            return new TagAccessor<>((d, o) -> d.setUUID(fieldName, o), d -> d.getUUID(fieldName));
        }
        if (type.isEnum()) {
            return new TagAccessor<>(
                    (d, o) -> d.setEnum(fieldName, o),
                    d -> d.getEnum(fieldName, (Class<? extends Enum<?>>)type)
            );
        }
        if (List.class.isAssignableFrom(type)) {
            Class<?> subType = tag.typeHint() != Object.class ? tag.typeHint() : type.getComponentType();
            if (subType == null) {
                throw new SerializationException(String.format("Unable to determine the type of List for field %s.  Hint: for enums use typeHint in the @TagField", fieldName));
            }

            if (subType.isEnum()) {
                return new TagAccessor<>(
                        (d, o) -> d.setEnumList(fieldName, o),
                        d -> d.getEnumList(fieldName, (Class<? extends Enum<?>>)subType)
                );
            }
            throw new SerializationException("Unable to decode generic lists, please use a custom TagMapper");
        }
        if (Vec3i.class.isAssignableFrom(type)) {
            return new TagAccessor<>((d, o) -> d.setVec3i(fieldName, o), d -> d.getVec3i(fieldName));
        }
        if (Vec3d.class.isAssignableFrom(type)) {
            return new TagAccessor<>((d, o) -> d.setVec3d(fieldName, o), d -> d.getVec3d(fieldName));
        }
        if (Entity.class.isAssignableFrom(type)) {
            return new TagAccessor<>((d, o) -> d.setEntity(fieldName, o), (d, w) -> d.getEntity(fieldName, w));
        }
        if (World.class.isAssignableFrom(type)) {
            return new TagAccessor<>((d, o) -> d.setWorld(fieldName, o), (d, w) -> d.getWorld(fieldName, w.isClient));
        }
        if (ItemStack.class.isAssignableFrom(type)) {
            return new TagAccessor<>((d, o) -> d.setStack(fieldName, o), (d, w) -> d.getStack(fieldName));
        }
        if (TagCompound.class.isAssignableFrom(type)) {
            return new TagAccessor<>((d, o) -> d.set(fieldName, o), d -> d.get(fieldName));
        }
        if (Object.class.isAssignableFrom(type)) {
            try {
                Constructor<?> ctr = type.getDeclaredConstructor();
                ctr.setAccessible(true);
                // Make sure construction works...
                ctr.newInstance();
                return new TagAccessor<>(
                        (d, o) -> {
                            if (o == null) {
                                d.remove(fieldName);
                                return;
                            }

                            TagCompound sub = new TagCompound();
                            TagSerializer.serialize(sub, o);
                            d.set(fieldName, sub);
                        },
                        (d, w) -> {
                            try {
                                Object o = ctr.newInstance();
                                TagSerializer.deserialize(d.get(fieldName), o);
                                return o;
                            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                                throw new SerializationException(String.format("Unable to construct type %s for field %s during deserialization", type, fieldName), e);
                            }
                        }
                );
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new SerializationException(String.format("Unable to construct type %s for field %s", type, fieldName), e);
            }
        }
        throw new SerializationException(String.format("Invalid type %s for field %s", type, fieldName));
    }
}
