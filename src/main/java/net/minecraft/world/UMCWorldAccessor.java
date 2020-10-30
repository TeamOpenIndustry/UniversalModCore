package net.minecraft.world;

public class UMCWorldAccessor {
    public static World world(IBlockAccess access) {
        if (access instanceof World) {
            return (World) access;
        }
        if (access instanceof ChunkCache) {
            return ((ChunkCache) access).world;
        }
        throw new RuntimeException(String.format("Unknown world accessor %s", access));
    }
}
