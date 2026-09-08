package cam72cam.mod.mixin.fix.direct_draw_call;

import cam72cam.mod.render.opengl.RenderContext;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL32;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//TODO why don't work with shader?
@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {
    //For DirectDraw call
    //Render our ones delayed for better occlusion
    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;addLateDebugPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/world/phys/Vec3;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V"))
    public void renderDeferred(GraphicsResourceAllocator p_361796_, DeltaTracker p_348530_, boolean p_109603_,
                               Camera p_109604_, Matrix4f p_254120_, Matrix4f p_323920_, GpuBufferSlice p_425977_,
                               Vector4f p_425544_, boolean p_426302_, CallbackInfo ci,
                               @Local FrameGraphBuilder builder) {
        FramePass pass = builder.addPass("UMC deferred");
        pass.executes(() -> {
            Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();
            GL11.glEnable(GL32.GL_DEPTH_TEST);
            GL11.glDepthMask(true);
            GL11.glDisable(GL32.GL_BLEND);

            RenderContext.flushDeferred();

            Minecraft.getInstance().gameRenderer.lightTexture().turnOffLightLayer();
        });
    }
}
