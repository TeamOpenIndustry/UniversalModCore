package cam72cam.mod.serialization;

import cam72cam.mod.world.World;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class TagSerializer {

    private static final Map<Class<? extends TagMapper>, TagMapper> mappers = new HashMap<>();
    private static final Map<Class<?>, Deserializer> deserializers = new HashMap<>();
    private static final Map<Class<?>, Serializer> serializers = new HashMap<>();

    @FunctionalInterface
    private interface Deserializer {
        SerializationException deserialize(TagCompound data, Object target, @Nullable World world);
    }

    @FunctionalInterface
    private interface Serializer {
        SerializationException serialize(TagCompound data, Object target);
    }

    private static TagMapper getMapper(Class<? extends TagMapper> mapCls) throws SerializationException {
        if (!mappers.containsKey(mapCls)) {
            try {
                mappers.put(mapCls, mapCls.newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                throw new SerializationException("Invalid type mapper: " + mapCls, e);
            }
        }
        return mappers.get(mapCls);
    }

    private static void init(Class<?> cls) throws SerializationException {
        if (deserializers.containsKey(cls) && serializers.containsKey(cls)) {
            return;
        }
        List<Deserializer> cachedDeserializers = new ArrayList<>();
        List<Serializer> cachedSerializers = new ArrayList<>();
        Field[] fields = cls.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            if (canAccess(field.getModifiers())) {
                TagField tag = field.getAnnotation(TagField.class);
                if (tag != null) {
                    String fieldName = tag.value().isEmpty() ? field.getName() : tag.value();
                    TagMapper mapper = getMapper(tag.mapper());
                    BiFunction<TagCompound, World, Object> deserializer = mapper.deserializer(field.getType(), fieldName, tag);
                    BiConsumer<TagCompound, Object> serializer = mapper.serializer(field.getType(), fieldName, tag);
                    cachedDeserializers.add((data, target, world) -> {
                        if (data.hasKey(fieldName)) {
                            try {
                                field.set(target, deserializer.apply(data, world));
                            } catch (IllegalAccessException e) {
                                return new SerializationException(String.format("Error decoding field %s in %s", fieldName, cls), e);
                            }
                        }
                        return null;
                    });
                    cachedSerializers.add((data, target) -> {
                        try {
                            serializer.accept(data, field.get(target));
                        } catch (IllegalAccessException e) {
                            return new SerializationException(String.format("Error encoding field %s in %s", fieldName, cls), e);
                        }
                        return null;
                    });
                }
            }
        }
        deserializers.put(cls, (data, target, world) -> {
            for (Deserializer d : cachedDeserializers) {
                SerializationException ex = d.deserialize(data, target, world);
                if (ex != null) {
                    // TODO agg serialization exception?
                    return ex;
                }
            }
            return null;
        });
        serializers.put(cls, (data, target) -> {
            for (Serializer d : cachedSerializers) {
                SerializationException ex = d.serialize(data, target);
                if (ex != null) {
                    // TODO agg serialization exception?
                    return ex;
                }
            }
            return null;
        });
    }

    public static void deserialize(TagCompound data, Object target) throws SerializationException {
        deserialize(data, target, null);
    }

    public static void deserialize(TagCompound data, Object target, @Nullable World world) throws SerializationException {
        init(target.getClass());
        SerializationException ex = deserializers.get(target.getClass()).deserialize(data, target, world);
        if (ex != null) {
            throw ex;
        }
    }

    public static void serialize(TagCompound data, Object target) throws SerializationException {
        init(target.getClass());
        SerializationException ex = serializers.get(target.getClass()).serialize(data, target);
        if (ex != null) {
            throw ex;
        }
    }

    private static boolean canAccess(int modifiers) {
        return !Modifier.isStatic(modifiers) && !Modifier.isFinal(modifiers);
    }
}
