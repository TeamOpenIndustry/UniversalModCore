package cam72cam.mod.mixin.feat.entity_render_batcher;

import cam72cam.mod.render.common.RenderContext;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {
    @Shadow
    public abstract void enableLightmap();

    @Shadow
    public abstract void disableLightmap();

    @Inject(method = "renderWorldPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;depthMask(Z)V", ordinal = 0))
    public void flushEntityAndParticle(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        enableLightmap();
        RenderContext.get().flushEnqueued(RenderContext.Stage.ENTITY);
        disableLightmap();
    }
}
