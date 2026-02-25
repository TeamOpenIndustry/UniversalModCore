package cam72cam.mod.mixin.feat.iris_pbr;

import cam72cam.mod.render.opengl.RenderContext;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.render.opengl.Texture;
import net.irisshaders.iris.pbr.texture.PBRTextureManager;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


/**
 * PBR handler for RenderContext (optional)
 */
@Pseudo
@Mixin(value = IrisRenderingPipeline.class, remap = false)
public class MixinNewWorldRenderingPipeline {
    @Shadow
    private int currentNormalTexture;

    @Shadow
    private int currentSpecularTexture;

    @Shadow
    private boolean shouldBindPBR;

    @Shadow
    private boolean isRenderingWorld;

    @Inject(method = "onSetShaderTexture", at = @At("HEAD"), cancellable = true)
    public void onSetPBRTex(int id, CallbackInfo ci) {
        if (this.shouldBindPBR && this.isRenderingWorld) {
            if (RenderContext.currentState.get() != null) {
                RenderState state = RenderContext.currentState.get();
                if (state.getNormals() != Texture.NO_TEXTURE && state.getNormals() != null && state.getNormals().getId() != -1) {
                    currentNormalTexture = state.getNormals().getId();
                }
                if (state.getSpecular() != Texture.NO_TEXTURE && state.getSpecular() != null && state.getSpecular().getId() != -1) {
                    currentSpecularTexture = state.getSpecular().getId();
                }

                PBRTextureManager.notifyPBRTexturesChanged();
                ci.cancel();
            }
        }
    }
}
