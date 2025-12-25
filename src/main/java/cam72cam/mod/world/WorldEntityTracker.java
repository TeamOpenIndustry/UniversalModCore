package cam72cam.mod.world;

import cam72cam.mod.entity.ModdedEntity;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.util.math.BlockPos;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Track UMC Entities and handle inter chunk collision
 * @see cam72cam.mod.mixin.feat.large_entity_collision.MixinVanillaWorld
 */
public class WorldEntityTracker {
    //Good enough for now...We'd assume there's no more ridiculous ones
    //Horizontal distance (48 blocks)
    private static final int HORIZONTAL_SEARCH_RADIUS_CHUNKS = 3;
    //Vertical distance (32 blocks)
    private static final int VERTICAL_SEARCH_RADIUS_CHUNKS = 2;
    private final Map<Long, Set<ModdedEntity>> umcEntities = new Long2ObjectOpenHashMap<>();
    //K are chunks containing UMC entities, V are neighbor chunks(245 per now) that this entity may extend to
    //Query value to see which chunk may contain possible colliding entities
    private final LongBiMultiMap scanningRange = new LongBiMultiMap();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public WorldEntityTracker() {}

    public void join(ModdedEntity entity) {
        long chunk = ChunkPos.asLong(entity.getPosition());
        int x = ChunkPos.x(chunk);
        int y = ChunkPos.y(chunk);
        int z = ChunkPos.z(chunk);

        lock.writeLock().lock();
        try {
            Set<ModdedEntity> moddedEntities = umcEntities.get(chunk);
            if (moddedEntities == null) {
                moddedEntities = new ObjectArraySet<>();
                umcEntities.put(chunk, moddedEntities);

                for (int i = x - HORIZONTAL_SEARCH_RADIUS_CHUNKS; i <= x + HORIZONTAL_SEARCH_RADIUS_CHUNKS; i++) {
                    for (int j = z - HORIZONTAL_SEARCH_RADIUS_CHUNKS; j <= z + HORIZONTAL_SEARCH_RADIUS_CHUNKS; j++) {
                        for (int k = y - VERTICAL_SEARCH_RADIUS_CHUNKS; k <= y + VERTICAL_SEARCH_RADIUS_CHUNKS; k++) {
                            scanningRange.put(chunk, ChunkPos.asLong(i, k, j));
                        }
                    }
                }
            }
            moddedEntities.add(entity);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void leave(ModdedEntity entity) {
        BlockPos pos = entity.getPosition();
        long sec = ChunkPos.asLong(pos);

        lock.writeLock().lock();
        try {
            if (!umcEntities.containsKey(sec)) {
                return;
            }
            Collection<ModdedEntity> moddedEntities = umcEntities.get(sec);
            moddedEntities.remove(entity);

            if (moddedEntities.isEmpty()) {
                umcEntities.remove(sec);
                scanningRange.removeKey(sec);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void move(ModdedEntity entity, long oldSection, long newSection) {
        lock.writeLock().lock();
        try {
            // remove from old section
            Set<ModdedEntity> moddedEntities = umcEntities.get(oldSection);
            if (moddedEntities != null) {
                moddedEntities.remove(entity);
                if (moddedEntities.isEmpty()) {
                    umcEntities.remove(oldSection);
                    scanningRange.removeKey(oldSection);
                }
            }

            long chunk = ChunkPos.asLong(entity.getPosition());
            int x = ChunkPos.x(chunk);
            int y = ChunkPos.y(chunk);
            int z = ChunkPos.z(chunk);

            moddedEntities = umcEntities.get(newSection);
            if (moddedEntities == null) {
                moddedEntities = new ObjectArraySet<>();
                umcEntities.put(newSection, moddedEntities);

                for (int i = x - HORIZONTAL_SEARCH_RADIUS_CHUNKS; i <= x + HORIZONTAL_SEARCH_RADIUS_CHUNKS; i++) {
                    for (int j = z - HORIZONTAL_SEARCH_RADIUS_CHUNKS; j <= z + HORIZONTAL_SEARCH_RADIUS_CHUNKS; j++) {
                        for (int k = y - VERTICAL_SEARCH_RADIUS_CHUNKS; k <= y + VERTICAL_SEARCH_RADIUS_CHUNKS; k++) {
                            scanningRange.put(newSection, ChunkPos.asLong(i, k, j));
                        }
                    }
                }
            }

            moddedEntities.add(entity);
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
            return umcEntities.get(pos);
        } finally {
            lock.readLock().unlock();
        }
    }

    private static class LongBiMultiMap {
        private final Long2ObjectOpenHashMap<LongOpenHashSet> keyToValues
                = new Long2ObjectOpenHashMap<>();
        private final Long2ObjectOpenHashMap<LongArraySet> valueToKeys
                = new Long2ObjectOpenHashMap<>();

        public void put(long key, long value) {
            keyToValues.computeIfAbsent(key, k -> new LongOpenHashSet(245)).add(value);
            valueToKeys.computeIfAbsent(value, v -> new LongArraySet(4)).add(key);
        }

        //DON't MODIFY RETURNED SET!
        //If you want please clone()
        public Set<Long> getKeys(long value) {
            LongArraySet set = valueToKeys.get(value);
            return set != null ? set : new LongArraySet();
        }

        public Set<Long> removeKey(long key) {
            LongOpenHashSet values = keyToValues.remove(key);
            if (values == null) {
                return new LongArraySet();
            }

            for (long value : values) {
                LongArraySet keys = valueToKeys.get(value);
                if (keys != null) {
                    keys.remove(key);
                    if (keys.isEmpty()) {
                        valueToKeys.remove(value);
                    }
                }
            }

            return values;
        }

        public boolean contains(long key) {
            return keyToValues.containsKey(key);
        }

        public void clear() {
            keyToValues.clear();
            valueToKeys.clear();
        }
    }
}
