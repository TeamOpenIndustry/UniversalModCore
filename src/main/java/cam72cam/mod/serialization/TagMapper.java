package cam72cam.mod.serialization;

import cam72cam.mod.world.World;

import java.util.function.BiConsumer;
import java.util.function.Function;


public interface TagMapper<T> {
    TagAccessor<T> apply(Class<T> type, String fieldName, TagField tag) throws SerializationException;

    class TagAccessor<T> {
        final Serializer<Object> serializer;
        final Deserializer<T> deserializer;

        public TagAccessor(Serializer<T> serializer, Deserializer<T> deserializer) {
            this.serializer = (d, o) -> serializer.serialize(d, (T)o);
            this.deserializer = deserializer;
        }

        public TagAccessor(BiConsumer<TagCompound, T> serializer, Function<TagCompound, T> deserializer) {
            this.serializer = (d, o) -> serializer.accept(d, (T)o);
            this.deserializer = (d, w) -> deserializer.apply(d);
        }

        public boolean applyIfMissing() {
            return false;
        }
    }

    interface Serializer<T> {
        void serialize(TagCompound d, T o) throws SerializationException;
    }
    interface Deserializer<T> {
        T deserialize(TagCompound d, World w) throws SerializationException;
    }
}
