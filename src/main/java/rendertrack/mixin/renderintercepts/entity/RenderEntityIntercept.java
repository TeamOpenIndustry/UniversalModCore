package rendertrack.mixin.renderintercepts.entity;

import cam72cam.mod.ModCore;
import cam72cam.mod.entity.ModdedEntity;
import cam72cam.mod.item.CustomItem;
import cam72cam.mod.render.Light;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.minecraft.client.model.geom.PartNames.HEAD;

/**
 * Inject to intercept renderEntity calls and redirect if they're UMC entities
 * @author Haxorouse
 */
@Mixin(value = LevelRenderer.class)
public abstract class RenderEntityIntercept {
    //javadoc after param rename
    @Inject(method = "renderEntity", at = @At("HEAD"), cancellable = true)
    public void checkUMCEntityRender(Entity entity, double p_109519_, double p_109520_, double p_109521_, float p_109522_, PoseStack p_109523_, MultiBufferSource p_109524_, CallbackInfo ci) {
        if(entity instanceof ModdedEntity) {
            ModCore.info("entity intercepted");
            ci.cancel();
        } else if(entity instanceof Light.LightEntity) {
            ModCore.info("light entity intercepted");
            ci.cancel();
        } else if(entity instanceof ItemEntity && ((ItemEntity) entity).getItem().getItem() instanceof CustomItem.ItemInternal) {
            ModCore.info("item entity intercepted");
            //ci.cancel();
        }

    }
}
