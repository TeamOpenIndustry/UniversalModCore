package cam72cam.mod.item;

import cam72cam.mod.ModCore;
import cam72cam.mod.serialization.TagCompound;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public record ModCoreDataComponent(CompoundTag compound) {
    private static final Codec<ModCoreDataComponent> COMPONENT_CODEC =
            RecordCodecBuilder.create(instance ->
                                              instance.group(
                                                      CompoundTag.CODEC.fieldOf("compound").forGetter(ModCoreDataComponent::compound)
                                              ).apply(instance, ModCoreDataComponent::new)
    );
    private static final StreamCodec<ByteBuf, ModCoreDataComponent> STREAM_CODEC =
            ByteBufCodecs.COMPOUND_TAG.map(ModCoreDataComponent::new, ModCoreDataComponent::compound);

    private static final DeferredRegister.DataComponents REGISTRAR =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ModCore.MODID);

    private static final DeferredHolder<DataComponentType<?>, DataComponentType<ModCoreDataComponent>> DATA_COMPONENT =
            REGISTRAR.registerComponentType(
            "umc_item_data",
            builder -> builder
                    .persistent(COMPONENT_CODEC)
                    .networkSynchronized(STREAM_CODEC)
    );

    public static ModCoreDataComponent of(TagCompound compound) {
        return new ModCoreDataComponent(compound.internal.copy());
    }

    public static ModCoreDataComponent of(CompoundTag compound) {
        return new ModCoreDataComponent(compound.copy());
    }

    public static DataComponentType<ModCoreDataComponent> type() {
        return DATA_COMPONENT.get();
    }

    public static void register(IEventBus bus) {
        REGISTRAR.register(bus);
    }
}
