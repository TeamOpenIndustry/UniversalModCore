package cam72cam.mod.mixin.feat.large_entity_collision;

import cam72cam.mod.world.World;
import cam72cam.mod.world.WorldEntityTracker;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.Set;

/**
 * Hook into <code>World</code> to inject large UMC entities into search result
 */
@Mixin(EntitySectionStorage.class)
public class MixinEntitySectionStorage<T extends EntityAccess>  {
    @Shadow
    @Final
    private Long2ObjectMap<EntitySection<T>> sections;

    @Inject(method = "forEachAccessibleNonEmptySection", at = @At("HEAD"))
    public void init(AABB p_188363_, AbortableIterationConsumer<EntitySection<T>> p_261588_, CallbackInfo ci,
                     @Share("level") LocalRef<WorldEntityTracker> levelLocalRef, @Share("pos")LocalRef<Set<Long>> setLocalRef) {
        //Try to get corresponding level of the search
        Optional<Long2ObjectMap.Entry<EntitySection<T>>> any = this.sections.long2ObjectEntrySet().stream().filter(
                entry -> !entry.getValue().storage.isEmpty()).findAny();
        any.flatMap(entitySectionEntry -> entitySectionEntry.getValue().getEntities().filter(
                e -> e instanceof net.minecraft.world.entity.Entity).findFirst())
           .ifPresent(e -> {
                levelLocalRef.set(World.get(((net.minecraft.world.entity.Entity)e).level()).tracker);
                setLocalRef.set(new LongArraySet());
            });
    }

    @Inject(method = "forEachAccessibleNonEmptySection", at = @At(value = "INVOKE_ASSIGN", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;get(J)Ljava/lang/Object;",
            shift = At.Shift.BY, by = 2), remap = false) //To capture the EntitySection
    public void capture(AABB p_188363_, AbortableIterationConsumer<EntitySection<T>> p_261588_, CallbackInfo ci,
                        @Share("level")LocalRef<WorldEntityTracker> levelLocalRef, @Share("pos")LocalRef<Set<Long>> setLocalRef,
                        @Local LocalRef<EntitySection<T>> sectionLocalRef, @Local(ordinal = 2) long k2) {
        if (levelLocalRef.get() != null) {
            EntitySection<T> section = sectionLocalRef.get();
            if (section != null && section.getStatus().isAccessible() && !section.isEmpty()) {
                setLocalRef.get().addAll(levelLocalRef.get().queryPotentialPackedSectionPos(k2));
            }
        }
    }

    @Inject(method = "forEachAccessibleNonEmptySection", at = @At("RETURN"))
    public void finish(AABB p_188363_, AbortableIterationConsumer<EntitySection<T>> p_261588_, CallbackInfo ci,
                       @Share("level")LocalRef<WorldEntityTracker> levelLocalRef, @Share("pos")LocalRef<Set<Long>> setLocalRef) {
        if (levelLocalRef.get() != null) {
            for (long l1 : setLocalRef.get()) {
                if(sections.containsKey(l1)) {
                    EntitySection<T> tEntitySection = sections.get(l1);
                    if (tEntitySection != null && tEntitySection.getStatus().isAccessible() && !tEntitySection.isEmpty()) {
                        p_261588_.accept(tEntitySection);
                    }
                }
            }
        }
    }
}
