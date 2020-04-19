package cam72cam.mod.serialization;

public class StrictTagMapper extends DefaultTagMapper {
    @Override
    public TagAccessor apply(Class type, String fieldName, TagField tag) throws SerializationException {
        TagAccessor std = super.apply(type, fieldName, tag);
        return new TagAccessor(std.serializer, std.deserializer) {
            @Override
            public boolean applyIfMissing() {
                return true;
            }
        };
    }
}
