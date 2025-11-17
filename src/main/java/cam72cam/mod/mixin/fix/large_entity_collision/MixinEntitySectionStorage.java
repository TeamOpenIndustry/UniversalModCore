package cam72cam.mod.mixin.fix.large_entity_collision;

import cam72cam.mod.entity.ModdedEntity;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
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

import java.util.function.Consumer;


/**
 * Fixes collision detection and ray tracing for entities that span multiple chunks.
 * <p>
 * Since Minecraft 1.17, the game only checks for entities within the chunks that the
 * given bounding box intersects. This causes issues with large entities that extend beyond
 * their primary chunk, as parts of them in other chunks may be ignored during collision
 * detection and ray tracing operations.
 * <p>
 * This mixin adds a check specifically for our {@link ModdedEntity} instances
 * to ensure that all relevant entity sections are considered, restoring proper functionality
 * for large entities that cross chunk boundaries.
 */
@Mixin(EntitySectionStorage.class)
public class MixinEntitySectionStorage<T extends EntityAccess>  {
    @Shadow
    @Final
    private Long2ObjectMap<EntitySection<T>> sections;

    @Inject(method = "forEachAccessibleSection", at = @At("TAIL"))
    public void inject(AABB pBoundingBox, Consumer<EntitySection<T>> pSection, CallbackInfo ci) {
        this.sections.values().stream()
                     .filter(e -> e.getStatus().isAccessible())
                     .filter(e -> e.getEntities().anyMatch(entity -> entity.getClass().equals(ModdedEntity.class)
                             && ((ModdedEntity)entity).getBoundingBox().intersects(pBoundingBox)))
                     .forEach(pSection);
    }
}
