package cam72cam.mod.mixin.feat.global_renderer;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.GlobalRender;
import cam72cam.mod.render.opengl.RenderState;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.*;
import net.minecraft.util.math.vector.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class MixinRenderGlobal {
    @Inject(method = "renderLevel",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/WorldRenderer;checkPoseStack(Lcom/mojang/blaze3d/matrix/MatrixStack;)V", ordinal = 1))
    public void injectRenderGlobal(MatrixStack stack, float partialTicks, long p_228426_3_, boolean p_228426_5_,
                                   ActiveRenderInfo info, GameRenderer gameRenderer, LightTexture light,
                                   Matrix4f matrix, CallbackInfo ci) {
        RenderType.getCutoutMipped().setupRenderState();
        Vec3d pos = GlobalRender.getCameraPos(partialTicks);
        RenderState state = new RenderState(stack).translate(-pos.x, -pos.y, -pos.z);
        GlobalRender.renderGlobalFuncs(state.clone(), partialTicks);
        RenderType.getCutoutMipped().clearRenderState();
    }
}
