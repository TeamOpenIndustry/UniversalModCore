package cam72cam.mod.serialization;

import cam72cam.mod.block.BlockEntity;
import cam72cam.mod.block.tile.TileEntity;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.world.World;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;

import java.io.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** Wraps MC's tag object */
public class TagCompound {
    /** Internal, do not use */
    public final NBTTagCompound internal;

    /** Wraps MC object, do not use */
    public TagCompound(NBTTagCompound data) {
        this.internal = data;
    }

    public TagCompound(byte[] data) throws IOException {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            internal = CompressedStreamTools.read(in);
        }
    }

    public TagCompound() {
        this(new NBTTagCompound());
    }

    public boolean hasKey(String key) {
        return internal.hasKey(key);
    }

    private <T> T getter(String key, Supplier<T> fn) {
        return hasKey(key) ? fn.get() : null;
    }

    private <T> T getter(String key, Function<String, T> fn) {
        return getter(key, () -> fn.apply(key));
    }

    public <T> TagCompound setter(String key, T value, Runnable fn) {
        if (value == null) {
            remove(key);
            return this;
        }
        fn.run();
        return this;
    }

    public <T> TagCompound setter(String key, T value, BiConsumer<String, T> fn) {
        return setter(key, value, () -> fn.accept(key, value));
    }

    public Boolean getBoolean(String key) {
        return getter(key, internal::getBoolean);
    }

    public TagCompound setBoolean(String key, Boolean value) {
        return setter(key, value, internal::setBoolean);
    }

    public Byte getByte(String key) {
        return getter(key, internal::getByte);
    }

    public TagCompound setByte(String key, Byte value) {
        return setter(key, value, internal::setByte);
    }

    public Integer getInteger(String key) {
        return getter(key, internal::getInteger);
    }

    public TagCompound setInteger(String key, Integer value) {
        return setter(key, value, internal::setInteger);
    }

    public Long getLong(String key) {
        return getter(key, internal::getLong);
    }

    public TagCompound setLong(String key, Long value) {
        return setter(key, value, internal::setLong);
    }

    public Float getFloat(String key) {
        return getter(key, internal::getFloat);
    }

    public TagCompound setFloat(String key, Float value) {
        return setter(key, value, internal::setFloat);
    }

    public Double getDouble(String key) {
        return getter(key, internal::getDouble);
    }

    public TagCompound setDouble(String key, Double value) {
        return setter(key, value, internal::setDouble);
    }

    public String getString(String key) {
        return getter(key, internal::getString);
    }

    public TagCompound setString(String key, String value) {
        return setter(key, value, internal::setString);
    }

    public UUID getUUID(String key) {
        return getter(key, s -> UUID.fromString(getString(s)));
    }

    public TagCompound setUUID(String key, UUID value) {
        return setter(key, value, (s, v) -> setString(s, v.toString()));
    }

    public Vec3i getVec3i(String key) {
        return getter(key, () -> {
            if (internal.getTagId(key) == 4) {
                return new Vec3i(internal.getLong(key));
            }
            NBTTagCompound tag = internal.getCompoundTag(key);
            return new Vec3i(tag.getInteger("X"), tag.getInteger("Y"), tag.getInteger("Z"));
        });
    }

    public TagCompound setVec3i(String key, Vec3i pos) {
        return setter(key, pos, () -> {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("X", pos.x);
            tag.setInteger("Y", pos.y);
            tag.setInteger("Z", pos.z);
            internal.setTag(key, tag);
        });
    }

    public Vec3d getVec3d(String key) {
        return getter(key, () -> {
            NBTTagCompound nbt = internal.getCompoundTag(key);
            return new Vec3d(nbt.getDouble("x"), nbt.getDouble("y"), nbt.getDouble("z"));
        });
    }

    public TagCompound setVec3d(String key, Vec3d value) {
        return setter(key, value, () -> {
            NBTTagCompound nbt = new NBTTagCompound();
            nbt.setDouble("x", value.x);
            nbt.setDouble("y", value.y);
            nbt.setDouble("z", value.z);
            internal.setTag(key, nbt);
        });
    }

    public cam72cam.mod.entity.Entity getEntity(String key, World world) {
        return getEntity(key, world, cam72cam.mod.entity.Entity.class);
    }

    public <T extends cam72cam.mod.entity.Entity> T getEntity(String key, World world, Class<T> cls) {
        return getter(key, () -> {
            TagCompound data = get(key);
            UUID id = data.getUUID("id");
            World w = data.getWorld("world", world.isClient);
            return w == null ? null : w.getEntity(id, cls);
        });
    }

    public TagCompound setEntity(String key, Entity entity) {
        return setter(key, entity, () -> {
            TagCompound data = new TagCompound();
            data.setUUID("id", entity.getUUID());
            data.setInteger("world", entity.getWorld().getId());
            set(key, data);
        });
    }

    private <T extends Enum<?>> T safeEnumDecode(Class<T> cls, int ordinal) {
        T[] values = cls.getEnumConstants();
        return ordinal >= values.length ? values[0] : values[ordinal];
    }

    public <T extends Enum<?>> T getEnum(String key, Class<T> cls) {
        return getter(key, () -> safeEnumDecode(cls, internal.getInteger(key)));
    }

    public TagCompound setEnum(String key, Enum<?> value) {
        return setter(key, value, () -> internal.setInteger(key, value.ordinal()));
    }

    public <T extends Enum<?>> List<T> getEnumList(String key, Class<T> cls) {
        return getter(key, () ->
            Arrays.stream(internal.getIntArray(key)).mapToObj((int i) -> safeEnumDecode(cls, i)).collect(Collectors.toList())
        );
    }

    public TagCompound setEnumList(String key, List<? extends Enum<?>> items) {
        return setter(key, items, () ->
            internal.setIntArray(key, items.stream().map(Enum::ordinal).mapToInt(i -> i).toArray())
        );
    }

    public TagCompound get(String key) {
        return getter(key, () -> new TagCompound(internal.getCompoundTag(key)));
    }

    public TagCompound set(String key, TagCompound value) {
        return setter(key, value, () -> internal.setTag(key, value.internal));
    }

    public void remove(String key) {
        internal.removeTag(key);
    }

    public <T> List<T> getList(String key, Function<TagCompound, T> decoder) {
        return getter(key, () -> {
            List<T> list = new ArrayList<>();
            TagCompound data = get(key);
            for (int i = 0; i < data.getInteger("count"); i++) {
                list.add(decoder.apply(data.get(i + "")));
            }
            return list;
        });
    }

    public <T> TagCompound setList(String key, List<T> list, Function<T, TagCompound> encoder) {
        return setter(key, list, () -> {
            TagCompound data = new TagCompound();
            data.setInteger("count", list.size());
            for (int i = 0; i < list.size(); i++) {
                data.set(i + "", encoder.apply(list.get(i)));
            }
            set(key, data);
        });
    }

    public <K, V> Map<K, V> getMap(String key, Function<String, K> keyFn, Function<TagCompound, V> valFn) {
        return getter(key, () -> {
            Map<K, V> map = new HashMap<>();
            TagCompound data = get(key);
            for (String item : ((Set<String>)data.internal.getKeySet())) {
                map.put(keyFn.apply(item), valFn.apply(data.get(item)));
            }
            return map;
        });
    }

    public <K, V> TagCompound setMap(String key, Map<K, V> map, Function<K, String> keyFn, Function<V, TagCompound> valFn) {
        return setter(key, map, () -> {
            TagCompound data = new TagCompound();
            for (K item : map.keySet()) {
                data.set(keyFn.apply(item), valFn.apply(map.get(item)));
            }
            set(key, data);
        });
    }

    public ItemStack getStack(String key) {
        ItemStack stack = getter(key, () -> new ItemStack(get(key)));
        return stack != null ? stack : ItemStack.EMPTY;
    }

    public TagCompound setStack(String key, ItemStack stack) {
        return setter(key, stack, () -> internal.setTag(key, stack.toTag().internal));
    }

    public String toString() {
        return internal.toString();
    }

    public World getWorld(String key, boolean isClient) {
        return getter(key, () -> World.get(getInteger(key), isClient));
    }

    public TagCompound setWorld(String key, World world) {
        return setter(key, world, () -> setInteger(key, world.getId()));
    }

    public <T extends BlockEntity> T getTile(String key, boolean isClient) {
        return getter(key, () -> {
            TagCompound ted = get(key);
            World world = ted.getWorld("world", isClient);

            if (world == null) {
                return null;
            }

            if (!ted.hasKey("data")) {
                return null;
            }

            net.minecraft.tileentity.TileEntity te = net.minecraft.tileentity.TileEntity.createAndLoadEntity(ted.get("data").internal);
            te.setWorldObj(world.internal);
            assert te instanceof TileEntity;
            return (T) ((TileEntity) te).instance();
        });
    }

    public <T extends BlockEntity> TagCompound setTile(String key, T tile) {
        return setter(key, tile, () -> {
            TagCompound ted = new TagCompound();
            ted.setWorld("world", tile.getWorld());

            TagCompound data = new TagCompound();
            tile.internal.writeToNBT(data.internal);
            ted.set("data", data);

            set(key, ted);
        });
    }

    public boolean isEmpty() {
        return internal.hasNoTags();
    }

    public byte[] toBytes() throws IOException {
        try(ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CompressedStreamTools.write(internal, new DataOutputStream(out));
            out.flush();
            return out.toByteArray();
        }
    }
}
