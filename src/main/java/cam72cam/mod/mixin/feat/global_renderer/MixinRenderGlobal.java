package cam72cam.mod.mixin.feat.global_renderer;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.GlobalRender;
import cam72cam.mod.render.opengl.RenderState;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderGlobal.class)
public class MixinRenderGlobal {
    @Inject(method = "renderEntities",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;disableLightmap(D)V"))
    public void injectRenderGlobal(EntityLivingBase entity, ICamera camera, float partialTicks, CallbackInfo ci) {
        Vec3d pos = GlobalRender.getCameraPos(partialTicks);
        RenderState state = new RenderState().translate(-pos.x, -pos.y, -pos.z);
        GlobalRender.renderGlobalFuncs(state.clone(), partialTicks);
    }
}
