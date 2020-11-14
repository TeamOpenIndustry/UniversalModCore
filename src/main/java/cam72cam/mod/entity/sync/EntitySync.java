package cam72cam.mod.entity.sync;

import cam72cam.mod.ModCore;
import cam72cam.mod.entity.CustomEntity;
import cam72cam.mod.net.Packet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import cam72cam.mod.serialization.SerializationException;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.serialization.TagSerializer;

import java.util.ArrayList;
import java.util.List;

/** TagCompound that auto-serializes an entity's @TagSync fields from server to client */
public class EntitySync extends TagCompound {
    // Entity to synchronize
    private final CustomEntity entity;
    // Previous entry (for calculating diff / needs update)
    private TagCompound old;

    /** Track properties on entity */
    public EntitySync(CustomEntity entity) {
        super();
        this.entity = entity;
        this.old = new TagCompound();
    }

    /** Perform synchronization */
    public void send() throws SerializationException {
        if (entity.getWorld().isClient) {
            return;
        }

        TagSerializer.serialize(this, entity, TagSync.class);

        TagCompound sync = new TagCompound();
        List<String> removed = new ArrayList<>();

        for (String key : internal.getKeys()) {
            Tag newVal = internal.get(key);
            if (old.internal.contains(key)) {
                Tag oldVal = old.internal.get(key);
                if (newVal.equals(oldVal)) {
                    continue;
                }
            }
            sync.internal.put(key, newVal);
        }

        for (String key : old.internal.getKeys()) {
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

        if (sync.internal.getKeys().size() != 0) {
            old = new TagCompound(this.internal.copy());

            new EntitySyncPacket(entity, sync).sendToObserving(entity);
        }
    }

    /** Receive update (should only be called from packets) */
    public void receive(TagCompound sync) throws SerializationException {
        for (String key : sync.internal.getKeys()) {
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
