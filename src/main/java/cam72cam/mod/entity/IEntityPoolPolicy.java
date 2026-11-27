package cam72cam.mod.entity;

public interface IEntityPoolPolicy {
    /** Whether the pool is enabled (corresponds to mod config). */
    boolean isEnabled();

    /** Whether this entity should be managed by the pool. */
    boolean shouldPool(CustomEntity self);

    /** Squared distance to spawn/pool the entity (e.g., 128 * 128). */
    double getSpawnDistanceSq();

    /** Squared distance to despawn/unpool the entity (e.g., 160 * 160). */
    double getDespawnDistanceSq();

    /** Scan interval in ticks. */
    int getProximityInterval();

    /** Called when the entity is spawned into the world or just loaded. */
    default void onEntitySpawned(ModdedEntity e) {}
}