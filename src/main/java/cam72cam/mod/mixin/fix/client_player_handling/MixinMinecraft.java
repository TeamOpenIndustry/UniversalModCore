package cam72cam.mod.mixin.fix.client_player_handling;

import cam72cam.mod.MinecraftClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Manually track loading state to avoid race condition on {@link MinecraftClient#isReady()} and actual player spawn
 */
@Mixin(Minecraft.class)
public class MixinMinecraft {
    @Inject(method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/client/multiplayer/WorldClient;spawnEntity(Lnet/minecraft/entity/Entity;)Z"))
    public void markReadyWhenLoaded(WorldClient worldClientIn, String loadingMessage, CallbackInfo ci) {
        MinecraftClient.setReady(true);
    }

    @Inject(method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/storage/ISaveFormat;flushCache()V"))
    public void markNotReadyWhenUnloaded(WorldClient worldClientIn, String loadingMessage, CallbackInfo ci) {
        MinecraftClient.setReady(false);
    }

    @Inject(method = "setDimensionAndSpawnPlayer", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/client/multiplayer/WorldClient;spawnEntity(Lnet/minecraft/entity/Entity;)Z"))
    public void markReadyWhenLoaded(int dimension, CallbackInfo ci) {
        MinecraftClient.setReady(true);
    }

    @Inject(method = "setDimensionAndSpawnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;getEntityId()I"))
    public void markNotReadyWhenTransfer(int dimension, CallbackInfo ci) {
        MinecraftClient.setReady(false);
    }
}
