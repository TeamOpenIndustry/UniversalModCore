package cam72cam.mod.mixin.fix.large_entity_collision;

import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySectionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Re-implement call of <code>Level::getMaxEntityRadius</>
 * @see cam72cam.mod.ModCore.Internal#commonEvent
 */
@Mixin(EntitySectionStorage.class)
public class MixinEntitySectionStorage<T extends EntityAccess>  {
    @ModifyConstant(method = "forEachAccessibleSection", constant = @Constant(doubleValue = 2.0))
    private double increaseMaxEntityRadius(double constant){
        return 32;
    }
}
