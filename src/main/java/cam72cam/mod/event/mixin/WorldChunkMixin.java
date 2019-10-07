package cam72cam.mod.event.mixin;

import cam72cam.mod.event.CommonEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.Entity;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldChunk.class)
public class WorldChunkMixin {
    public static Event<EntityJoinCallback> JOIN_EVENT = EventFactory.createArrayBacked(EntityJoinCallback.class,
            (listeners) -> (entity) -> {
                for (EntityJoinCallback event : listeners) {
                    event.onJoin(entity);
                }
            });

    public static Event<EntityLeaveCallback> LEAVE_EVENT = EventFactory.createArrayBacked(EntityLeaveCallback.class,
            (listeners) -> (entity) -> {
                for (EntityLeaveCallback event : listeners) {
                    event.onLeave(entity);
                }
            });

    @Inject(at = @At("RETURN"), method = "addEntity")
    public void addEntity(Entity entity) {
        if (entity.isLiving()) {
            JOIN_EVENT.invoker().onJoin(entity);
        }
    }

    @Inject(at = @At("RETURN"), method="remove(E)Z")
    public void remove(Entity entity_1) {
        LEAVE_EVENT.invoker().onLeave(entity_1);
    }

    @FunctionalInterface
    public interface EntityJoinCallback {
        void onJoin(Entity entity);
    }

    @FunctionalInterface
    public interface EntityLeaveCallback {
        void onLeave(Entity entity);
    }

    @Inject(at = @At("HEAD"), method = "addEntity")
    public void addEntityHead(Entity entity, CallbackInfo info) {
        if (!CommonEvents.Entity.JOIN.executeCancellable(h -> h.onJoin(entity.world, entity))) {
            info.cancel();
        }
    }

}
