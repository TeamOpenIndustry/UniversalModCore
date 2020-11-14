package cam72cam.mod.event.mixin;

import cam72cam.mod.resource.DynamicResourcePack;
import net.minecraft.resource.ReloadableResourceManagerImpl;
import net.minecraft.resource.ResourceReloadListener;
import net.minecraft.resource.ResourceReloadMonitor;
import net.minecraft.util.Unit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** Inspired by https://github.com/Devan-Kerman/ARRP */
@Mixin(ReloadableResourceManagerImpl.class)
public abstract class ReloadableResourceManagerImplMixin {
    @Inject(method = "beginReloadInner", at = @At(value = "INVOKE"))
    protected void beginMonitoredReload(Executor prepareExecutor,
                                      Executor applyExecutor,
                                      List<ResourceReloadListener> listeners,
                                      CompletableFuture<Unit> initialStage,
                                      CallbackInfoReturnable<ResourceReloadMonitor> cir) {
        ((ReloadableResourceManagerImpl)(Object)this).addPack(DynamicResourcePack.INSTANCE);
    }
}
