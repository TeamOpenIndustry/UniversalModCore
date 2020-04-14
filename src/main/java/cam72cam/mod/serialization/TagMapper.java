package cam72cam.mod.serialization;

import cam72cam.mod.world.World;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public interface TagMapper {
    BiFunction<TagCompound, World, Object> deserializer(Class<?> type, String fieldName, TagField tag) throws SerializationException;
    BiConsumer<TagCompound, Object> serializer(Class<?> type, String fieldName, TagField tag) throws SerializationException;
}
