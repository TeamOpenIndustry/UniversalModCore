package cam72cam.mod.serialization;

import cam72cam.mod.world.World;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;

/** Main deserialization API */
public class TagSerializer {

    private static final Map<Class<? extends TagMapper>, TagMapper> mappers = new HashMap<>();
    private static final Map<Class<?>, Deserializer> deserializers = new HashMap<>();
    private static final Map<Class<?>, Serializer> serializers = new HashMap<>();

    @FunctionalInterface
    private interface Deserializer {
        SerializationException deserialize(TagCompound data, Object target, @Nullable World world, Class<? extends Annotation>[] filter);
    }

    @FunctionalInterface
    private interface Serializer {
        SerializationException serialize(TagCompound data, Object target, Class<? extends Annotation>[] filter);
    }

    static TagMapper getMapper(Class<? extends TagMapper> mapCls) throws SerializationException {
        if (!mappers.containsKey(mapCls)) {
            try {
                Constructor<? extends TagMapper> ctr = mapCls.getDeclaredConstructor();
                ctr.setAccessible(true);
                mappers.put(mapCls, ctr.newInstance());
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
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
        List<Field> fields = new ArrayList<>();
        Class<?> parent = cls;
        while(parent != null && parent != Object.class) {
            fields.addAll(Arrays.asList(parent.getDeclaredFields()));
            parent = parent.getSuperclass();
        }
        for (Field field : fields) {
            field.setAccessible(true);
            if (!Modifier.isStatic(field.getModifiers())) {
                TagField tag = field.getAnnotation(TagField.class);
                if (tag != null) {
                    String fieldName = tag.value().isEmpty() ? field.getName() : tag.value();

                    if (Modifier.isFinal(field.getModifiers())) {
                        try {
                            Field modifiersField = Field.class.getDeclaredField("modifiers");
                            modifiersField.setAccessible(true);
                            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
                        } catch (NoSuchFieldException | IllegalAccessException e) {
                            throw new SerializationException(String.format("Unable to access field %s in class %s", fieldName, cls), e);
                        }
                    }

                    TagMapped mapped = field.getType().getAnnotation(TagMapped.class);

                    TagMapper mapper = getMapper(tag.mapper().equals(DefaultTagMapper.class) && mapped != null ? mapped.value() : tag.mapper());

                    TagMapper.TagAccessor<?> accessor = mapper.apply(field.getType(), fieldName, tag);
                    cachedDeserializers.add((data, target, world, filter) -> {
                        if (accessor.applyIfMissing() || data.hasKey(fieldName)) {
                            if (filter != null && filter.length != 0) {
                                for (Class<? extends Annotation> annotation : filter) {
                                    if (field.getAnnotation(annotation) == null) {
                                        return null;
                                    }
                                }
                            }

                            try {
                                field.set(target, accessor.deserializer.deserialize(data, world));
                            } catch (IllegalAccessException | SerializationException e) {
                                return new SerializationException(String.format("Error decoding field %s in %s", fieldName, cls), e);
                            }
                        }
                        return null;
                    });
                    cachedSerializers.add((data, target, filter) -> {
                        try {
                            if (filter != null && filter.length != 0) {
                                for (Class<? extends Annotation> annotation : filter) {
                                    if (field.getAnnotation(annotation) == null) {
                                        return null;
                                    }
                                }
                            }

                            Object o = field.get(target);
                            accessor.serializer.serialize(data, o);
                        } catch (IllegalAccessException | SerializationException | StackOverflowError e) {
                            return new SerializationException(String.format("Error encoding field %s in %s", fieldName, cls), e);
                        }
                        return null;
                    });
                }
            }
        }
        deserializers.put(cls, (data, target, world, filter) -> {
            for (Deserializer d : cachedDeserializers) {
                SerializationException ex = d.deserialize(data, target, world, filter);
                if (ex != null) {
                    // TODO agg serialization exception?
                    return ex;
                }
            }
            return null;
        });
        serializers.put(cls, (data, target, filter) -> {
            for (Serializer d : cachedSerializers) {
                SerializationException ex = d.serialize(data, target, filter);
                if (ex != null) {
                    // TODO agg serialization exception?
                    return ex;
                }
            }
            return null;
        });
    }

    /** Look for @TagFields in target and try to match with fields in data */
    public static void deserialize(TagCompound data, Object target) throws SerializationException {
        deserialize(data, target, null);
    }

    /** Look for @TagFields in target and try to match with fields in data.  Filter based on filter annotations (default no filter) */
    @SafeVarargs
    public static void deserialize(TagCompound data, Object target, @Nullable World world, Class<? extends Annotation>... filter) throws SerializationException {
        init(target.getClass());
        SerializationException ex = deserializers.get(target.getClass()).deserialize(data, target, world, filter);
        if (ex != null) {
            throw ex;
        }
    }

    /** Look for @TagFields in target and turn them into TagCompounds to be added to data.  Filter based on filter annotations (default no filter) */
    @SafeVarargs
    public static void serialize(TagCompound data, Object target, Class<? extends Annotation>... filter) throws SerializationException {
        init(target.getClass());
        SerializationException ex = serializers.get(target.getClass()).serialize(data, target, filter);
        if (ex != null) {
            throw ex;
        }
    }
}
