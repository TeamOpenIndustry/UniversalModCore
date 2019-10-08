package cam72cam.mod.event.mixin;

import cam72cam.mod.event.ClientEvents;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Inject(method = "drawHighlightedBlockOutline", at = @At("HEAD"))
    public void drawHighlightedBlockOutline(Camera camera_1, HitResult hitResult_1, int int_1, CallbackInfo info) {
        if (hitResult_1.getType() == HitResult.Type.BLOCK) {
            // TODO MinecraftClient mc = MinecraftClient.getInstance();
            //mc.isPaused() ? mc.pausedTickDelta : mc.renderTickCounter.tickDelta
            ClientEvents.RENDER_MOUSEOVER.execute(x -> x.accept(0f));
        }

    }
}
