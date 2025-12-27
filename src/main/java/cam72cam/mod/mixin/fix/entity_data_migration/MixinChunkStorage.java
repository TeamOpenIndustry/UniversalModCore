package cam72cam.mod.mixin.fix.entity_data_migration;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.storage.ChunkStorage;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Fix mojang's DataFixer which deletes our custom entity data in saves updated from 1.16- to 1.18+
 * <p>
 * Though we could solve that by letting players update to 1.17 first then 1.18 we'd better handle here
 */
@Mixin(ChunkStorage.class)
public class MixinChunkStorage {
    @Inject(method = "upgradeChunkTag", at = @At("HEAD"))
    public void captureEntitiesTag(ResourceKey<Level> p_188289_, Supplier<DimensionDataStorage> p_188290_, CompoundTag p_188291_, Optional<ResourceKey<Codec<? extends ChunkGenerator>>> p_188292_,
                                   CallbackInfoReturnable<CompoundTag> cir, @Share("tag") LocalRef<ListTag> tag) {
        if(p_188291_.contains("Level")
           && p_188291_.getCompound("Level").contains("Entities")) {
            tag.set(p_188291_.getCompound("Level").getList("Entities", CompoundTag.TAG_COMPOUND));
        }
    }

    @Redirect(method = "upgradeChunkTag", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/datafix/DataFixTypes;updateToCurrentVersion(Lcom/mojang/datafixers/DataFixer;Lnet/minecraft/nbt/CompoundTag;I)Lnet/minecraft/nbt/CompoundTag;"))
    public CompoundTag reInject(DataFixTypes instance, DataFixer p_265583_, CompoundTag p_265401_, int p_265111_, @Share("tag") LocalRef<ListTag> tag) {
        CompoundTag compoundTag = instance.updateToCurrentVersion(p_265583_, p_265401_, p_265111_);
        if (tag.get() != null) {
            compoundTag.put("entities", tag.get());
        }
        return compoundTag;
    }
}
