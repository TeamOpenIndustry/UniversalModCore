package cam72cam.mod.mixin.feat.pipeline_access;

import cam72cam.mod.mixin.accessor.ARenderPass;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.neoforged.neoforge.client.blaze3d.validation.ValidationRenderPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ValidationRenderPass.class)
public class MixinValidationRenderPass implements ARenderPass {
    @Unique
    RenderPipeline pipeline;

    @Inject(method = "setPipeline", at = @At("TAIL"))
    public void snapPipeline(RenderPipeline pipeline, CallbackInfo ci) {
        this.pipeline = pipeline;
    }

    public RenderPipeline getRenderPipeline() {
        return pipeline;
    }
}
