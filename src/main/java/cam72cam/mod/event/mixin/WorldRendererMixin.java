package cam72cam.mod.event.mixin;

import cam72cam.mod.event.ClientEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Inject(method = "drawBlockOutline", at = @At("HEAD"))
    public void drawHighlightedBlockOutline(MatrixStack matrixStack_1, VertexConsumer vertexConsumer_1, Entity entity_1, double double_1, double double_2, double double_3, BlockPos blockPos_1, BlockState blockState_1, CallbackInfo info) {
            // TODO MinecraftClient mc = MinecraftClient.getInstance();
            //mc.isPaused() ? mc.pausedTickDelta : mc.renderTickCounter.tickDelta
            ClientEvents.RENDER_MOUSEOVER.execute(x -> x.accept(0f));
    }
}
