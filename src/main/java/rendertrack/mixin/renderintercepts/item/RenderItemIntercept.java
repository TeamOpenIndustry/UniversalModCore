package rendertrack.mixin.renderintercepts.item;

import cam72cam.mod.ModCore;
import cam72cam.mod.item.CustomItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 *
 * @author Haxorouse
 */
@Mixin(value = ItemRenderer.class)
public abstract class RenderItemIntercept {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void renderUMC(ItemStack item, ItemTransforms.TransformType perspective, boolean leftHand, PoseStack matrixStack, MultiBufferSource bufferIn, int light, int overlay, BakedModel model, CallbackInfo ci) {
        if(item.getItem() instanceof CustomItem.ItemInternal) {
            ModCore.info("Item Render Intercepted");
            ci.cancel();
        }
    }
}
