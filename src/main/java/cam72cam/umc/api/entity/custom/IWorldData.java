package cam72cam.umc.api.entity.custom;

import cam72cam.umc.api.serialization.TagCompound;

public interface IWorldData {
    IWorldData NOP = new IWorldData() {
        @Override
        public void load(TagCompound data) {

        }

        @Override
        public void save(TagCompound data) {

        }
    };

    static IWorldData get(Object o) {
        if (o instanceof IWorldData) {
            return (IWorldData) o;
        }
        return IWorldData.NOP;
    }

    /** World Load */
    void load(TagCompound data);

    /** World Save */
    void save(TagCompound data);
}
