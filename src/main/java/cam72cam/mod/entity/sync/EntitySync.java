package cam72cam.mod.entity.sync;

import cam72cam.mod.ModCore;
import cam72cam.mod.entity.CustomEntity;
import cam72cam.mod.net.Packet;
import cam72cam.mod.serialization.SerializationException;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.serialization.TagSerializer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;

import java.util.Set;

import java.util.ArrayList;
import java.util.List;

/** TagCompound that auto-serializes an entity's @TagSync fields from server to client */
public class EntitySync extends TagCompound {
    // Entity to synchronize
    private final CustomEntity entity;
    private int interval;
    // Previous entry (for calculating diff / needs update)
    private TagCompound old;

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

        TagCompound sync = new TagCompound();
        List<String> removed = new ArrayList<>();

        for (String key : (Set<String>)internal.getKeySet()) {
            NBTBase newVal = internal.getTag(key);
            if (old.internal.hasKey(key)) {
                NBTBase oldVal = old.internal.getTag(key);
                if (newVal.toString().equals(oldVal.toString())) {
                    continue;
                }
                if (oldVal.getId() == 5) {
                    if (Math.abs(old.internal.getFloat(key) - internal.getFloat(key)) < 0.001) {
                        continue;
                    }
                }
                if (oldVal.getId() == 6) {
                    if (Math.abs(old.internal.getDouble(key) - internal.getDouble(key)) < 0.00001) {
                        continue;
                    }
                }
            }
            sync.internal.setTag(key, newVal);
        }

        for (String key : (Set<String>)old.internal.getKeySet()) {
            if (!internal.hasKey(key)) {
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

        if (sync.internal.getKeySet().size() != 0) {
            old = new TagCompound((NBTTagCompound) this.internal.copy());
            new EntitySyncPacket(entity, sync).sendToObserving(entity);
        }
    }

    /** Receive update (should only be called from packets) */
    public void receive(TagCompound sync) throws SerializationException {
        for (String key : ((Set<String>)sync.internal.getKeySet())) {
            if (key.equals("sync_internal_removed")) {
                for (String removed : sync.getList(key, x -> x.getString("removed"))) {
                    internal.removeTag(removed);
                }
            } else {
                internal.setTag(key, sync.internal.getTag(key));
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
