package cam72cam.mod.mixin.feat.pipeline_access;

import cam72cam.mod.mixin.accessor.ARenderPass;
import com.mojang.blaze3d.opengl.GlRenderPass;
import com.mojang.blaze3d.opengl.GlRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

@Mixin(GlRenderPass.class)
public class MixinGlRenderPass implements ARenderPass {
    @Shadow
    @Nullable
    protected GlRenderPipeline pipeline;

    public RenderPipeline getRenderPipeline() {
        if (pipeline != null) {
            return pipeline.info();
        }
        return RenderPipeline.builder().build();
    }
}
