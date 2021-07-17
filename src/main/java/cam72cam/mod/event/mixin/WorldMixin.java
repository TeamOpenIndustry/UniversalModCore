package cam72cam.mod.event.mixin;

import cam72cam.mod.event.CommonEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.world.WorldTickCallback;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(World.class)
public class WorldMixin {
    static {
        ServerTickEvents.END_WORLD_TICK.register((world) -> CommonEvents.World.TICK.execute(x -> x.accept(world)));
        ClientTickEvents.END_WORLD_TICK.register((world) -> CommonEvents.World.TICK.execute(x -> x.accept(world)));
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    public void init(CallbackInfo info) {
        CommonEvents.World.LOAD.execute(x -> x.accept((World)(Object)this));
    }
}
