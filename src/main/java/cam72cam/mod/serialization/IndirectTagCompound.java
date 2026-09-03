package cam72cam.mod.serialization;

import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import com.mojang.serialization.Codec;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public class IndirectTagCompound extends TagCompound {
    // If input is null, all getter/hasKey checks will be NO-OP
    public final ValueInput input;
    // If output is null, all setter will be NO-OP
    public final ValueOutput output;

    public IndirectTagCompound(@Nullable ValueInput input, @Nullable ValueOutput output) {
        this.input = input;
        this.output = output;
    }

    private static <T extends Enum<?>> T safeEnumDecode(Class<T> cls, int ordinal) {
        T[] values = cls.getEnumConstants();
        return ordinal >= values.length ? values[0] : values[ordinal];
    }

    @Override
    public boolean hasKey(String key) {
        if (input == null) {
            return false;
        }
        try {
            return input.keySet().contains(key);
        } catch (RuntimeException e) {
            return false;
        }
    }

    @Override
    public void remove(String key) {
        if (output != null) {
            output.discard(key);
        }
    }

    @Override
    public boolean isEmpty() {
        if (input != null) {
            try {
                return input.keySet().isEmpty();
            } catch (RuntimeException e) {
                return true;
            }
        }
        if (output != null) {
            return output.isEmpty();
        }
        return true;
    }

    @Override
    public Boolean getBoolean(String key) {
        if (input == null || !hasKey(key)) {
            return null;
        }
        return input.getBooleanOr(key, false);
    }

    @Override
    public TagCompound setBoolean(String key, Boolean value) {
        if (value == null) {
            remove(key);
            return this;
        }
        if (output == null) {
            return this;
        }
        output.putBoolean(key, value);
        return this;
    }

    @Override
    public Byte getByte(String key) {
        if (input == null || !hasKey(key)) {
            return null;
        }
        return input.getByteOr(key, (byte) 0);
    }

    @Override
    public TagCompound setByte(String key, Byte value) {
        if (value == null) {
            remove(key);
            return this;
        }
        if (output == null) {
            return this;
        }
        output.putByte(key, value);
        return this;
    }

    @Override
    public Integer getInteger(String key) {
        if (input == null || !hasKey(key)) {
            return null;
        }
        return input.getInt(key).orElseThrow();
    }

    @Override
    public TagCompound setInteger(String key, Integer value) {
        if (value == null) {
            remove(key);
            return this;
        }
        if (output == null) {
            return this;
        }
        output.putInt(key, value);
        return this;
    }

    @Override
    public Long getLong(String key) {
        if (input == null || !hasKey(key)) {
            return null;
        }
        return input.getLong(key).orElseThrow();
    }

    @Override
    public TagCompound setLong(String key, Long value) {
        if (value == null) {
            remove(key);
            return this;
        }
        if (output == null) {
            return this;
        }
        output.putLong(key, value);
        return this;
    }

    @Override
    public Float getFloat(String key) {
        if (input == null || !hasKey(key)) {
            return null;
        }
        return input.getFloatOr(key, 0f);
    }

    @Override
    public TagCompound setFloat(String key, Float value) {
        if (value == null) {
            remove(key);
            return this;
        }
        if (output == null) {
            return this;
        }
        output.putFloat(key, value);
        return this;
    }

    @Override
    public Double getDouble(String key) {
        if (input == null || !hasKey(key)) {
            return null;
        }
        return input.getDoubleOr(key, 0d);
    }

    @Override
    public TagCompound setDouble(String key, Double value) {
        if (value == null) {
            remove(key);
            return this;
        }
        if (output == null) {
            return this;
        }
        output.putDouble(key, value);
        return this;
    }

    @Override
    public String getString(String key) {
        if (input == null || !hasKey(key)) {
            return null;
        }
        return input.getString(key).orElseThrow();
    }

    @Override
    public TagCompound setString(String key, String value) {
        if (value == null) {
            remove(key);
            return this;
        }
        if (output == null) {
            return this;
        }
        output.putString(key, value);
        return this;
    }

    @Override
    public UUID getUUID(String key) {
        if (input == null || !hasKey(key)) {
            return null;
        }
        return UUID.fromString(input.getString(key).orElseThrow());
    }

    @Override
    public TagCompound get(String key) {
        if (input == null || !hasKey(key)) {
            return null;
        }
        return input.child(key).map(TagCompound::indirect).orElseGet(TagCompound::new);
    }

    @Override
    public TagCompound set(String key, TagCompound value) {
        if (value == null) {
            remove(key);
            return this;
        }
        if (output == null) {
            return this;
        }
        output.child(key).store(value.internal);
        return this;
    }

    @Override
    public Vec3i getVec3i(String key) {
        if (input == null || !hasKey(key)) {
            return null;
        }
        Optional<Long> asLong = input.getLong(key);
        if (asLong.isPresent()) {
            return new Vec3i(asLong.get());
        }
        ValueInput child = input.child(key).orElseThrow();
        return new Vec3i(
                child.getInt("X").orElseThrow(),
                child.getInt("Y").orElseThrow(),
                child.getInt("Z").orElseThrow()
        );
    }

    @Override
    public TagCompound setVec3i(String key, Vec3i pos) {
        if (pos == null) {
            remove(key);
            return this;
        }
        if (output == null) {
            return this;
        }
        ValueOutput child = output.child(key);
        child.putInt("X", pos.x);
        child.putInt("Y", pos.y);
        child.putInt("Z", pos.z);
        return this;
    }

    @Override
    public Vec3d getVec3d(String key) {
        if (input == null || !hasKey(key)) {
            return null;
        }
        ValueInput child = input.child(key).orElseThrow();
        return new Vec3d(
                child.read("x", Codec.DOUBLE).orElseThrow(),
                child.read("y", Codec.DOUBLE).orElseThrow(),
                child.read("z", Codec.DOUBLE).orElseThrow()
        );
    }

    @Override
    public TagCompound setVec3d(String key, Vec3d value) {
        if (value == null) {
            remove(key);
            return this;
        }
        if (output == null) {
            return this;
        }
        ValueOutput child = output.child(key);
        child.putDouble("x", value.x);
        child.putDouble("y", value.y);
        child.putDouble("z", value.z);
        return this;
    }

    @Override
    public <T extends Enum<?>> T getEnum(String key, Class<T> cls) {
        if (input == null || !hasKey(key)) {
            return null;
        }
        return safeEnumDecode(cls, input.getInt(key).orElseThrow());
    }

    @Override
    public TagCompound setEnum(String key, Enum<?> value) {
        if (value == null) {
            remove(key);
            return this;
        }
        if (output == null) {
            return this;
        }
        output.putInt(key, value.ordinal());
        return this;
    }

    @Override
    public <T extends Enum<?>> List<T> getEnumList(String key, Class<T> cls) {
        if (input == null || !hasKey(key)) {
            return null;
        }
        List<T> list = new ArrayList<>();
        for (int ordinal : input.getIntArray(key).orElseThrow()) {
            list.add(safeEnumDecode(cls, ordinal));
        }
        return list;
    }

    @Override
    public TagCompound setEnumList(String key, List<? extends Enum<?>> items) {
        if (items == null) {
            remove(key);
            return this;
        }
        if (output == null) {
            return this;
        }
        int[] ordinals = new int[items.size()];
        for (int i = 0; i < items.size(); i++) {
            ordinals[i] = items.get(i).ordinal();
        }
        output.putIntArray(key, ordinals);
        return this;
    }

    @Override
    public <K, V> Map<K, V> getMap(String key, Function<String, K> keyFn, Function<TagCompound, V> valFn) {
        if (input == null || !hasKey(key)) {
            return null;
        }
        Optional<ValueInput> child = input.child(key);
        if (child.isEmpty()) {
            return new HashMap<>();
        }
        java.util.Set<String> keys;
        try {
            keys = child.get().keySet();
        } catch (RuntimeException e) {
            return new HashMap<>();
        }
        Map<K, V> map = new HashMap<>();
        for (String item : keys) {
            map.put(keyFn.apply(item), valFn.apply(indirect(child.get().childOrEmpty(item))));
        }
        return map;
    }

    @Override
    public ItemStack getStack(String key) {
        if (input == null || !hasKey(key)) {
            return ItemStack.EMPTY;
        }
        Optional<net.minecraft.world.item.ItemStack> stack = input.read(key, net.minecraft.world.item.ItemStack.CODEC);
        return stack.map(ItemStack::new).orElse(ItemStack.EMPTY);
    }

    @Override
    public TagCompound setStack(String key, ItemStack stack) {
        if (stack == null) {
            remove(key);
            return this;
        }
        if (output == null) {
            return this;
        }
        output.child(key).store(stack.toTag().internal);
        return this;
    }
}
