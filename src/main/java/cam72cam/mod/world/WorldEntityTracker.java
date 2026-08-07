package cam72cam.mod.world;

import cam72cam.mod.entity.ModdedEntity;

import java.util.*;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Track UMC Entities and handle inter chunk collision
 * <p>
 * Internal, don't use outside UMC!
 * @see cam72cam.mod.mixin.feat.large_entity_collision.MixinVanillaWorld
 */
//TODO We need fastutil!
public class WorldEntityTracker {
    //Good enough for now...We'd assume there's no more ridiculous ones
    //Horizontal distance (48 blocks)
    private static final int HORIZONTAL_SEARCH_RADIUS_CHUNKS = 3;
    //Vertical distance (32 blocks)
    private static final int VERTICAL_SEARCH_RADIUS_CHUNKS = 2;
    private final Map<Long, Set<WeakReference<ModdedEntity>>> umcEntities = new HashMap<>();
    //K are chunks containing UMC entities, V are neighbor chunks(245 per now) that this entity may extend to
    //Query value to see which chunk may contain possible colliding entities
    private final LongBiMultiMap scanningRange = new LongBiMultiMap();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public WorldEntityTracker() {}

    public void join(ModdedEntity entity) {
        long chunk = ChunkPos.asLongExcludeY(entity.getBoundingBox());
        int x = ChunkPos.x(chunk);
        int y = ChunkPos.y(chunk);
        int z = ChunkPos.z(chunk);

        lock.writeLock().lock();
        try {
            Set<WeakReference<ModdedEntity>> moddedEntities = umcEntities.get(chunk);
            if (moddedEntities == null) {
                moddedEntities = new HashSet<>();
                umcEntities.put(chunk, moddedEntities);

                for (int i = x - HORIZONTAL_SEARCH_RADIUS_CHUNKS; i <= x + HORIZONTAL_SEARCH_RADIUS_CHUNKS; i++) {
                    for (int j = z - HORIZONTAL_SEARCH_RADIUS_CHUNKS; j <= z + HORIZONTAL_SEARCH_RADIUS_CHUNKS; j++) {
                        //Handle Y below 1.17 is likely to cause more bug
                        scanningRange.put(chunk, ChunkPos.asLong(i, 0, j));
                    }
                }
            }
            moddedEntities.removeIf(ref -> ref.get() == null);
            moddedEntities.add(new WeakReference<>(entity));
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void leave(ModdedEntity entity) {
        long chunk = ChunkPos.asLongExcludeY(entity.getBoundingBox());

        lock.writeLock().lock();
        try {
            if (!umcEntities.containsKey(chunk)) {
                return;
            }
            Set<WeakReference<ModdedEntity>> moddedEntities = umcEntities.get(chunk);
            moddedEntities.removeIf(ref -> ref.get() == null || ref.get() == entity);

            if (moddedEntities.isEmpty()) {
                umcEntities.remove(chunk);
                scanningRange.removeKey(chunk);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void move(ModdedEntity entity, long oldSection, long newSection) {
        lock.writeLock().lock();
        try {
            if (!umcEntities.containsKey(oldSection)) {
                //Newly added, no need to process here
                //Let join handle it
                return;
            }

            // remove from old section
            Set<WeakReference<ModdedEntity>> moddedEntities = umcEntities.get(oldSection);
            if (moddedEntities != null) {
                moddedEntities.removeIf(ref -> ref.get() == null || ref.get() == entity);
                if (moddedEntities.isEmpty()) {
                    umcEntities.remove(oldSection);
                    scanningRange.removeKey(oldSection);
                }
            }

            long chunk = ChunkPos.asLongExcludeY(entity.getBoundingBox());
            int x = ChunkPos.x(chunk);
            int y = ChunkPos.y(chunk);
            int z = ChunkPos.z(chunk);

            moddedEntities = umcEntities.get(newSection);
            if (moddedEntities == null) {
                moddedEntities = new HashSet<>();
                umcEntities.put(newSection, moddedEntities);

                for (int i = x - HORIZONTAL_SEARCH_RADIUS_CHUNKS; i <= x + HORIZONTAL_SEARCH_RADIUS_CHUNKS; i++) {
                    for (int j = z - HORIZONTAL_SEARCH_RADIUS_CHUNKS; j <= z + HORIZONTAL_SEARCH_RADIUS_CHUNKS; j++) {
                        scanningRange.put(newSection, ChunkPos.asLong(i, 0, j));
                    }
                }
            }

            moddedEntities.removeIf(ref -> ref.get() == null);
            moddedEntities.add(new WeakReference<>(entity));
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Set<Long> queryPotentialPackedChunkPos(long pos) {
        lock.readLock().lock();
        try {
            return scanningRange.getKeys(pos);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Set<ModdedEntity> queryEntities(long pos) {
        lock.readLock().lock();
        try {
            Set<WeakReference<ModdedEntity>> refs = umcEntities.get(pos);
            if (refs == null || refs.isEmpty()) {
                return Collections.emptySet();
            }
            return refs.stream()
                       .map(WeakReference::get)
                       .filter(Objects::nonNull)
                       .collect(Collectors.toSet());
        } finally {
            lock.readLock().unlock();
        }
    }

    private static class LongBiMultiMap {
        private final HashMap<Long, Set<Long>> keyToValues = new HashMap<>();
        private final HashMap<Long, Set<Long>> valueToKeys = new HashMap<>();

        public void put(long key, long value) {
            keyToValues.computeIfAbsent(key, k -> new HashSet<>(245)).add(value);
            valueToKeys.computeIfAbsent(value, v -> new HashSet<>(4)).add(key);
        }

        public Set<Long> getKeys(long value) {
            Set<Long> set = valueToKeys.get(value);
            return set != null ? set : Collections.emptySet();
        }

        public Set<Long> removeKey(long key) {
            Set<Long> values = keyToValues.remove(key);
            if (values == null) {
                return Collections.emptySet();
            }

            for (long value : values) {
                Set<Long> keys = valueToKeys.get(value);
                if (keys != null) {
                    keys.remove(key);
                    if (keys.isEmpty()) {
                        valueToKeys.remove(value);
                    }
                }
            }

            return values;
        }
    }
}
