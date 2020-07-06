package cam72cam.mod.entity;


import cam72cam.mod.serialization.TagCompound;

public interface IAdditionalSpawnData {
    void readSpawnData(TagCompound data, float yaw, float pitch);
    void writeSpawnData(TagCompound data);
}
