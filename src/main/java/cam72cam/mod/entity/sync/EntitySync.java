package cam72cam.mod.entity.sync;

import cam72cam.mod.ModCore;
import cam72cam.mod.entity.CustomEntity;
import cam72cam.mod.net.Packet;
import net.minecraft.nbt.INBT;
import cam72cam.mod.serialization.SerializationException;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.serialization.TagSerializer;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.lang.reflect.Field;
import java.util.*;

/** TagCompound that auto-serializes an entity's @TagSync fields from server to client */
public class EntitySync extends TagCompound {
    // Entity to synchronize
    private final CustomEntity entity;
    private int interval;
    // Previous entry (for calculating diff / needs update)
    private TagCompound old;
    //Cache for TagSync.floatPrecision and doublePrecision
    private static final Map<Class<?>, Map<String, Integer>> precisionMapper = new HashMap<>();
    //lookup table for precisions, 9 is fine
    private static final double[] syncs = new double[]{1, 0.1, 0.01, 0.001, 0.0001, 0.00001, 0.000001, 0.00000001, 0.000000001};

    /** Track properties on entity */
    public EntitySync(CustomEntity entity) {
        super();
        this.entity = entity;
        this.old = new TagCompound();
        this.interval = 10;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    /** Perform synchronization */
    public void send() throws SerializationException {
        if (entity.getWorld().isClient) {
            return;
        }

        if (entity.getTickCount() % interval != 0) {
            return;
        }

        TagSerializer.serialize(this, entity, TagSync.class);
        Map<String, Integer> precision = precisionMapper.computeIfAbsent(entity.getClass(), clazz -> {
            List<Field> allFields = new ArrayList<>();
            //Get all fields recursively
            for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
                allFields.addAll(Arrays.asList(c.getDeclaredFields()));
            }
            return allFields.stream()
                            .filter(field -> {
                                TagSync tagSync = field.getAnnotation(TagSync.class);
                                TagField tagField = field.getAnnotation(TagField.class);
                                return tagField != null && tagSync != null;
                            })
                            .collect(Object2IntOpenHashMap::new,
                                     (m, f) -> {
                                         TagSync tag = f.getAnnotation(TagSync.class);
                                         String name = f.getAnnotation(TagField.class).value();
                                         if("".equals(name)) {
                                             name = f.getName();
                                         }
                                         Class<?> type = f.getType();

                                         if (type == float.class || type == Float.class) {
                                             m.put(name, Math.max(0, Math.min(tag.floatPrecision(), 8)));
                                         } else if (type == double.class || type == Double.class) {
                                             m.put(name, Math.max(0, Math.min(tag.doublePrecision(), 8)));
                                         }
                                     },
                                     Object2IntOpenHashMap::putAll);
        });

        TagCompound sync = new TagCompound();
        List<String> removed = new ArrayList<>();

        for (String key : internal.keySet()) {
            INBT newVal = internal.get(key);
            if (old.internal.contains(key)) {
                INBT oldVal = old.internal.get(key);
                if (newVal.equals(oldVal)) {
                    continue;
                }
                if (oldVal.getId() == 5) {
                    if (Math.abs(old.internal.getFloat(key) - internal.getFloat(key)) < syncs[precision.get(key)]) {
                        continue;
                    }
                }
                if (oldVal.getId() == 6) {
                    if (Math.abs(old.internal.getDouble(key) - internal.getDouble(key)) < syncs[precision.get(key)]) {
                        continue;
                    }
                }
            }
            sync.internal.put(key, newVal);
        }

        for (String key : old.internal.keySet()) {
            if (!internal.contains(key)) {
                removed.add(key);
            }
        }
        if (!removed.isEmpty()) {
            sync.setList("sync_internal_removed", removed, key -> {
                TagCompound tc = new TagCompound();
                tc.setString("removed", key);
                return tc;
            });
        }

        if (!sync.internal.keySet().isEmpty()) {
            old = new TagCompound(this.internal.copy());
            new EntitySyncPacket(entity, sync).sendToObserving(entity);
        }
    }

    /** Receive update (should only be called from packets) */
    public void receive(TagCompound sync) throws SerializationException {
        for (String key : sync.internal.keySet()) {
            if (key.equals("sync_internal_removed")) {
                for (String removed : sync.getList(key, x -> x.getString("removed"))) {
                    internal.remove(removed);
                }
            } else {
                internal.put(key, sync.internal.get(key));
            }
        }
        TagSerializer.deserialize(this, entity, entity.getWorld(), TagSync.class);
    }

    public static class EntitySyncPacket extends Packet {
        @TagField
        CustomEntity target;
        @TagField
        private TagCompound info;

        public EntitySyncPacket() {}

        public EntitySyncPacket(CustomEntity entity, TagCompound sync) {
            this.target = entity;
            this.info = sync;
        }

        @Override
        public void handle() {
            if (target != null) {
                try {
                    target.sync.receive(info);
                } catch (SerializationException e) {
                    ModCore.catching(e, "Invalid sync payload for %s: %s", target, info);
                }
            }
        }
    }
}
