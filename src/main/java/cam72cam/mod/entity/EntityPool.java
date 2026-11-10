package cam72cam.mod.entity;

import cam72cam.mod.ModCore;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.world.World;

import java.util.*;
import java.util.stream.Collectors;

public class EntityPool {
    private static final Map<World, EntityPool> INSTANCES = new HashMap<>();
    private static IEntityPoolPolicy policy = null;
    private static boolean registered = false;

    public static void register(IEntityPoolPolicy policyImpl) {
        policy = policyImpl;
        if (!registered) {
            registered = true;
            World.registerLifecycleListener(new World.EntityLifecycleListener() {
                @Override
                public void onEntityAdded(net.minecraft.entity.Entity entity) {
                    if (!(entity instanceof ModdedEntity)) return;
                    World w = World.get(entity.world);
                    if (w == null || !w.isServer || policy == null || !policy.isEnabled()) return;

                    UUID uuid = entity.getPersistentID();
                    EntityPool pool = get(w);
                    ModdedEntity current = pool.all.get(uuid);

                    if (current == entity) return;
                    if (current != null) {
                        // Duplicate wild entity found, kill it
                        entity.setDead();
                        return;
                    }
                    pool.add((ModdedEntity) entity);
                }

                @Override
                public void onEntityRemoved(net.minecraft.entity.Entity entity) {
                    if (!(entity instanceof ModdedEntity)) return;
                    World w = World.get(entity.world);
                    if (w == null || !w.isServer || policy == null || !policy.isEnabled()) return;

                    EntityPool pool = get(w);
                    UUID uuid = entity.getPersistentID();
                    if (pool.activelyDespawning.contains(uuid)) return;

                    ModdedEntity current = pool.all.get(uuid);
                    if (current != null && current != entity) return;

                    if (entity.isDead) {
                        pool.remove(uuid);
                    }
                }
            });

            World.onTick(EntityPool::onWorldTick);
        }
    }

    // Provides a unified interface for entity lookup, replacing the previous offlineEntityLookup in UMC World
    public static CustomEntity lookupOfflineEntity(UUID uuid) {
        if (policy == null || !policy.isEnabled()) return null;
        for (EntityPool pool : INSTANCES.values()) {
            ModdedEntity e = pool.all.get(uuid);
            if (e != null) {
                CustomEntity self = e.getSelf();
                if (self != null) return self;
            }
        }
        return null;
    }

    public static EntityPool get(World world) {
        return INSTANCES.computeIfAbsent(world, EntityPool::new);
    }

    private final World world;
    private final Map<UUID, ModdedEntity> all = new HashMap<>();
    private final Set<UUID> activelyDespawning = new HashSet<>();
    private int tickCounter = 0;

    private EntityPool(World world) {
        this.world = world;
    }

    private static void onWorldTick(World w) {
        if (!w.isServer || policy == null) return;

        EntityPool pool = INSTANCES.get(w);
        if (pool == null) return;

        if (!policy.isEnabled()) {
            pool.flushAndDisable();
            return;
        }
        pool.tick();
    }

    private void add(ModdedEntity e) {
        if (policy == null) return;
        if (policy.shouldPool(e.getSelf())) {
            UUID uuid = e.getPersistentID();
            ModdedEntity existing = all.put(uuid, e);

            // If this is an entity returning to the world, trigger the policy callback
            if (existing != e && e.isAddedToChunk()) {
                policy.onEntitySpawned(e);
            }
        }
    }

    private void remove(UUID uuid) {
        all.remove(uuid);
    }

    private void tick() {
        // Manually tick offline entities
        for (ModdedEntity e : all.values()) {
            if (!e.isAddedToChunk() && !e.isDead) {
                try {
                    e.onUpdate();
                } catch (Exception ex) {
                    ModCore.catching(ex, "Offline tick failed for %s", e.getPersistentID());
                }
            }
        }

        // Periodically scan distances
        if (++tickCounter >= policy.getProximityInterval()) {
            tickCounter = 0;
            try {
                checkProximity();
            } catch (Exception ex) {
                ModCore.catching(ex, "checkProximity failed in world %d", world.getId());
            }
        }
    }

    private void checkProximity() {
        List<Player> players = world.getEntities(Player.class);
        if (players.isEmpty()) return;

        for (ModdedEntity e : all.values()) {
            if (e.isDead) continue;

            CustomEntity self = e.getSelf();
            if (self != null && !self.getPassengers().isEmpty()) continue;

            double distSq = nearestPlayerDistSq(players, new Vec3d(e.getPositionVector()));

            if (e.isAddedToChunk()) {
                if (distSq > policy.getDespawnDistanceSq()) despawn(e);
            } else {
                if (distSq < policy.getSpawnDistanceSq()) spawn(e);
            }
        }
    }

    private double nearestPlayerDistSq(List<Player> players, Vec3d pos) {
        double best = Double.MAX_VALUE;
        for (Player p : players) {
            double d = p.internal.getPositionVector().squareDistanceTo(pos.internal());
            if (d < best) best = d;
        }
        return best;
    }

    private void despawn(ModdedEntity e) {
        UUID uuid = e.getPersistentID();
        activelyDespawning.add(uuid);
        try {
            world.internal.removeEntityDangerously(e);
            if (e.isAddedToChunk()) {
                e.setAddedToChunk(false);
                world.internal.loadedEntityList.remove(e);
            }
            e.isDead = false;
        } finally {
            activelyDespawning.remove(uuid);
        }
    }

    private void ensureChunkLoaded(ModdedEntity e, boolean longTermProtect) {
        int chunkX = ((int) Math.floor(e.posX)) >> 4;
        int chunkZ = ((int) Math.floor(e.posZ)) >> 4;

        // Ensure chunk is in memory
        e.world.getChunk(chunkX, chunkZ);

        // If it's a bulk force-eject when disabling the pool, add a lock to prevent immediate unloading
        if (longTermProtect) {
            world.keepLoaded(new Vec3i(chunkX << 4, 0, chunkZ << 4));
        }
    }

    public void spawn(ModdedEntity e) {
        e.isDead = false;
        if (!e.isAddedToChunk()) {
            // Normal spawn: needs loading, but no forced lock
            ensureChunkLoaded(e, false);
            world.internal.spawnEntity(e);
        }
    }

    private void flushAndDisable() {
        if (all.isEmpty()) return;
        for (ModdedEntity e : new ArrayList<>(all.values())) {
            e.isDead = false;
            if (!e.isAddedToChunk()) {
                // Pool disable force-eject, load + lock for a few seconds
                ensureChunkLoaded(e, true);
                world.internal.spawnEntity(e);
            }
        }
        all.clear();
        activelyDespawning.clear();
    }

    public Collection<ModdedEntity> getAllEntities() {
        return all.values();
    }

    public ModdedEntity getEntity(UUID uuid) {
        return all.get(uuid);
    }

    public ModdedEntity createFromTagCompound(TagCompound data) {
        ModdedEntity e = world.createModdedEntity();
        e.deserializeFromTagCompound(data);
        e.setAddedToChunk(false);
        e.isDead = false;
        add(e);
        return e;
    }
}