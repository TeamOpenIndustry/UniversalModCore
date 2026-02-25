//package cam72cam.mod.mixin.feat.iris_pbr;
//
//import cam72cam.mod.render.opengl.RenderContext;
//import cam72cam.mod.render.opengl.RenderState;
//import cam72cam.mod.render.opengl.Texture;
//import net.irisshaders.iris.pipeline.VanillaRenderingPipeline;
//import net.irisshaders.iris.texture.pbr.PBRTextureManager;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.Pseudo;
//import org.spongepowered.asm.mixin.Shadow;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//
///**
// * PBR handler for RenderContext (optional)
// */
//@Pseudo
//@Mixin(value = VanillaRenderingPipeline.class, remap = false)
//public class MixinDeferredWorldRenderingPipeline {
//    @Inject(method = "onSetShaderTexture", at = @At(value = "HEAD"))
//    public void onSetPBRTex(int id, CallbackInfo ci) {
//        //Nothing to do with vanilla
//    }
//}
