package cam72cam.mod.mixin.accessor;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;

public interface ARenderPass {
    RenderPipeline getRenderPipeline();

    static ARenderPass from(RenderPass pass) {
        return (ARenderPass) pass;
    }
}
