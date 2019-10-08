package cam72cam.mod.event.mixin;

import cam72cam.mod.event.CommonEvents;
import net.minecraft.entity.Entity;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldChunk.class)
public class WorldChunkMixin {
    @Inject(at = @At("RETURN"), method = "addEntity")
    public void addEntity(Entity entity, CallbackInfo info) {
        if (entity.isLiving()) {
            CommonEvents.Entity.WORLD_JOIN.execute(x -> x.accept(entity));
        }
    }

    @Inject(at = @At("RETURN"), method="remove")
    public void remove(Entity entity_1, CallbackInfo info) {
        CommonEvents.Entity.WORLD_LEAVE.execute(x -> x.accept(entity_1));
    }

    @Inject(at = @At("HEAD"), method = "addEntity", cancellable = true)
    public void addEntityHead(Entity entity, CallbackInfo info) {
        if (!CommonEvents.Entity.JOIN.executeCancellable(h -> h.onJoin(entity.world, entity))) {
            info.cancel();
        }
    }

}
