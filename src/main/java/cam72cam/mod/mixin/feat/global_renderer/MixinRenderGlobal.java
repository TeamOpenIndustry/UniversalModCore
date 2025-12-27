package cam72cam.mod.mixin.feat.global_renderer;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.GlobalRender;
import cam72cam.mod.render.opengl.RenderContext;
import cam72cam.mod.render.opengl.RenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class MixinRenderGlobal {
    @Inject(method = "renderLevel",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;checkPoseStack(Lcom/mojang/blaze3d/vertex/PoseStack;)V", ordinal = 1))
    public void injectRenderGlobal(PoseStack stack, float partialTicks, long p_228426_3_, boolean p_228426_5_,
                                   Camera info, GameRenderer gameRenderer, LightTexture light,
                                   Matrix4f matrix, CallbackInfo ci) {
        RenderType.cutoutMipped().setupRenderState();
        Vec3d pos = GlobalRender.getCameraPos(partialTicks);
        RenderState state = new RenderState(stack).translate(-pos.x, -pos.y, -pos.z);
        GlobalRender.renderGlobalFuncs(state.clone(), partialTicks);
        RenderType.cutoutMipped().clearRenderState();
        RenderContext.resetState();
    }
}
