package cam72cam.mod.mixin.fix.direct_draw_call;

import cam72cam.mod.render.ShaderHelper;
import cam72cam.mod.render.opengl.RenderContext;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//TODO why don't work with shader?
@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {
    //For DirectDraw call
    //Render our ones delayed for better occlusion
    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderDebug(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/Camera;)V"))
    public void renderDeferred(DeltaTracker delta, boolean outline, Camera camera, GameRenderer gameRenderer,
                               LightTexture lightTexture, Matrix4f matrix4f1, Matrix4f matrix, CallbackInfo ci) {
        Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();

        RenderContext.flushDeferred();

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        Minecraft.getInstance().gameRenderer.lightTexture().turnOffLightLayer();
    }

    //Enable depthMask if we have something special to render
    @Redirect(method = "renderSnowAndRain", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;depthMask(Z)V", ordinal = 0))
    public void allowDepthMask(boolean p_69459_) {
        if (RenderContext.hasDeferred() && !ShaderHelper.isIrisShaderEnabled()) {
            RenderSystem.depthMask(true);
        } else {
            RenderSystem.depthMask(p_69459_);
        }
    }

    @Redirect(method = "renderWorldBorder", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;depthMask(Z)V", ordinal = 0))
    public void allowBoarderDepthMask(boolean p_69459_) {
        if (RenderContext.hasDeferred() && !ShaderHelper.isIrisShaderEnabled()) {
            RenderSystem.depthMask(true);
        } else {
            RenderSystem.depthMask(p_69459_);
        }
    }
}
