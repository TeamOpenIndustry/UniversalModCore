package cam72cam.mod.serialization;

import cam72cam.mod.block.BlockEntity;
import cam72cam.mod.block.tile.TileEntity;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.world.World;
import net.minecraft.nbt.CompoundNBT;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** Wraps MC's tag object */
public class TagCompound {
    /** Internal, do not use */
    public final CompoundNBT internal;

    /** Wraps MC object, do not use */
    public TagCompound(CompoundNBT data) {
        this.internal = data;
    }

    public TagCompound() {
        this(new CompoundNBT());
    }

    public boolean hasKey(String key) {
        return internal.contains(key);
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
        return setter(key, value, internal::putBoolean);
    }

    public Byte getByte(String key) {
        return getter(key, internal::getByte);
    }

    public TagCompound setByte(String key, Byte value) {
        return setter(key, value, internal::putByte);
    }

    public Integer getInteger(String key) {
        return getter(key, internal::getInt);
    }

    public TagCompound setInteger(String key, Integer value) {
        return setter(key, value, internal::putInt);
    }

    public Long getLong(String key) {
        return getter(key, internal::getLong);
    }

    public TagCompound setLong(String key, Long value) {
        return setter(key, value, internal::putLong);
    }

    public Float getFloat(String key) {
        return getter(key, internal::getFloat);
    }

    public TagCompound setFloat(String key, Float value) {
        return setter(key, value, internal::putFloat);
    }

    public Double getDouble(String key) {
        return getter(key, internal::getDouble);
    }

    public TagCompound setDouble(String key, Double value) {
        return setter(key, value, internal::putDouble);
    }

    public String getString(String key) {
        return getter(key, (Function<String, String>) internal::getString);
    }

    public TagCompound setString(String key, String value) {
        return setter(key, value, internal::putString);
    }

    public UUID getUUID(String key) {
        if (internal.hasUniqueId(key)) {
            return internal.getUniqueId(key);
        }
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
            CompoundNBT tag = internal.getCompound(key);
            return new Vec3i(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"));
        });
    }

    public TagCompound setVec3i(String key, Vec3i pos) {
        return setter(key, pos, () -> {
            CompoundNBT tag = new CompoundNBT();
            tag.putInt("X", pos.x);
            tag.putInt("Y", pos.y);
            tag.putInt("Z", pos.z);
            internal.put(key, tag);
        });
    }

    public Vec3d getVec3d(String key) {
        return getter(key, () -> {
            CompoundNBT nbt = internal.getCompound(key);
            return new Vec3d(nbt.getDouble("x"), nbt.getDouble("y"), nbt.getDouble("z"));
        });
    }

    public TagCompound setVec3d(String key, Vec3d value) {
        return setter(key, value, () -> {
            CompoundNBT nbt = new CompoundNBT();
            nbt.putDouble("x", value.x);
            nbt.putDouble("y", value.y);
            nbt.putDouble("z", value.z);
            internal.put(key, nbt);
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

    public <T extends Enum<?>> T getEnum(String key, Class<T> cls) {
        return getter(key, () -> cls.getEnumConstants()[internal.getInt(key)]);
    }

    public TagCompound setEnum(String key, Enum<?> value) {
        return setter(key, value, () -> internal.putInt(key, value.ordinal()));
    }

    public <T extends Enum<?>> List<T> getEnumList(String key, Class<T> cls) {
        return getter(key, () ->
            Arrays.stream(internal.getIntArray(key)).mapToObj((int i) -> cls.getEnumConstants()[i]).collect(Collectors.toList())
        );
    }

    public TagCompound setEnumList(String key, List<? extends Enum<?>> items) {
        return setter(key, items, () ->
            internal.putIntArray(key, items.stream().map(Enum::ordinal).mapToInt(i -> i).toArray())
        );
    }

    public TagCompound get(String key) {
        return getter(key, () -> new TagCompound(internal.getCompound(key)));
    }

    public TagCompound set(String key, TagCompound value) {
        return setter(key, value, () -> internal.put(key, value.internal));
    }

    public void remove(String key) {
        internal.remove(key);
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
            for (String item : data.internal.keySet()) {
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
        return setter(key, stack, () -> internal.put(key, stack.toTag().internal));
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

            net.minecraft.tileentity.TileEntity te = net.minecraft.tileentity.TileEntity.create(ted.get("data").internal);
            te.setWorldAndPos(world.internal, te.getPos());
            assert te instanceof TileEntity;
            return (T) ((TileEntity) te).instance();
        });
    }

    public <T extends BlockEntity> TagCompound setTile(String key, T tile) {
        return setter(key, tile, () -> {
            TagCompound ted = new TagCompound();
            ted.setWorld("world", tile.getWorld());

            TagCompound data = new TagCompound();
            tile.internal.write(data.internal);
            ted.set("data", data);

            set(key, ted);
        });
    }

    public boolean isEmpty() {
        return internal.isEmpty();
    }
}
