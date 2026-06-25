package cam72cam.umc.api.mixin.feat.global_renderer;

import cam72cam.umc.api.math.Vec3d;
import cam72cam.umc.api.render.GlobalRender;
import cam72cam.umc.api.render.opengl.RenderState;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderGlobal.class)
public class MixinRenderGlobal {
    @Inject(method = "renderEntities",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;preRenderDamagedBlocks()V"))
    public void injectRenderGlobal(Entity renderViewEntity, ICamera camera, float partialTicks, CallbackInfo ci) {
        Vec3d pos = GlobalRender.getCameraPos(partialTicks);
        RenderState state = new RenderState().translate(-pos.x, -pos.y, -pos.z);
        GlobalRender.renderGlobalFuncs(state.clone(), partialTicks);
    }
}
